package neon.test;

import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neon.core.DefaultGameStores;
import neon.core.Engine;
import neon.core.Game;
import neon.core.GameStores;
import neon.core.event.TaskQueue;
import neon.entities.Entity;
import neon.entities.Player;
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
@Getter
  private static GameStores gameStores;
  @Getter
  private static StubResourceManager testResources;
  private static Game testGame;
  @Getter private static neon.entities.UIDStore testStore;
  @Getter private static PhysicsManager stubPhysicsManager;

  @Getter private static AtlasPosition atlasPosition;

  /** -- GETTER -- Gets the test ZoneFactory instance. */
  @Getter private static ZoneFactory testZoneFactory;

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
    testStore = new neon.entities.UIDStore(testDb);

    // Create test EntityStore


    // Create stub PhysicsManager and ZoneActivator
    stubPhysicsManager = new StubPhysicsManager();
    Player stubPlayer = new StubPlayer();
    testZoneActivator = new ZoneActivator(stubPhysicsManager);

    // Create ZoneFactory for tests
    testZoneFactory = new ZoneFactory(db);
    mapLoader = new MapLoader( getStubFileSystem(),testStore, testResources);
    // Create test Atlas with dependency injection (doesn't need Engine.game)
    testAtlas = new Atlas(getStubFileSystem(), testDb, testStore, mapLoader);
    gameStores =
        new DefaultGameStores(getTestResources(), getStubFileSystem(), testStore, testAtlas);
    questTracker = new QuestTracker(gameStores);
    atlasPosition =
        new AtlasPosition(
            testAtlas, testZoneActivator, testResources, questTracker, testStore);
    // Create test Game using new DI constructor

    testGame = new Game(stubPlayer, gameStores, atlasPosition);
    setStaticField(Engine.class, "game", testGame);

    // Create stub FileSystem
    setStaticField(Engine.class, "files", stubFileSystem);

    // Create stub PhysicsSystem
    setStaticField(Engine.class, "physics", new StubPhysicsSystem());

    // Create and initialize test GameContext
    testContext = new neon.core.DefaultGameContext();

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
    private final neon.entities.UIDStore store;

    public StubEntityStore(neon.entities.UIDStore store) {
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
      super(new RCreature("test"), "TestPlayer", Gender.MALE, Specialisation.combat, "Warrior",getGameStores());
      this.physicsComponent = new PhysicsComponent(0L, new Rectangle(0, 0, 1, 1));
    }

    @Override
    public PhysicsComponent getPhysicsComponent() {
      return physicsComponent;
    }
  }
}
