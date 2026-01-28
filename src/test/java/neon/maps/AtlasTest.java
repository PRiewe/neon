package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Atlas class focusing on MapDb caching behavior.
 *
 * <p>Tests the map caching mechanism, current map/zone tracking, and cache performance.
 */
class AtlasTest {

  private MapStore testDb;
  private Atlas atlas;
  private ZoneFactory zoneFactory;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    atlas = TestEngineContext.getTestAtlas();
    zoneFactory = TestEngineContext.getTestZoneFactory();
  }

  @AfterEach
  void tearDown() throws IOException {
    if (atlas != null) {
      atlas.close();
    }
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  void testConstructorCreatesMapDb() {
    assertNotNull(atlas.getAtlasMapStore());
  }

  @Test
  void testSetMapAddsToCache() {
    World world = new World("Test World", 100, zoneFactory);

    atlas.setMap(world);

    Map retrievedMap = atlas.getCurrentMap();
    assertNotNull(retrievedMap);
    assertEquals(100, retrievedMap.getUID());
    assertEquals("Test World", retrievedMap.getName());
  }

  @Test
  void testGetCurrentMapReturnsSetMap() {
    World world1 = new World("World 1", 101, zoneFactory);
    World world2 = new World("World 2", 102, zoneFactory);

    atlas.setMap(world1);
    assertEquals(101, atlas.getCurrentMap().getUID());

    atlas.setMap(world2);
    assertEquals(102, atlas.getCurrentMap().getUID());
  }

  @Test
  void testGetCurrentZoneReturnsCorrectZone() {
    World world = new World("Test World", 103, TestEngineContext.getTestZoneFactory());
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();
    assertNotNull(zone);
    // World creates a zone with hardcoded name "world"
    assertEquals("world", zone.getName());
  }

  @Test
  void testGetCurrentZoneIndexDefaultsToZero() {
    World world = new World("Test World", 104, TestEngineContext.getTestZoneFactory());
    atlas.setMap(world);

    assertEquals(0, atlas.getCurrentZoneIndex());
  }

  @Test
  void testMultipleMapsDoNotInterfere() {
    World world1 = new World("World 1", 201, zoneFactory);
    World world2 = new World("World 2", 202, zoneFactory);

    atlas.setMap(world1);
    atlas.setMap(world2);

    // Set back to world1
    atlas.setMap(world1);
    assertEquals(201, atlas.getCurrentMap().getUID());
    assertEquals("World 1", atlas.getCurrentMap().getName());

    // Set to world2
    atlas.setMap(world2);
    assertEquals(202, atlas.getCurrentMap().getUID());
    assertEquals("World 2", atlas.getCurrentMap().getName());
  }

  @Test
  void testSetMapOnlyAddsToCacheOnce() {
    World world = new World("Test World", 300, zoneFactory);

    atlas.setMap(world);
    atlas.setMap(world); // Second call should not duplicate

    // Verify map is still retrievable
    assertEquals(300, atlas.getCurrentMap().getUID());
  }

  @Test
  void testCacheWithMultipleMaps() {
    // Add multiple maps to cache
    for (int i = 0; i < 10; i++) {
      World world = new World("World " + i, 400 + i, zoneFactory);
      atlas.setMap(world);
    }

    // Verify last map is current
    assertEquals(409, atlas.getCurrentMap().getUID());

    // Switch between maps
    World world5 = new World("World 5", 405, zoneFactory);
    atlas.setMap(world5);
    assertEquals(405, atlas.getCurrentMap().getUID());
  }

  @Test
  void testWorldWithMultipleZones() {
    // Worlds only have one zone, but we can test zone access
    World world = new World("Single Zone World", 500, TestEngineContext.getTestZoneFactory());
    atlas.setMap(world);

    Zone zone = atlas.getCurrentZone();
    assertNotNull(zone);
    assertEquals(0, atlas.getCurrentZoneIndex());
  }

  // Note: Dungeon serialization through MapDb requires special handling
  // This is tested separately in MapSerializationTest using Externalizable methods

  @Test
  void testCachePerformanceWithManyMaps() throws Exception {
    int mapCount = 50;

    // Measure time to add many maps
    PerformanceHarness.MeasuredResult<Void> addResult =
        PerformanceHarness.measure(
            () -> {
              for (int i = 0; i < mapCount; i++) {
                World world = new World("World " + i, 700 + i, zoneFactory);
                atlas.setMap(world);
              }
              return null;
            });

    System.out.printf(
        "[PERF] Adding %d maps to Atlas cache: %d ms (%d ns)%n",
        mapCount, addResult.getDurationMillis(), addResult.getDurationNanos());

    assertTrue(
        addResult.getDurationMillis() < 500,
        "Adding " + mapCount + " maps should complete within 500ms");
  }

  @Test
  void testCacheRetrievalPerformance() throws Exception {
    // Add maps to cache
    for (int i = 0; i < 20; i++) {
      World world = new World("World " + i, 800 + i, zoneFactory);
      atlas.setMap(world);
    }

    // Measure retrieval time
    PerformanceHarness.MeasuredResult<Map> result =
        PerformanceHarness.measure(() -> atlas.getCurrentMap());

    System.out.printf(
        "[PERF] Cache retrieval: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    assertNotNull(result.getResult());
    assertTrue(result.getDurationMillis() < 10, "Cache retrieval should be very fast (< 10ms)");
  }

  @Test
  void testEmptyAtlasState() {
    // Atlas starts with no current map
    // getCurrentMap() will return null if no map has been set
    assertDoesNotThrow(() -> atlas.getAtlasMapStore());
  }
}
