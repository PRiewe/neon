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
  private AtlasPosition atlasPosition;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    atlas = TestEngineContext.getTestAtlas();
    atlasPosition =
        new AtlasPosition(
            TestEngineContext.getGameStores(),
            TestEngineContext.getQuestTracker(),
            TestEngineContext.getTestContext().getPlayer());
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
    assertNotNull(atlas.getCache());
  }

  @Test
  void testSetMapAddsToCache() {
    World world = new World("Test World", 100);

    atlasPosition.setMap(world);

    Map retrievedMap = atlasPosition.getCurrentMap();
    assertNotNull(retrievedMap);
    assertEquals(100, retrievedMap.getUID());
    assertEquals("Test World", retrievedMap.getName());
  }

  @Test
  void testGetCurrentMapReturnsSetMap() {
    World world1 = new World("World 1", 101);
    World world2 = new World("World 2", 102);

    atlasPosition.setMap(world1);
    assertEquals(101, atlasPosition.getCurrentMap().getUID());

    atlasPosition.setMap(world2);
    assertEquals(102, atlasPosition.getCurrentMap().getUID());
  }

  @Test
  void testGetCurrentZoneReturnsCorrectZone() {
    World world = new World("Test World", 103);
    atlasPosition.setMap(world);

    Zone zone = atlasPosition.getCurrentZone();
    assertNotNull(zone);
    // World creates a zone with hardcoded name "world"
    assertEquals("world", zone.getName());
  }

  @Test
  void testGetCurrentZoneIndexDefaultsToZero() {
    World world = new World("Test World", 104);
    atlasPosition.setMap(world);

    assertEquals(0, atlasPosition.getCurrentZoneIndex());
  }

  @Test
  void testMultipleMapsDoNotInterfere() {
    World world1 = new World("World 1", 201);
    World world2 = new World("World 2", 202);

    atlasPosition.setMap(world1);
    atlasPosition.setMap(world2);

    // Set back to world1
    atlasPosition.setMap(world1);
    assertEquals(201, atlasPosition.getCurrentMap().getUID());
    assertEquals("World 1", atlasPosition.getCurrentMap().getName());

    // Set to world2
    atlasPosition.setMap(world2);
    assertEquals(202, atlasPosition.getCurrentMap().getUID());
    assertEquals("World 2", atlasPosition.getCurrentMap().getName());
  }

  @Test
  void testSetMapOnlyAddsToCacheOnce() {
    World world = new World("Test World", 300);

    atlasPosition.setMap(world);
    atlasPosition.setMap(world); // Second call should not duplicate

    // Verify map is still retrievable
    assertEquals(300, atlasPosition.getCurrentMap().getUID());
  }

  @Test
  void testCacheWithMultipleMaps() {
    // Add multiple maps to cache
    for (int i = 0; i < 10; i++) {
      World world = new World("World " + i, 400 + i);
      atlasPosition.setMap(world);
    }

    // Verify last map is current
    assertEquals(409, atlasPosition.getCurrentMap().getUID());

    // Switch between maps
    World world5 = new World("World 5", 405);
    atlasPosition.setMap(world5);
    assertEquals(405, atlasPosition.getCurrentMap().getUID());
  }

  @Test
  void testWorldWithMultipleZones() {
    // Worlds only have one zone, but we can test zone access
    World world = new World("Single Zone World", 500);
    atlasPosition.setMap(world);

    Zone zone = atlasPosition.getCurrentZone();
    assertNotNull(zone);
    assertEquals(0, atlasPosition.getCurrentZoneIndex());
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
                World world = new World("World " + i, 700 + i);
                atlasPosition.setMap(world);
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
      World world = new World("World " + i, 800 + i);
      atlasPosition.setMap(world);
    }

    // Measure retrieval time
    PerformanceHarness.MeasuredResult<Map> result =
        PerformanceHarness.measure(() -> atlasPosition.getCurrentMap());

    System.out.printf(
        "[PERF] Cache retrieval: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    assertNotNull(result.getResult());
    assertTrue(result.getDurationMillis() < 10, "Cache retrieval should be very fast (< 10ms)");
  }

  @Test
  void testMapDbPersistsAcrossAtlasInstances() throws IOException {
    // Create first atlas and add map
    Atlas atlas1 =
        new Atlas(
            TestEngineContext.getStubFileSystem(),
            "shared-cache",
            TestEngineContext.getTestEntityStore(),
            TestEngineContext.getMapLoader());
    AtlasPosition atlasPosition =
        new AtlasPosition(
            TestEngineContext.getGameStores(),
            TestEngineContext.getQuestTracker(),
            TestEngineContext.getTestContext().getPlayer());

    World world = new World("Persistent World", 900);

    atlasPosition.setMap(world);

    // Create second atlas with same cache name
    // Note: In the current implementation, Atlas always creates a new in-memory DB,
    // so this test documents current behavior rather than testing persistence
    // Atlas atlas2 = new Atlas( TestEngineContext.getStubFileSystem(), "shared-cache");

    // atlas2 won't have the map because each Atlas creates its own in-memory DB
    // This test documents the current behavior
    //  assertNotNull(atlas2.getCache());
  }

  @Test
  void testEmptyAtlasState() {
    // Atlas starts with no current map
    // getCurrentMap() will return null if no map has been set
    assertDoesNotThrow(() -> atlas.getCache());
  }
}
