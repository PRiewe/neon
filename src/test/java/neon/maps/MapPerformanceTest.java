package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import neon.entities.Creature;
import neon.entities.Item;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive performance tests for map system.
 *
 * <p>Tests performance of Region, Zone, and Atlas operations under various loads.
 */
class MapPerformanceTest {

  private MapStore testDb;
  ZoneFactory zoneFactory;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    zoneFactory = TestEngineContext.getTestZoneFactory();
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  // ==================== Region Performance Tests ====================

  @Test
  void testRegionCreationPerformance() throws Exception {
    int regionCount = 1000;

    PerformanceHarness.MeasuredResult<List<Region>> result =
        PerformanceHarness.measure(
            () -> {
              List<Region> regions = new ArrayList<>();
              for (int i = 0; i < regionCount; i++) {
                Region region = MapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10);
                regions.add(region);
              }
              return regions;
            });

    System.out.printf(
        "[PERF] Create %d regions: %d ms (%d ns)%n",
        regionCount, result.getDurationMillis(), result.getDurationNanos());

    assertEquals(regionCount, result.getResult().size());
    assertTrue(
        result.getDurationMillis() < 500, regionCount + " regions should create within 500ms");
  }

  @Test
  void testRegionScriptOperationsPerformance() throws Exception {
    Region region = MapTestFixtures.createTestRegion(0, 0, 100, 100);

    PerformanceHarness.MeasuredResult<Integer> addResult =
        PerformanceHarness.measure(
            () -> {
              for (int i = 0; i < 100; i++) {
                region.addScript("script" + i + ".js", false);
              }
              return region.getScripts().size();
            });

    System.out.printf(
        "[PERF] Add 100 scripts to region: %d ms (%d ns)%n",
        addResult.getDurationMillis(), addResult.getDurationNanos());

    assertEquals(100, addResult.getResult());

    PerformanceHarness.MeasuredResult<Integer> removeResult =
        PerformanceHarness.measure(
            () -> {
              for (int i = 0; i < 50; i++) {
                region.removeScript("script" + i + ".js");
              }
              return region.getScripts().size();
            });

    System.out.printf(
        "[PERF] Remove 50 scripts from region: %d ms (%d ns)%n",
        removeResult.getDurationMillis(), removeResult.getDurationNanos());

    assertEquals(50, removeResult.getResult());
  }

  @Test
  void testRegionPropertyAccessPerformance() throws Exception {
    Region region = MapTestFixtures.createTestRegion("perf-region", 100, 200, 50, 75, 3);
    region.setLabel("Performance Test Region");
    region.addScript("test.js", false);

    int iterations = 100000;

    PerformanceHarness.MeasuredResult<Long> result =
        PerformanceHarness.measure(
            () -> {
              long sum = 0;
              for (int i = 0; i < iterations; i++) {
                sum += region.getX();
                sum += region.getY();
                sum += region.getZ();
                sum += region.getWidth();
                sum += region.getHeight();
                region.getLabel();
                region.getBounds();
                region.getScripts();
              }
              return sum;
            });

    System.out.printf(
        "[PERF] %d region property accesses: %d ms (%d ns)%n",
        iterations * 8, result.getDurationMillis(), result.getDurationNanos());

    assertTrue(
        result.getDurationMillis() < 100,
        "Property access should be very fast (< 100ms for " + iterations + " iterations)");
  }

  // ==================== Zone Performance Tests ====================

  @Test
  void testZoneRegionInsertionPerformance() throws Exception {
    Zone zone = zoneFactory.createZone("perf-zone", 1000, 0);
    int regionCount = 500;
    int creaturesPerRegion = 10;
    int itemsPerRegion = 10;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              long uidCounter = 10000;
              for (int i = 0; i < regionCount; i++) {
                int x = (i % 25) * 20;
                int y = (i / 25) * 20;
                Region region = MapTestFixtures.createTestRegion("r" + i, x, y, 20, 20, i % 3);
                zone.addRegion(region);

                // Add creatures to zone
                for (int c = 0; c < creaturesPerRegion; c++) {
                  Creature creature =
                      MapTestFixtures.createTestCreature(
                          "creature-" + i + "-" + c, uidCounter++, x + c * 5, y + c * 5);
                  zone.addCreature(creature);
                }

                // Add items to zone
                for (int it = 0; it < itemsPerRegion; it++) {
                  Item item =
                      MapTestFixtures.createTestItem(
                          "item-" + i + "-" + it, uidCounter++, x + it * 3, y + it * 3);
                  zone.addItem(item);
                }
              }
              testDb.commit();
              return zone.getRegions().size();
            });

    System.out.printf(
        "[PERF] Insert %d regions + %d creatures + %d items into zone: %d ms (%d ns)%n",
        regionCount,
        regionCount * creaturesPerRegion,
        regionCount * itemsPerRegion,
        result.getDurationMillis(),
        result.getDurationNanos());

    assertEquals(regionCount, result.getResult());
    assertTrue(
        result.getDurationMillis() < 5000,
        "Inserting "
            + regionCount
            + " regions with creatures and items should complete within 5 seconds");
  }

  @Test
  void testZoneSpatialQueryPerformanceAtScale() throws Exception {
    Zone zone = zoneFactory.createZone("spatial-perf-zone", 1001, 0);

    // Create large zone with 500 regions, plus creatures and items
    long uidCounter = 20000;
    for (int i = 0; i < 500; i++) {
      int x = (i % 50) * 10;
      int y = (i / 50) * 10;
      Region region = MapTestFixtures.createTestRegion("r" + i, x, y, 10, 10, 0);
      zone.addRegion(region);

      // Add 1 creature and 2 items per region
      Creature creature = MapTestFixtures.createTestCreature("c" + i, uidCounter++, x + 2, y + 2);
      zone.addCreature(creature);

      Item item1 = MapTestFixtures.createTestItem("i" + i + "-1", uidCounter++, x + 3, y + 3);
      Item item2 = MapTestFixtures.createTestItem("i" + i + "-2", uidCounter++, x + 4, y + 4);
      zone.addItem(item1);
      zone.addItem(item2);
    }

    testDb.commit();

    int queryCount = 500;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              int totalFound = 0;
              for (int i = 0; i < queryCount; i++) {
                int x = (i % 40) * 10;
                int y = (i % 40) * 10;
                Rectangle query = new Rectangle(x, y, 50, 50);
                Collection<Region> found = zone.getRegions(query);
                totalFound += found.size();
              }
              return totalFound;
            });

    System.out.printf(
        "[PERF] %d spatial queries on zone with 1000 regions + 1000 creatures + 2000 items: %d ms (%d ns), found %d total%n",
        queryCount, result.getDurationMillis(), result.getDurationNanos(), result.getResult());

    assertTrue(result.getResult() > 0, "Should find some regions");
    assertTrue(
        result.getDurationMillis() < 5000,
        queryCount + " queries should complete within 5 seconds");
  }

  @Test
  void testZoneBulkRegionAddition() throws Exception {
    Zone zone = zoneFactory.createZone("bulk-add-zone", 1002, 0);

    // Measure bulk addition time including creatures and items
    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              long uidCounter = 30000;
              for (int i = 0; i < 300; i++) {
                Region region = MapTestFixtures.createTestRegion("r" + i, i * 5, i * 5, 10, 10, 0);
                zone.addRegion(region);

                // Add 2 creatures and 2 items per region
                Creature c1 =
                    MapTestFixtures.createTestCreature("c" + i + "-1", uidCounter++, i * 5, i * 5);
                Creature c2 =
                    MapTestFixtures.createTestCreature(
                        "c" + i + "-2", uidCounter++, i * 5 + 2, i * 5 + 2);
                zone.addCreature(c1);
                zone.addCreature(c2);

                Item it1 =
                    MapTestFixtures.createTestItem(
                        "it" + i + "-1", uidCounter++, i * 5 + 1, i * 5 + 1);
                Item it2 =
                    MapTestFixtures.createTestItem(
                        "it" + i + "-2", uidCounter++, i * 5 + 3, i * 5 + 3);
                zone.addItem(it1);
                zone.addItem(it2);
              }
              testDb.commit();
              return zone.getRegions().size();
            });

    System.out.printf(
        "[PERF] Add 300 regions + 600 creatures + 600 items to zone: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    assertEquals(300, result.getResult());
    assertTrue(
        result.getDurationMillis() < 5000,
        "Adding 300 regions with creatures and items should complete within 5 seconds");
  }

  @Test
  void testZoneGetRegionByPositionPerformance() throws Exception {
    Zone zone = zoneFactory.createZone("position-perf-zone", 1003, 0);

    // Create 100x100 grid (10,000 regions) with creatures and items
    long uidCounter = 40000;
    for (int y = 0; y < 100; y++) {
      for (int x = 0; x < 100; x++) {
        Region region = MapTestFixtures.createTestRegion("r-" + x + "-" + y, x * 5, y * 5, 5, 5, 0);
        zone.addRegion(region);

        // Add 1 creature per region (every 10th region to keep memory reasonable)
        if ((x + y) % 10 == 0) {
          Creature creature =
              MapTestFixtures.createTestCreature(
                  "c-" + x + "-" + y, uidCounter++, x * 5 + 1, y * 5 + 1);
          zone.addCreature(creature);
        }

        // Add 1 item per region (every 5th region)
        if ((x + y) % 5 == 0) {
          Item item =
              MapTestFixtures.createTestItem(
                  "i-" + x + "-" + y, uidCounter++, x * 5 + 2, y * 5 + 2);
          zone.addItem(item);
        }
      }
    }

    testDb.commit();

    int lookupCount = 10000;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              int foundCount = 0;
              for (int i = 0; i < lookupCount; i++) {
                int x = (i % 500);
                int y = (i % 500);
                Region found = zone.getRegion(new java.awt.Point(x, y));
                if (found != null) foundCount++;
              }
              return foundCount;
            });

    System.out.printf(
        "[PERF] %d position lookups in zone with 10,000 regions + creatures + items: %d ms (%d ns), found %d%n",
        lookupCount, result.getDurationMillis(), result.getDurationNanos(), result.getResult());

    assertTrue(result.getResult() > 0, "Should find many regions");
    assertTrue(
        result.getDurationMillis() < 5000,
        lookupCount + " lookups should complete within 5 seconds");
  }

  @Test
  void testZoneMultiLayerPerformance() throws Exception {
    Zone zone = zoneFactory.createZone("multilayer-perf-zone", 1004, 0);

    int layerCount = 10;
    int regionsPerLayer = 50;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              long uidCounter = 60000;
              for (int z = 0; z < layerCount; z++) {
                for (int i = 0; i < regionsPerLayer; i++) {
                  Region region =
                      MapTestFixtures.createTestRegion(
                          "r-z" + z + "-" + i, i * 10, z * 10, 10, 10, z);
                  zone.addRegion(region);

                  // Add 1 creature and 1 item per region
                  Creature creature =
                      MapTestFixtures.createTestCreature(
                          "c-z" + z + "-" + i, uidCounter++, i * 10 + 2, z * 10 + 2);
                  zone.addCreature(creature);

                  Item item =
                      MapTestFixtures.createTestItem(
                          "i-z" + z + "-" + i, uidCounter++, i * 10 + 3, z * 10 + 3);
                  zone.addItem(item);
                }
              }
              testDb.commit();
              return zone.getRegions().size();
            });

    System.out.printf(
        "[PERF] Create zone with %d layers, %d regions + %d creatures + %d items: %d ms (%d ns)%n",
        layerCount,
        layerCount * regionsPerLayer,
        layerCount * regionsPerLayer,
        layerCount * regionsPerLayer,
        result.getDurationMillis(),
        result.getDurationNanos());

    assertEquals(layerCount * regionsPerLayer, result.getResult());

    // Query regions at different positions to verify layering
    long startTime = System.nanoTime();
    for (int z = 0; z < layerCount; z++) {
      // Query a point that should have a region at this z-level
      java.awt.Point p = new java.awt.Point(5, z * 10 + 5);
      Collection<Region> regionsAtPoint = zone.getRegions(p);
      assertTrue(regionsAtPoint.size() >= 1, "Should find region at layer " + z);
    }
    long endTime = System.nanoTime();
    long queryMs = (endTime - startTime) / 1_000_000;

    System.out.printf("[PERF] Query %d layer positions: %d ms%n", layerCount, queryMs);

    assertTrue(queryMs < 1000, "Layer queries should be fast");
  }

  // ==================== Atlas Performance Tests ====================

  @Test
  void testAtlasMapCachingPerformance() throws Exception {
    Atlas atlas = TestEngineContext.getTestAtlas();
    AtlasPosition atlasPosition =
        new AtlasPosition(TestEngineContext.getGameStores(), TestEngineContext.getQuestTracker());
    int mapCount = 100;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              for (int i = 0; i < mapCount; i++) {
                World world = new World("World " + i, 2000 + i, zoneFactory);
                atlasPosition.setMap(world);
              }
              return mapCount;
            });

    System.out.printf(
        "[PERF] Cache %d maps in Atlas: %d ms (%d ns)%n",
        mapCount, result.getDurationMillis(), result.getDurationNanos());

    assertTrue(result.getDurationMillis() < 1000, "Caching " + mapCount + " maps should be fast");

    atlas.getCache().close();
  }

  @Test
  void testAtlasMapSwitchingPerformance() throws Exception {
    Atlas atlas = TestEngineContext.getTestAtlas();
    AtlasPosition atlasPosition =
        new AtlasPosition(TestEngineContext.getGameStores(), TestEngineContext.getQuestTracker());

    // Create and cache 50 maps
    List<World> worlds = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      World world = new World("World " + i, 3000 + i, zoneFactory);
      worlds.add(world);
      atlasPosition.setMap(world);
    }

    int switchCount = 1000;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              for (int i = 0; i < switchCount; i++) {
                World world = worlds.get(i % worlds.size());
                atlasPosition.setMap(world);
                atlasPosition.getCurrentMap();
              }
              return switchCount;
            });

    System.out.printf(
        "[PERF] %d map switches in Atlas: %d ms (%d ns)%n",
        switchCount, result.getDurationMillis(), result.getDurationNanos());

    assertTrue(
        result.getDurationMillis() < 1000,
        switchCount + " switches should complete within 1 second");

    atlas.getCache().close();
  }

  @Test
  @Disabled
  void testAtlasZoneAccessPerformance() throws Exception {
    Atlas atlas = TestEngineContext.getTestAtlas();
    AtlasPosition atlasPosition =
        new AtlasPosition(TestEngineContext.getGameStores(), TestEngineContext.getQuestTracker());

    World world = new World("Zone Access World", 4000, zoneFactory);
    atlasPosition.setMap(world);

    Zone zone = atlasPosition.getCurrentZone();
    for (int i = 0; i < 100; i++) {
      Region region = MapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10);
      zone.addRegion(region);
    }

    testDb.commit();

    int accessCount = 10000;

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              int sum = 0;
              for (int i = 0; i < accessCount; i++) {
                Zone z = atlasPosition.getCurrentZone();
                sum += z.getRegions().size();
              }
              return sum;
            });

    System.out.printf(
        "[PERF] %d zone accesses through Atlas: %d ms (%d ns)%n",
        accessCount, result.getDurationMillis(), result.getDurationNanos());

    assertTrue(
        result.getDurationMillis() < 10000,
        accessCount + " zone accesses should complete within 10 seconds");

    atlas.getCache().close();
  }

  // ==================== Integration Performance Tests ====================

  @Test
  void testFullMapLoadAndQueryPerformance() throws Exception {
    Atlas atlas = TestEngineContext.getTestAtlas();
    AtlasPosition atlasPosition =
        new AtlasPosition(TestEngineContext.getGameStores(), TestEngineContext.getQuestTracker());

    PerformanceHarness.MeasuredResult<Integer> result =
        PerformanceHarness.measure(
            () -> {
              // Create a large world
              World world = new World("Large World", 5000, zoneFactory);
              atlasPosition.setMap(world);

              Zone zone = atlasPosition.getCurrentZone();

              // Add 500 regions with creatures and items
              long uidCounter = 70000;
              for (int i = 0; i < 500; i++) {
                int x = (i % 25) * 20;
                int y = (i / 25) * 20;
                Region region = MapTestFixtures.createTestRegion("r" + i, x, y, 20, 20, i % 3);
                region.setLabel("Region " + i);
                region.addScript("script" + i + ".js", false);
                zone.addRegion(region);

                // Add 2 creatures and 3 items per region
                Creature c1 =
                    MapTestFixtures.createTestCreature("c" + i + "-1", uidCounter++, x + 2, y + 2);
                Creature c2 =
                    MapTestFixtures.createTestCreature("c" + i + "-2", uidCounter++, x + 4, y + 4);
                zone.addCreature(c1);
                zone.addCreature(c2);

                Item it1 =
                    MapTestFixtures.createTestItem("it" + i + "-1", uidCounter++, x + 1, y + 1);
                Item it2 =
                    MapTestFixtures.createTestItem("it" + i + "-2", uidCounter++, x + 3, y + 3);
                Item it3 =
                    MapTestFixtures.createTestItem("it" + i + "-3", uidCounter++, x + 5, y + 5);
                zone.addItem(it1);
                zone.addItem(it2);
                zone.addItem(it3);
              }

              testDb.commit();

              // Perform 100 spatial queries
              int foundTotal = 0;
              for (int i = 0; i < 100; i++) {
                Rectangle query = new Rectangle(i * 5, i * 5, 100, 100);
                Collection<Region> found = zone.getRegions(query);
                foundTotal += found.size();
              }

              return foundTotal;
            });

    System.out.printf(
        "[PERF] Full workflow (500 regions + 1000 creatures + 1500 items + 100 queries): %d ms (%d ns), found %d total%n",
        result.getDurationMillis(), result.getDurationNanos(), result.getResult());

    assertTrue(result.getResult() > 0, "Should find regions");
    assertTrue(
        result.getDurationMillis() < 10000,
        "Full workflow with creatures and items should complete within 10 seconds");

    atlas.getCache().close();
  }

  @Test
  void testMemoryEfficiencyWithLargeMaps() throws Exception {
    Atlas atlas = TestEngineContext.getTestAtlas();
    AtlasPosition atlasPosition =
        new AtlasPosition(TestEngineContext.getGameStores(), TestEngineContext.getQuestTracker());

    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    // Create 10 large worlds
    for (int w = 0; w < 10; w++) {
      World world = new World("World " + w, 6000 + w, zoneFactory);
      atlasPosition.setMap(world);

      Zone zone = atlasPosition.getCurrentZone();
      for (int i = 0; i < 200; i++) {
        Region region = MapTestFixtures.createTestRegion(i * 5, i * 5, 10, 10);
        zone.addRegion(region);
      }

      testDb.commit();
    }

    runtime.gc();
    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024; // MB

    System.out.printf(
        "[PERF] Memory used for 10 worlds (2000 total regions): ~%d MB%n", memoryUsed);

    // Very lenient assertion - just checking it doesn't explode
    assertTrue(memoryUsed < 500, "Memory usage should be reasonable");

    atlas.getCache().close();
  }
}
