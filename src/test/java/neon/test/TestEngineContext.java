package neon.test;

import java.io.File;
import java.io.IOException;
import lombok.Getter;
import neon.core.*;
import neon.core.event.TaskQueue;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.property.Gender;
import neon.maps.*;
import neon.maps.services.*;
import neon.narrative.QuestTracker;
import neon.resources.*;
import neon.resources.builder.IniBuilder;
import neon.systems.files.FileSystem;
import neon.systems.physics.PhysicsSystem;
import neon.util.mapstorage.MapStore;

/**
 * Test utility for managing Engine singleton dependencies in tests.
 *
 * <p>Provides minimal stub implementations of Engine singletons to support testing without full
 * Engine initialization.
 */
public class TestEngineContext {

  private static MapStore testDb;

  /** -- GETTER -- Gets the test Atlas instance. */
  @Getter private static Atlas testAtlas;

  private static ResourceManager testResources;
  private static Game testGame;
  private static UIDStore testStore;
  @Getter private static ZoneFactory testZoneFactory;
  @Getter private static GameStore gameStore;

  private static ZoneActivator testZoneActivator;
  @Getter private static DefaultUIEngineContext testUiEngineContext;
  @Getter private static QuestTracker testQuestTracker;
  @Getter private static StubFileSystem stubFileSystem;
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
    testResources = new ResourceManager();
    gameStore = new GameStore(stubFileSystem, testResources);

    // Create test UIDStore
    testStore = gameStore.getStore();
    // Create test Game using new DI constructor

    // Create stub PhysicsManager and ZoneActivator
    PhysicsSystem physicsSystem = new PhysicsSystem();
    GameServices gameServices = new GameServices(physicsSystem, Engine.createScriptEngine());

    testQuestTracker = new QuestTracker(gameStore, gameServices);
    TaskQueue taskQueue = new TaskQueue(gameServices.scriptEngine());
    testZoneActivator = new ZoneActivator(physicsSystem, gameStore);
    testUiEngineContext = new DefaultUIEngineContext(gameStore, testQuestTracker, taskQueue);
    Player stubPlayer =
        new Player(
            new RCreature("test"),
            "TestPlayer",
            Gender.MALE,
            Player.Specialisation.combat,
            "Warrior",
            testUiEngineContext);
    gameStore.setPlayer(stubPlayer);
    testUiEngineContext.setGameServices(gameServices);
    // Create ZoneFactory for tests
    testZoneFactory = testUiEngineContext.getZoneFactory();

    MapLoader testMapLoader = new MapLoader(testUiEngineContext);
    // Create test Atlas with dependency injection (doesn't need Engine.game)
    testAtlas =
        new Atlas(
            gameStore,
            testDb,
            testQuestTracker,
            testZoneActivator,
            testZoneFactory,
            testMapLoader,
            testUiEngineContext);
    testGame = new Game(gameStore, testUiEngineContext, testAtlas);
    testUiEngineContext.setGame(testGame);
    gameStore.getUidStore().initialize(testUiEngineContext);
  }

  /**
   * Resets the test context by setting all Engine static fields to null.
   *
   * <p>Should be called in @AfterEach to ensure test isolation.
   */
  public static void reset() {
    try {
      if (testStore != null) {
        gameStore.close();
        new File(gameStore.getUidStoreFileName()).delete();
      }
      if (testAtlas != null) {
        testAtlas.close();
      }
      if (testDb != null) {
        testDb.close();
      }
      if (testZoneFactory != null) {
        testZoneFactory.close();
        new File(testUiEngineContext.getZoneMapStoreFileName()).delete();
      }
      gameStore.close();
      testResources = null;
      testGame = null;
      testStore = null;

    } catch (Exception e) {
      System.err.println("Warning: Failed to reset test engine context: " + e.getMessage());
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
    IniBuilder iniBuilder =
        new IniBuilder(
            configFilename, getStubFileSystem(), new TaskQueue(Engine.createScriptEngine()));
    iniBuilder.build(getTestResources());
  }

  public static EntityStore getTestEntityStore() {
    return getTestUiEngineContext().getStore();
  }

  /** Stub FileSystem (minimal implementation). */
  public static class StubFileSystem extends FileSystem {

    private StubFileSystem() throws IOException {}
  }
}
