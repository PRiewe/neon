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
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.components.PhysicsComponent;
import neon.entities.mvstore.EntityDataType;
import neon.entities.mvstore.ModDataType;
import neon.entities.property.Gender;
import neon.entities.serialization.EntityFactory;
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
@Slf4j
public class TestEngineContext {

  private static MapStore testDb;

  /** -- GETTER -- Gets the test Atlas instance. */
  @Getter private static Atlas testAtlas;

  @Getter private static GameStores gameStores;
  @Getter private static StubResourceManager testResources;
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
    testStore = new UIDStore(testDb);
    // Create test Game using new DI constructor
    Player stubPlayer =
        new Player(
            new RCreature("test"),
            "TestPlayer",
            Gender.MALE,
            Player.Specialisation.combat,
            "Warrior",
            testStore);
    testStore.setPlayer(stubPlayer);
    // Phase 2: Create EntityFactory with dependencies (GameContext is null at this stage)
    EntityFactory entityFactory = new EntityFactory(testResources, testStore);
    // Phase 3: Create DataTypes
    EntityDataType entityDataType = new EntityDataType(entityFactory);
    ModDataType modDataType = new ModDataType();

    // Phase 4: Initialize UIDStore's maps with DataTypes
    testStore.setDataTypes(entityDataType, modDataType);

    // Create test EntityStore

    // Create stub PhysicsManager and ZoneActivator
    stubPhysicsManager = new StubPhysicsManager();

    testZoneActivator = new ZoneActivator(stubPhysicsManager);

    // Create ZoneFactory for tests
    testZoneFactory = new ZoneFactory(testDb, testStore, testResources);
    mapLoader = new MapLoader(getStubFileSystem(), testStore, testResources, testZoneFactory);
    // Create test Atlas with dependency injection (doesn't need Engine.game)
    testAtlas = new Atlas(getStubFileSystem(), testDb, testStore, testResources, mapLoader);
    gameStores = new DefaultGameStores(getTestResources(), getStubFileSystem(), testStore);
    questTracker = new QuestTracker(gameStores);
    atlasPosition = new AtlasPosition(gameStores, questTracker);
    testGame = new Game(gameStores, atlasPosition);
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
        testStore.close();
      }

      if (testAtlas != null) {
        testAtlas.close();
      }
      if (testDb != null) {
        testDb.close();
      }
      if (gameStores.getZoneMapStore() != null) {
        gameStores.getZoneMapStore().close();
      }

      if (gameStores.getStore() != null) {
        gameStores.getStore().close();
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

  public static UIDStore getTestEntityStore() {
    return TestEngineContext.getTestStore();
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

  /** Stub ResourceManager that returns dummy resources. */
  static class StubResourceManager extends ResourceManager implements ResourceProvider {}

  /** Stub FileSystem (minimal implementation). */
  public static class StubFileSystem extends FileSystem {

    private StubFileSystem() throws IOException {}
  }

  /** Stub PhysicsSystem (minimal implementation). */
  static class StubPhysicsSystem extends PhysicsSystem {
    // Minimal stub - can be extended if needed
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
}
