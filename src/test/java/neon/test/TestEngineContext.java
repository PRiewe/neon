package neon.test;

import java.lang.reflect.Field;
import neon.core.Engine;
import neon.core.Game;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.resources.ResourceManager;
import neon.resources.RRegionTheme;
import neon.resources.RTerrain;
import neon.resources.RZoneTheme;
import neon.systems.files.FileSystem;
import neon.systems.physics.PhysicsSystem;
import org.mapdb.DB;

/**
 * Test utility for managing Engine singleton dependencies in tests.
 *
 * <p>Provides minimal stub implementations of Engine singletons to support testing without full
 * Engine initialization.
 */
public class TestEngineContext {

  private static DB testDb;
  private static Atlas testAtlas;
  private static ResourceManager testResources;
  private static Game testGame;
  private static UIDStore testStore;

  /**
   * Initializes a minimal test context for Engine dependencies.
   *
   * <p>Sets up stub implementations for:
   *
   * <ul>
   *   <li>ResourceManager (returns dummy resources)
   *   <li>Atlas (uses provided test DB)
   *   <li>Game (minimal implementation for getAtlas/getStore)
   *   <li>UIDStore (in-memory)
   *   <li>FileSystem (stub)
   *   <li>PhysicsSystem (stub)
   * </ul>
   *
   * @param db the MapDb database to use for Atlas
   * @throws RuntimeException if reflection fails
   */
  public static void initialize(DB db) {
    testDb = db;

    try {
      // Create stub ResourceManager
      testResources = new StubResourceManager();
      setStaticField(Engine.class, "resources", testResources);

      // Create test Atlas
      testAtlas = new Atlas(new StubFileSystem(), "test");
      setAtlasDb(testAtlas, db);

      // Create test UIDStore
      testStore = new UIDStore("test-store.dat");

      // Create minimal Game instance
      testGame = new StubGame(testAtlas, testStore);
      setStaticField(Engine.class, "game", testGame);

      // Create stub FileSystem
      setStaticField(Engine.class, "files", new StubFileSystem());

      // Create stub PhysicsSystem
      setStaticField(Engine.class, "physics", new StubPhysicsSystem());

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize test engine context", e);
    }
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

      setStaticField(Engine.class, "resources", null);
      setStaticField(Engine.class, "game", null);
      setStaticField(Engine.class, "files", null);
      setStaticField(Engine.class, "physics", null);

      testDb = null;
      testAtlas = null;
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

  /** Sets a static field using reflection. */
  private static void setStaticField(Class<?> clazz, String fieldName, Object value)
      throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  /** Sets the Atlas's DB field using reflection (it's private). */
  private static void setAtlasDb(Atlas atlas, DB db) throws Exception {
    Field dbField = Atlas.class.getDeclaredField("db");
    dbField.setAccessible(true);
    dbField.set(atlas, db);

    // Also need to recreate the maps HTreeMap with the new DB
    Field mapsField = Atlas.class.getDeclaredField("maps");
    mapsField.setAccessible(true);
    mapsField.set(atlas, db.hashMap("maps").createOrOpen());
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

  /** Stub Game that provides Atlas and UIDStore. */
  static class StubGame extends Game {
    private final Atlas atlas;
    private final UIDStore store;

    public StubGame(Atlas atlas, UIDStore store) {
      super(null, new StubFileSystem());
      this.atlas = atlas;
      this.store = store;
    }

    @Override
    public Atlas getAtlas() {
      return atlas;
    }

    @Override
    public UIDStore getStore() {
      return store;
    }
  }

  /** Stub FileSystem (minimal implementation). */
  static class StubFileSystem extends FileSystem {
    // Minimal stub - can be extended if needed
  }

  /** Stub PhysicsSystem (minimal implementation). */
  static class StubPhysicsSystem extends PhysicsSystem {
    // Minimal stub - can be extended if needed
  }
}
