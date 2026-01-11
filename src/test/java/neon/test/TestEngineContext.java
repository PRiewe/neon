package neon.test;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neon.core.Engine;
import neon.core.Game;
import neon.core.event.TaskQueue;
import neon.entities.Entity;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.components.PhysicsComponent;
import neon.entities.property.Gender;
import neon.maps.*;
import neon.maps.services.*;
import neon.narrative.QuestTracker;
import neon.resources.*;
import neon.resources.builder.IniBuilder;
import neon.systems.files.FileSystem;
import neon.systems.physics.PhysicsSystem;
import neon.util.mapstorage.MapStore;
import org.h2.mvstore.MVStore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 * Test utility for managing Engine singleton dependencies in tests.
 *
 * <p>Provides minimal stub implementations of Engine singletons to support testing without full
 * Engine initialization.
 */
@Slf4j
public class TestEngineContext {

  private static MapStore testDb;

  /** -- GETTER -- Gets the test Atlas instance. */
  @Getter private static Atlas testAtlas;

  private static StubResourceManager testResources;
  private static Game testGame;
  private static UIDStore testStore;
  @Getter private static PhysicsManager stubPhysicsManager;

  @Getter private static AtlasPosition atlasPosition;

  /** -- GETTER -- Gets the test ZoneFactory instance. */
  @Getter private static ZoneFactory testZoneFactory;

  /** -- GETTER -- Gets the test ResourceProvider instance. */
  @Getter private static EntityStore testEntityStore;

  @Getter private static ZoneActivator testZoneActivator;
  @Getter private static PhysicsSystem physicsSystem;

  @Getter private static StubFileSystem stubFileSystem;
  @Getter private static neon.core.DefaultGameContext testContext;
  @Getter private static MapLoader mapLoader;
  @Getter private static QuestTracker questTracker;

  static {
    try {
      stubFileSystem = new StubFileSystem();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initializes a minimal test context for Engine dependencies.
   *
   * <p>Sets up stub implementations for:
   *
   * <ul>
   *   <li>ResourceManager (returns dummy resources)
   *   <li>Atlas (uses provided test DB with DI)
   *   <li>Game (minimal implementation for getAtlas/getStore)
   *   <li>UIDStore (in-memory)
   *   <li>FileSystem (stub)
   *   <li>PhysicsSystem (stub)
   *   <li>ZoneFactory (for creating zones in tests)
   * </ul>
   *
   * @param db the MapDb database to use for Atlas
   * @throws RuntimeException if reflection fails
   */
  public static void initialize(MapStore db) throws Exception {
    testDb = db;

    // Create stub ResourceManager
    testResources = new StubResourceManager();
    setStaticField(Engine.class, "resources", testResources);

    // Create test UIDStore
    testStore = new UIDStore(testDb);
    questTracker = new QuestTracker();
    // Create test EntityStore
    testEntityStore = new StubEntityStore(testStore);

    // Create stub PhysicsManager and ZoneActivator
    stubPhysicsManager = new StubPhysicsManager();
    Player stubPlayer = new StubPlayer();
    testZoneActivator = new ZoneActivator(stubPhysicsManager);

    // Create ZoneFactory for tests
    testZoneFactory = new ZoneFactory(db);
    mapLoader = new MapLoader(testEntityStore, testResources, getStubFileSystem());
    // Create test Atlas with dependency injection (doesn't need Engine.game)
    testAtlas = new Atlas(getStubFileSystem(), testDb, getTestEntityStore(), mapLoader);
    atlasPosition =
        new AtlasPosition(
            testAtlas, testZoneActivator, testResources, questTracker, testEntityStore);
    // Create test Game using new DI constructor

    testGame = new Game(stubPlayer, testAtlas, testStore, atlasPosition);
    setStaticField(Engine.class, "game", testGame);

    // Create stub FileSystem
    setStaticField(Engine.class, "files", stubFileSystem);

    // Create stub PhysicsSystem
    setStaticField(Engine.class, "physics", new StubPhysicsSystem());

    // Create and initialize test GameContext
    testContext = new neon.core.DefaultGameContext();
    testContext.setResources(testResources);
    testContext.setFileSystem(stubFileSystem);
    testContext.setPhysicsEngine(new StubPhysicsSystem());
    testContext.setQueue(new neon.core.event.TaskQueue());
    testContext.setGame(testGame);

    // Note: We don't set Engine reference in tests since we don't have a real Engine instance
    setStaticField(Engine.class, "context", testContext);
  }

  /**
   * Resets the test context by setting all Engine static fields to null.
   *
   * <p>Should be called in @AfterEach to ensure test isolation.
   */
  public static void reset() {
    try {
      if (testStore != null) {
        testStore.getCache().close();
      }

      if (testAtlas != null) {
        testAtlas.close();
      }
      if (testDb != null) {
        testDb.close();
      }
      setStaticField(Engine.class, "resources", null);
      setStaticField(Engine.class, "game", null);
      setStaticField(Engine.class, "files", null);
      setStaticField(Engine.class, "physics", null);
      setStaticField(Engine.class, "context", null);

      testResources = null;
      testGame = null;
      testStore = null;
      testContext = null;

    } catch (Exception e) {
      log.error("Failed to reset test engine context", e);
    }
  }

  /** Gets the test ResourceManager instance. */
  public static ResourceManager getTestResources() {
    return testResources;
  }

  /** Gets the test ResourceProvider instance. */
  public static ResourceProvider getTestResourceProvider() {
    return testResources;
  }

  public static void loadTestResourceViaConfig(String configFilename) throws Exception {
    IniBuilder iniBuilder = new IniBuilder(configFilename, getStubFileSystem(), new TaskQueue());
    iniBuilder.build(getTestResources());
  }

  /**
   * Loads test resources from a mod path following the same pattern as ModLoader.
   *
   * <p>Loads items, creatures, terrain, and themes from XML files in the specified path.
   *
   * @param modPath the path to the mod directory (e.g., "src/test/resources/sampleMod1")
   */
  public static void loadTestResources(String modPath) throws Exception {
    SAXBuilder builder = new SAXBuilder();

    // Load items
    File itemsFile = new File(modPath + "/objects/items.xml");
    if (itemsFile.exists()) {
      Document doc = builder.build(itemsFile);
      for (Element e : doc.getRootElement().getChildren()) {
        switch (e.getName()) {
          case "book", "scroll" -> Engine.getResources().addResource(new RItem.Text(e));
          case "weapon" -> Engine.getResources().addResource(new RWeapon(e));
          case "door" -> Engine.getResources().addResource(new RItem.Door(e));
          case "potion" -> Engine.getResources().addResource(new RItem.Potion(e));
          case "container" -> Engine.getResources().addResource(new RItem.Container(e));
          case "armor", "clothing" -> Engine.getResources().addResource(new RClothing(e));
          case "list" -> Engine.getResources().addResource(new LItem(e));
          default -> Engine.getResources().addResource(new RItem(e));
        }
      }
    }

    // Load creatures
    File monstersFile = new File(modPath + "/objects/monsters.xml");
    if (monstersFile.exists()) {
      Document doc = builder.build(monstersFile);
      for (Element c : doc.getRootElement().getChildren()) {
        switch (c.getName()) {
          case "list" -> Engine.getResources().addResource(new LCreature(c));
          default -> Engine.getResources().addResource(new RCreature(c));
        }
      }
    }

    // Load terrain
    File terrainFile = new File(modPath + "/terrain.xml");
    if (terrainFile.exists()) {
      Document doc = builder.build(terrainFile);
      for (Element e : doc.getRootElement().getChildren()) {
        Engine.getResources().addResource(new RTerrain(e), "terrain");
      }
    }

    // Load themes
    File dungeonsFile = new File(modPath + "/themes/dungeons.xml");
    if (dungeonsFile.exists()) {
      Document doc = builder.build(dungeonsFile);
      for (Element theme : doc.getRootElement().getChildren("dungeon")) {
        Engine.getResources().addResource(new RDungeonTheme(theme), "theme");
      }
    }

    File zonesFile = new File(modPath + "/themes/zones.xml");
    if (zonesFile.exists()) {
      Document doc = builder.build(zonesFile);
      for (Element theme : doc.getRootElement().getChildren("zone")) {
        Engine.getResources().addResource(new RZoneTheme(theme), "theme");
      }
    }

    File regionsFile = new File(modPath + "/themes/regions.xml");
    if (regionsFile.exists()) {
      Document doc = builder.build(regionsFile);
      for (Element theme : doc.getRootElement().getChildren("region")) {
        Engine.getResources().addResource(new RRegionTheme(theme), "theme");
      }
    }
  }

  /** Sets a static field using reflection. */
  private static void setStaticField(Class<?> clazz, String fieldName, Object value)
      throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  /** Sets the Atlas's DB field using reflection (it's private). */
  private static void setAtlasDb(Atlas atlas, MVStore db) throws Exception {

    Field dbField = Atlas.class.getDeclaredField("db");
    dbField.setAccessible(true);
    dbField.set(atlas, db);

    // Also need to recreate the maps HTreeMap with the new DB
    Field mapsField = Atlas.class.getDeclaredField("maps");
    mapsField.setAccessible(true);
    mapsField.set(atlas, db.openMap("maps"));
  }

  /** Stub ResourceManager that returns dummy resources. */
  static class StubResourceManager extends ResourceManager implements ResourceProvider {}

  /** Stub FileSystem (minimal implementation). */
  public static class StubFileSystem extends FileSystem {

    private StubFileSystem() throws IOException {}
  }

  public static StubFileSystem createNewStubFileSystem() throws IOException {
    return new StubFileSystem();
  }

  /** Stub PhysicsSystem (minimal implementation). */
  static class StubPhysicsSystem extends PhysicsSystem {
    // Minimal stub - can be extended if needed
  }

  /** Stub EntityStore for testing. */
  static class StubEntityStore implements EntityStore {
    private final UIDStore store;

    public StubEntityStore(UIDStore store) {
      this.store = store;
    }

    @Override
    public Entity getEntity(long uid) {
      return store.getEntity(uid);
    }

    @Override
    public void addEntity(Entity entity) {
      store.addEntity(entity);
    }

    @Override
    public long createNewEntityUID() {
      return store.createNewEntityUID();
    }

    @Override
    public int createNewMapUID() {
      return store.createNewMapUID();
    }

    @Override
    public String[] getMapPath(int uid) {
      return store.getMapPath(uid);
    }
  }

  /** Stub PhysicsManager for testing. */
  static class StubPhysicsManager implements PhysicsManager {
    @Override
    public void clear() {
      // No-op for tests
    }

    @Override
    public void register(neon.maps.Region region, Rectangle bounds, boolean fixed) {
      // No-op for tests
    }

    @Override
    public void register(PhysicsComponent component) {
      // No-op for tests
    }
  }

  /** Stub Player for testing. */
  static class StubPlayer extends Player {
    private final PhysicsComponent physicsComponent;

    public StubPlayer() {
      super(new RCreature("test"), "TestPlayer", Gender.MALE, Specialisation.combat, "Warrior");
      this.physicsComponent = new PhysicsComponent(0L, new Rectangle(0, 0, 1, 1));
    }

    @Override
    public PhysicsComponent getPhysicsComponent() {
      return physicsComponent;
    }
  }
}
