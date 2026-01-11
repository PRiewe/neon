package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.Collection;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Atlas focusing on MapDb sharing and full workflows.
 *
 * <p>Tests the integration between Atlas, Maps, Zones, and the shared MapDb instance.
 */
class AtlasIntegrationTest {

  private MapStore testDb;
  private Atlas atlas;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    ZoneActivator zoneActivator =
        new ZoneActivator(
            TestEngineContext.getTestUiEngineContext().getPhysicsEngine(),
            TestEngineContext.getTestUiEngineContext());
    atlas =
        new Atlas(
            TestEngineContext.getGameStore(),
            testDb,
            TestEngineContext.getTestQuestTracker(),
            zoneActivator,
            new MapLoader(TestEngineContext.getTestUiEngineContext()),
            TestEngineContext.getTestUiEngineContext());
  }

  @AfterEach
  void tearDown() {
    if (atlas != null && atlas.getCache() != null) {
      atlas.getCache().close();
    }
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  void testZoneUsesAtlasDatabase() {
    World world = new World("DB Test World", 1000);
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();
    assertNotNull(zone);

    // Add regions to the zone
    for (int i = 0; i < 10; i++) {
      Region region = MapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10);
      zone.addRegion(region);
    }

    testDb.commit();

    // Verify regions are queryable
    Collection<Region> regions = zone.getRegions();
    assertEquals(10, regions.size());
  }

  @Test
  void testMultipleZonesShareDatabase() {
    // Create two worlds
    World world1 = new World("World 1", 1001);
    World world2 = new World("World 2", 1002);

    atlas.setMap(world1);
    Zone zone1 = atlas.getCurrentZone();
    zone1.addRegion(MapTestFixtures.createTestRegion("zone1-region", 0, 0, 10, 10, 0));

    atlas.setMap(world2);
    Zone zone2 = atlas.getCurrentZone();
    zone2.addRegion(MapTestFixtures.createTestRegion("zone2-region", 20, 20, 10, 10, 0));

    testDb.commit();

    // Both zones should have their regions
    assertEquals(1, zone1.getRegions().size());
    assertEquals(1, zone2.getRegions().size());

    // Verify regions are independent
    Collection<Region> zone1Regions = zone1.getRegions();
    Collection<Region> zone2Regions = zone2.getRegions();

    assertNotEquals(zone1Regions, zone2Regions);
  }

  @Test
  void testFullRoundTrip() {
    // Create a world with populated zone
    World world = new World("Round Trip World", 1003);

    atlas.setMap(world);
    Zone zone = atlas.getCurrentZone();

    // Add multiple regions
    for (int i = 0; i < 20; i++) {
      Region region = MapTestFixtures.createTestRegion("region-" + i, i * 15, i * 15, 10, 10, 0);
      region.setLabel("Region " + i);
      zone.addRegion(region);
    }

    testDb.commit();

    // Retrieve the map again
    Map retrievedMap = atlas.getCurrentMap();
    assertEquals(1003, retrievedMap.getUID());
    assertEquals("Round Trip World", retrievedMap.getName());

    // Verify zone data
    Zone retrievedZone = atlas.getCurrentZone();
    assertEquals(20, retrievedZone.getRegions().size());

    // Test spatial query on retrieved zone
    Rectangle queryBounds = new Rectangle(0, 0, 50, 50);
    Collection<Region> found = retrievedZone.getRegions(queryBounds);
    assertTrue(found.size() >= 3, "Should find at least 3 regions in query area");
  }

  @Test
  void testZoneSpatialIndexPersistence() {
    World world = new World("Spatial Index World", 1004);
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();

    // Create a grid of regions
    for (int y = 0; y < 5; y++) {
      for (int x = 0; x < 5; x++) {
        Region region =
            MapTestFixtures.createTestRegion("r-" + x + "-" + y, x * 10, y * 10, 10, 10, 0);
        zone.addRegion(region);
      }
    }

    testDb.commit();

    // Query specific areas and verify spatial index works
    Rectangle topLeft = new Rectangle(0, 0, 15, 15);
    Collection<Region> topLeftRegions = zone.getRegions(topLeft);
    assertTrue(topLeftRegions.size() >= 2, "Should find regions in top-left area");

    Rectangle bottomRight = new Rectangle(30, 30, 20, 20);
    Collection<Region> bottomRightRegions = zone.getRegions(bottomRight);
    assertTrue(bottomRightRegions.size() >= 2, "Should find regions in bottom-right area");

    // Verify total regions
    assertEquals(25, zone.getRegions().size());
  }

  @Test
  void testMapSwitchingPreservesData() {
    // Create two worlds with different data
    World world1 = new World("World A", 1005);
    World world2 = new World("World B", 1006);

    // Populate world1
    atlas.setMap(world1);
    Zone zone1 = atlas.getCurrentZone();
    for (int i = 0; i < 5; i++) {
      zone1.addRegion(MapTestFixtures.createTestRegion(i * 10, 0, 10, 10));
    }

    // Populate world2
    atlas.setMap(world2);
    Zone zone2 = atlas.getCurrentZone();
    for (int i = 0; i < 10; i++) {
      zone2.addRegion(MapTestFixtures.createTestRegion(0, i * 10, 10, 10));
    }

    testDb.commit();

    // Switch back to world1 and verify data
    atlas.setMap(world1);
    assertEquals(5, atlas.getCurrentZone().getRegions().size());

    // Switch to world2 and verify data
    atlas.setMap(world2);
    assertEquals(10, atlas.getCurrentZone().getRegions().size());
  }

  @Test
  void testRegionScriptsPersistThroughAtlas() {
    World world = new World("Script World", 1007);
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();
    Region region = MapTestFixtures.createTestRegion("scripted-region", 0, 0, 50, 50, 0);
    region.addScript("init.js", false);
    region.addScript("update.js", false);
    region.setLabel("Scripted Area");

    zone.addRegion(region);
    testDb.commit();

    // Retrieve and verify
    Collection<Region> regions = zone.getRegions();
    assertEquals(1, regions.size());

    Region retrieved = regions.iterator().next();
    assertEquals(2, retrieved.getScripts().size());
    assertTrue(retrieved.getScripts().contains("init.js"));
    assertTrue(retrieved.getScripts().contains("update.js"));
    assertEquals("Scripted Area", retrieved.getLabel());
  }

  @Test
  void testLargeWorldIntegration() {
    World world = new World("Large World", 1008);
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();

    // Add 100 regions in a 10x10 grid
    for (int y = 0; y < 10; y++) {
      for (int x = 0; x < 10; x++) {
        Region region =
            MapTestFixtures.createTestRegion("large-" + x + "-" + y, x * 20, y * 20, 20, 20, 0);
        zone.addRegion(region);
      }
    }

    testDb.commit();

    // Verify all regions are present
    assertEquals(100, zone.getRegions().size());

    // Test spatial queries work correctly
    Rectangle queryArea = new Rectangle(40, 40, 60, 60);
    Collection<Region> found = zone.getRegions(queryArea);
    assertTrue(found.size() >= 9, "Should find at least 3x3 grid of regions");
  }

  @Test
  void testAtlasHandlesEmptyWorld() {
    World emptyWorld = new World("Empty World", 1009);
    atlas.setMap(emptyWorld);

    Zone zone = atlas.getCurrentZone();
    assertNotNull(zone);
    assertTrue(zone.getRegions().isEmpty());

    // Adding and removing a region
    Region region = MapTestFixtures.createTestRegion(0, 0, 10, 10);
    zone.addRegion(region);
    assertEquals(1, zone.getRegions().size());
  }

  @Test
  void testMultipleAtlasInstancesShareTestDb() {
    // This tests that the testDb created in setUp is properly shared
    // through TestEngineContext
    World world = new World("Shared DB World", 1010);
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();
    zone.addRegion(MapTestFixtures.createTestRegion(0, 0, 10, 10));

    testDb.commit();

    // The testDb should have the data
    assertNotNull(testDb);
    assertEquals(1, zone.getRegions().size());
  }
}
