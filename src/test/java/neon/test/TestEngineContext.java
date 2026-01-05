package neon.test;

import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.Getter;
import neon.core.Engine;
import neon.core.Game;
import neon.entities.Entity;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.components.PhysicsComponent;
import neon.entities.property.Gender;
import neon.maps.Atlas;
import neon.maps.ZoneActivator;
import neon.maps.ZoneFactory;
import neon.maps.services.EngineQuestProvider;
import neon.maps.services.EngineResourceProvider;
import neon.maps.services.EntityStore;
import neon.maps.services.PhysicsManager;
import neon.resources.RCreature;
import neon.resources.RTerrain;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.systems.physics.PhysicsSystem;
import org.h2.mvstore.MVStore;

/**
 * Test utility for managing Engine singleton dependencies in tests.
 *
 * <p>Provides minimal stub implementations of Engine singletons to support testing without full
 * Engine initialization.
 */
public class TestEngineContext {

  private static MVStore testDb;
  private static Atlas testAtlas;
  private static ResourceManager testResources;
  private static Game testGame;
  private static UIDStore testStore;
  private static ZoneFactory testZoneFactory;
  private static EntityStore testEntityStore;
  private static ZoneActivator testZoneActivator;
  @Getter private static StubFileSystem stubFileSystem;

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
  public static void initialize(MVStore db) throws Exception {
    testDb = db;

    // Create stub ResourceManager
    testResources = new StubResourceManager();
    setStaticField(Engine.class, "resources", testResources);

    // Create test UIDStore
    testStore = new UIDStore("test-store.dat");

    // Create test EntityStore
    testEntityStore = new StubEntityStore(testStore);

    // Create stub PhysicsManager and ZoneActivator
    PhysicsManager stubPhysicsManager = new StubPhysicsManager();
    Player stubPlayer = new StubPlayer();
    testZoneActivator = new ZoneActivator(stubPhysicsManager, () -> stubPlayer);

    // Create ZoneFactory for tests
    testZoneFactory = new ZoneFactory(db);

    // Create test Atlas with dependency injection (doesn't need Engine.game)
    testAtlas =
        new Atlas(
            getStubFileSystem(),
            db,
            testEntityStore,
            new EngineResourceProvider(),
            new EngineQuestProvider(),
            testZoneActivator);

    // Create test Game using new DI constructor
    testGame = new Game(stubPlayer, testAtlas, testStore);
    setStaticField(Engine.class, "game", testGame);

    // Create stub FileSystem
    setStaticField(Engine.class, "files", new StubFileSystem());

    // Create stub PhysicsSystem
    setStaticField(Engine.class, "physics", new StubPhysicsSystem());
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

      testResources = null;
      testGame = null;
      testStore = null;

    } catch (Exception e) {
      System.err.println("Warning: Failed to reset test engine context: " + e.getMessage());
    }
  }

  /** Gets the test Atlas instance. */
  public static Atlas getTestAtlas() {
    return testAtlas;
  }

  /** Gets the test ResourceManager instance. */
  public static ResourceManager getTestResources() {
    return testResources;
  }

  /** Gets the test ZoneFactory instance. */
  public static ZoneFactory getTestZoneFactory() {
    return testZoneFactory;
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
  static class StubResourceManager extends ResourceManager {
    @Override
    public neon.resources.Resource getResource(String id, String namespace) {
      return switch (namespace) {
        case "terrain" -> new RTerrain(id);
        case "theme", "ztheme" -> null; // regions can have null theme for fixed terrain
        default -> null;
      };
    }
  }

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
