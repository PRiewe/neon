package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.io.*;
import java.util.Collection;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import neon.test.TestEngineContext;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Zone serialization and deserialization.
 *
 * <p>Verifies that Zone objects can be correctly serialized and deserialized, with special focus on
 * RTree spatial index persistence through MapDb. This is critical for map loading performance.
 */
class ZoneSerializationTest {

  private MVStore testDb;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  void testEmptyZoneRoundTrip() throws Exception {
    Zone original = new Zone("test-zone", 1, 0);

    Zone deserialized = serializeAndDeserialize(original);

    assertEquals("test-zone", deserialized.getName());
  }

  @Test
  void testZoneWithSingleRegion() throws Exception {
    Zone original = new Zone("zone-with-region", 2, 0);
    Region region = MapTestFixtures.createTestRegion(10, 20, 30, 40);
    original.addRegion(region);

    // Commit to MapDb so RTree is persisted
    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    assertEquals("zone-with-region", deserialized.getName());
    Collection<Region> regions = deserialized.getRegions();
    assertEquals(1, regions.size());
  }

  @Test
  void testZoneWithMultipleRegions() throws Exception {
    Zone original = new Zone("multi-region-zone", 3, 0);

    // Add 10 regions in a grid
    for (int i = 0; i < 10; i++) {
      Region region = MapTestFixtures.createTestRegion("region-" + i, i * 10, i * 10, 10, 10, 0);
      original.addRegion(region);
    }

    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    Collection<Region> regions = deserialized.getRegions();
    assertEquals(10, regions.size());
  }

  @Test
  void testZoneRTreeSpatialQueriesAfterDeserialization() throws Exception {
    Zone original = new Zone("spatial-test-zone", 4, 0);

    // Create regions at specific locations
    Region r1 = MapTestFixtures.createTestRegion("r1", 0, 0, 10, 10, 0);
    Region r2 = MapTestFixtures.createTestRegion("r2", 50, 50, 10, 10, 0);
    Region r3 = MapTestFixtures.createTestRegion("r3", 100, 100, 10, 10, 0);

    original.addRegion(r1);
    original.addRegion(r2);
    original.addRegion(r3);

    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    // Test spatial query: get regions overlapping (0, 0, 20, 20)
    Rectangle queryBounds = new Rectangle(0, 0, 20, 20);
    Collection<Region> found = deserialized.getRegions(queryBounds);

    // Should find r1 but not r2 or r3
    assertEquals(1, found.size());
  }

  @Test
  void testZoneRTreeRangeQueries() throws Exception {
    Zone original = new Zone("range-query-zone", 5, 0);

    // Create a 10x10 grid of regions
    for (int y = 0; y < 10; y++) {
      for (int x = 0; x < 10; x++) {
        Region region =
            MapTestFixtures.createTestRegion("r-" + x + "-" + y, x * 10, y * 10, 10, 10, 0);
        original.addRegion(region);
      }
    }

    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    // Query for regions in a specific area (20, 20, 30, 30) - should get 3x3 = 9 regions
    Rectangle queryBounds = new Rectangle(20, 20, 30, 30);
    Collection<Region> found = deserialized.getRegions(queryBounds);

    // Should find regions at (2,2), (2,3), (2,4), (3,2), (3,3), (3,4), (4,2), (4,3), (4,4)
    assertTrue(found.size() >= 9, "Should find at least 9 overlapping regions");
  }

  @Test
  void testZonePreservesMetadata() throws Exception {
    Zone original = new Zone("metadata-zone", 6, 5);

    Region region = MapTestFixtures.createTestRegion(0, 0, 50, 50);
    original.addRegion(region);

    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    assertEquals("metadata-zone", deserialized.getName());
    // Note: The Zone class has index and map fields but no public getters
    // We verify correctness by ensuring RTree reconstructs properly
    assertNotNull(deserialized.getRegions());
  }

  @Test
  void testLargeZoneSerialization() throws Exception {
    int regionCount = 100;
    Zone original = new Zone("large-zone", 7, 0);

    // Create 100 regions
    for (int i = 0; i < regionCount; i++) {
      int x = (i % 10) * 10;
      int y = (i / 10) * 10;
      Region region = MapTestFixtures.createTestRegion("r" + i, x, y, 10, 10, 0);
      original.addRegion(region);
    }

    testDb.commit();

    Zone deserialized = serializeAndDeserialize(original);

    assertEquals(regionCount, deserialized.getRegions().size());
  }

  @Test
  void testZoneSerializationPerformance() throws Exception {
    Zone zone = new Zone("perf-zone", 8, 0);

    // Add 50 regions
    for (int i = 0; i < 50; i++) {
      Region region = MapTestFixtures.createTestRegion(i * 5, i * 5, 10, 10);
      zone.addRegion(region);
    }

    testDb.commit();

    // Measure serialization performance
    PerformanceHarness.MeasuredResult<Zone> result =
        PerformanceHarness.measure(() -> serializeAndDeserialize(zone));

    System.out.printf(
        "[PERF] Zone with 50 regions serialization round-trip: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    // Verify correctness
    assertNotNull(result.getResult());
    assertEquals(50, result.getResult().getRegions().size());

    // Lenient performance assertion
    assertTrue(result.getDurationMillis() < 500, "Zone serialization should complete within 500ms");
  }

  @Test
  void testZoneWithVaryingSizesPerformance() throws Exception {
    int[] sizes = {10, 50, 100, 200};

    for (int size : sizes) {
      Zone zone = new Zone("zone-" + size, 100 + size, 0);

      for (int i = 0; i < size; i++) {
        Region region = MapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10);
        zone.addRegion(region);
      }

      testDb.commit();

      long startTime = System.nanoTime();
      Zone deserialized = serializeAndDeserialize(zone);
      long endTime = System.nanoTime();
      long durationMillis = (endTime - startTime) / 1_000_000;

      System.out.printf("[PERF] Zone with %d regions: %d ms%n", size, durationMillis);

      assertEquals(size, deserialized.getRegions().size());
    }
  }

  @Test
  void testRTreeReconstructionFromMapDb() throws Exception {
    Zone original = new Zone("rtree-test", 9, 0);

    // Add regions
    for (int i = 0; i < 20; i++) {
      Region region = MapTestFixtures.createTestRegion(i * 15, i * 15, 10, 10);
      original.addRegion(region);
    }

    testDb.commit();

    // Serialize and deserialize
    Zone deserialized = serializeAndDeserialize(original);

    // Verify all regions can be queried
    Rectangle fullBounds = new Rectangle(0, 0, 1000, 1000);
    Collection<Region> allRegions = deserialized.getRegions(fullBounds);

    assertEquals(20, allRegions.size(), "RTree should reconstruct all 20 regions from MapDb");
  }

  @Test
  void testMultipleZonesSameMapDb() throws Exception {
    // Create two zones using the same MapDb instance
    Zone zone1 = new Zone("zone1", 10, 0);
    Zone zone2 = new Zone("zone1", 10, 1); // Same map, different index

    for (int i = 0; i < 5; i++) {
      zone1.addRegion(MapTestFixtures.createTestRegion(i * 10, 0, 10, 10));
      zone2.addRegion(MapTestFixtures.createTestRegion(0, i * 10, 10, 10));
    }

    testDb.commit();

    Zone deser1 = serializeAndDeserialize(zone1);
    Zone deser2 = serializeAndDeserialize(zone2);

    assertEquals(5, deser1.getRegions().size());
    assertEquals(5, deser2.getRegions().size());

    // Verify they're independent (different spatial distributions)
    Rectangle horizontalQuery = new Rectangle(0, 0, 100, 5);
    assertEquals(5, deser1.getRegions(horizontalQuery).size());
    assertEquals(1, deser2.getRegions(horizontalQuery).size());
  }

  /** Helper method to serialize and deserialize a zone. */
  private Zone serializeAndDeserialize(Zone original) throws IOException, ClassNotFoundException {
    // Serialize
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    original.writeExternal(oos);
    oos.flush();

    // Deserialize
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    Zone deserialized = new Zone();
    deserialized.readExternal(ois);

    return deserialized;
  }
}
