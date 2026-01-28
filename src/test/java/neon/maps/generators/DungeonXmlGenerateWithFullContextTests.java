package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import neon.entities.Door;
import neon.entities.Entity;
import neon.maps.*;
import neon.maps.services.EntityStore;
import neon.maps.services.QuestProvider;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.Dice;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * test class for integration tests that require full Engine context. These tests verify the
 * generate(Door, Zone, Atlas) method which creates actual entities.
 */
class DungeonXmlGenerateWithFullContextTests {

  private MapStore testDb;
  private Atlas testAtlas;
  private EntityStore entityStore;
  private MapTestFixtures mapTestFixtures;
  private ZoneFactory zoneFactory;

  @BeforeEach
  void setUp() throws Exception {
    // Clean up any stale test files
    new File("test-store.dat").delete();
    new File("testfile3.dat").delete();

    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    // Load resources from sampleMod1 (same pattern as ModLoader)
    //
    TestEngineContext.loadTestResourceViaConfig("src/test/resources/neon.ini.sampleMod1.xml");
    // TestEngineContext.loadTestResources("src/test/resources/sampleMod1");
    testAtlas = TestEngineContext.getTestAtlas();
    entityStore = TestEngineContext.getTestUiEngineContext().getStore();
    mapTestFixtures =
        new MapTestFixtures(
            TestEngineContext.getTestResources(), TestEngineContext.getTestZoneFactory());
    zoneFactory = TestEngineContext.getTestZoneFactory();
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    new File("test-store.dat").delete();
    new File("testfile3.dat").delete();
  }

  /** QuestProvider that returns no quest objects. */
  private class NoQuestProvider implements QuestProvider {
    @Override
    public String getNextRequestedObject() {
      return null;
    }
  }

  // ==================== Tests Using generate() ====================

  @ParameterizedTest(name = "generate() with XML theme: {0}")
  @MethodSource("neon.maps.generators.DungeonGeneratorXmlIntegrationTest#zoneThemeScenarios")
  void generate_withXmlZoneTheme_createsZoneWithRegions(
      DungeonGeneratorXmlIntegrationTest.ZoneThemeScenario scenario) throws Exception {
    // Given: Set up dungeon structure
    int mapUID = entityStore.createNewMapUID();
    Dungeon dungeon = new Dungeon("test-dungeon-" + scenario.zoneId(), mapUID, zoneFactory);
    dungeon.addZone(0, "zone-0"); // Previous zone
    dungeon.addZone(1, "zone-1", scenario.theme()); // Target zone with theme

    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));
    testAtlas.setMap(dungeon);

    // Create entry door in previous zone
    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    // Create generator with full dependencies
    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When: Call the full generate() method (same entry point as engine uses)
    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: Verify actual entities were created
    assertFalse(targetZone.getRegions().isEmpty(), "Zone should have regions");
    assertHasReturnDoor(targetZone, previousZone.getIndex());
  }

  @ParameterizedTest(name = "generate() door linking: {0}")
  @MethodSource("neon.maps.generators.DungeonGeneratorXmlIntegrationTest#zoneThemeScenarios")
  void generate_withXmlZoneTheme_linksDoorsCorrectly(
      DungeonGeneratorXmlIntegrationTest.ZoneThemeScenario scenario) throws Exception {
    // Given
    int mapUID = entityStore.createNewMapUID();
    Dungeon dungeon = new Dungeon("test-dungeon-" + scenario.zoneId(), mapUID, zoneFactory);
    dungeon.addZone(0, "zone-0");
    dungeon.addZone(1, "zone-1", scenario.theme());

    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));
    testAtlas.setMap(dungeon);

    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 10, 10, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(scenario.seed()),
            Dice.withSeed(scenario.seed()));

    // When
    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: entry door should have destination position set
    assertNotNull(entryDoor.portal.getDestPos(), "Entry door should have destination position");

    // Find return door and verify it links back
    Door returnDoor = findReturnDoor(targetZone, previousZone.getIndex());
    assertNotNull(returnDoor, "Should have a return door");
    assertNotNull(returnDoor.portal.getDestPos(), "Return door should have destination position");
    assertEquals(10, returnDoor.portal.getDestPos().x, "Return door should point to entry door X");
    assertEquals(10, returnDoor.portal.getDestPos().y, "Return door should point to entry door Y");
  }

  // ==================== Helper Methods ====================

  private void assertHasReturnDoor(Zone zone, int previousZoneIndex) {
    boolean hasReturnDoor = false;
    for (long itemUid : zone.getItems()) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof Door door) {
        if (door.portal.getDestZone() == previousZoneIndex) {
          hasReturnDoor = true;
          break;
        }
      }
    }
    assertTrue(hasReturnDoor, "Zone should have return door to previous zone");
  }

  private Door findReturnDoor(Zone zone, int previousZoneIndex) {
    for (long itemUid : zone.getItems()) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof Door door && door.portal.getDestZone() == previousZoneIndex) {
        return door;
      }
    }
    return null;
  }
}
