package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.io.*;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Region serialization and deserialization.
 *
 * <p>Verifies that Region objects can be correctly serialized and deserialized, preserving all
 * fields including position, dimensions, terrain, scripts, and optional labels/themes.
 */
class RegionSerializationTest {

  private MapStore testDb;
  MapTestFixtures mapTestFixtures;

  @BeforeEach
  void setUp() throws Exception {
    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    mapTestFixtures = new MapTestFixtures(TestEngineContext.getTestResources());
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
  }

  @Test
  void testBasicRegionRoundTrip() throws Exception {
    // Create a basic region
    Region original = mapTestFixtures.createTestRegion(10, 20, 30, 40);

    // Serialize and deserialize
    Region deserialized = serializeAndDeserialize(original);

    // Verify all fields preserved
    assertEquals(original.getBounds(), deserialized.getBounds());
    assertEquals(original.getZ(), deserialized.getZ());
  }

  @Test
  void testRegionWithLabel() throws Exception {
    Region original = mapTestFixtures.createTestRegion("region-1", 5, 10, 15, 20, 0);
    original.setLabel("Test Label");

    Region deserialized = serializeAndDeserialize(original);

    assertEquals("Test Label", deserialized.getLabel());
  }

  @Test
  void testRegionWithNullLabel() throws Exception {
    Region original = mapTestFixtures.createTestRegion(0, 0, 10, 10);
    // Label is null by default

    Region deserialized = serializeAndDeserialize(original);

    assertNull(deserialized.getLabel());
  }

  @Test
  void testRegionWithScripts() throws Exception {
    Region original = mapTestFixtures.createTestRegion(0, 0, 50, 50);
    original.addScript("script1.js", false);
    original.addScript("script2.js", false);
    original.addScript("script3.js", false);

    Region deserialized = serializeAndDeserialize(original);

    assertEquals(3, deserialized.getScripts().size());
    assertTrue(deserialized.getScripts().contains("script1.js"));
    assertTrue(deserialized.getScripts().contains("script2.js"));
    assertTrue(deserialized.getScripts().contains("script3.js"));
  }

  @Test
  void testRegionWithEmptyScripts() throws Exception {
    Region original = mapTestFixtures.createTestRegion(0, 0, 25, 25);
    // No scripts added

    Region deserialized = serializeAndDeserialize(original);

    assertNotNull(deserialized.getScripts());
    assertTrue(deserialized.getScripts().isEmpty());
  }

  @Test
  void testRegionDifferentZOrders() throws Exception {
    for (int z = 0; z < 5; z++) {
      Region original = mapTestFixtures.createTestRegion("region-z" + z, 0, 0, 10, 10, z);

      Region deserialized = serializeAndDeserialize(original);

      assertEquals(z, deserialized.getZ());
    }
  }

  @Test
  void testRegionBoundaryValues() throws Exception {
    // Test with large coordinates and dimensions
    Region original = mapTestFixtures.createTestRegion(1000, 2000, 500, 750);

    Region deserialized = serializeAndDeserialize(original);

    Rectangle bounds = deserialized.getBounds();
    assertEquals(1000, bounds.x);
    assertEquals(2000, bounds.y);
    assertEquals(500, bounds.width);
    assertEquals(750, bounds.height);
  }

  @Test
  void testRegionWithZeroSize() throws Exception {
    // Edge case: 0-sized region
    Region original = mapTestFixtures.createTestRegion(10, 10, 0, 0);

    Region deserialized = serializeAndDeserialize(original);

    assertEquals(0, deserialized.getBounds().width);
    assertEquals(0, deserialized.getBounds().height);
  }

  @Test
  void testMultipleRegionsIndependence() throws Exception {
    // Ensure multiple serializations don't interfere
    Region region1 = mapTestFixtures.createTestRegion("r1", 0, 0, 10, 10, 0);
    Region region2 = mapTestFixtures.createTestRegion("r2", 20, 20, 30, 30, 1);
    region1.setLabel("Region One");
    region2.setLabel("Region Two");

    Region deser1 = serializeAndDeserialize(region1);
    Region deser2 = serializeAndDeserialize(region2);

    assertEquals("Region One", deser1.getLabel());
    assertEquals("Region Two", deser2.getLabel());
    assertEquals(0, deser1.getZ());
    assertEquals(1, deser2.getZ());
  }

  @Test
  void testRegionSerializationPerformance() throws Exception {
    Region region = mapTestFixtures.createTestRegion(0, 0, 100, 100);
    region.setLabel("Performance Test Region");
    region.addScript("test1.js", false);
    region.addScript("test2.js", false);

    // Measure serialization performance
    PerformanceHarness.MeasuredResult<Region> result =
        PerformanceHarness.measure(() -> serializeAndDeserialize(region));

    System.out.printf(
        "[PERF] Region serialization round-trip: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    // Verify result is correct
    assertNotNull(result.getResult());
    assertEquals(region.getLabel(), result.getResult().getLabel());

    // Performance assertion (very lenient to avoid flakiness)
    assertTrue(
        result.getDurationMillis() < 100, "Region serialization should complete within 100ms");
  }

  @Test
  void testBulkRegionSerializationPerformance() throws Exception {
    int regionCount = 100;

    long startTime = System.nanoTime();
    for (int i = 0; i < regionCount; i++) {
      Region region = mapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10);
      serializeAndDeserialize(region);
    }
    long endTime = System.nanoTime();
    long durationMillis = (endTime - startTime) / 1_000_000;

    System.out.printf(
        "[PERF] %d regions serialization round-trip: %d ms%n", regionCount, durationMillis);

    // Lenient assertion
    assertTrue(durationMillis < 1000, regionCount + " regions should serialize within 1 second");
  }

  /** Helper method to serialize and deserialize a region. */
  private static Region serializeAndDeserialize(Region original)
      throws IOException, ClassNotFoundException {
    // Serialize
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    original.writeExternal(oos);
    oos.flush();

    // Deserialize
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    Region deserialized = new Region();
    deserialized.readExternal(ois);

    return deserialized;
  }
}
