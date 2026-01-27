package neon.maps;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Collection;
import neon.test.MapDbTestHelper;
import neon.test.PerformanceHarness;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Map implementations (World and Dungeon) serialization and deserialization.
 *
 * <p>Verifies that World and Dungeon objects can be correctly serialized and deserialized,
 * preserving their structure including zones, zone connections (for dungeons), and all nested data.
 */
class MapSerializationTest {

  private MapStore testDb;
  private MapTestFixtures mapTestFixtures;

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

  // ==================== World Tests ====================

  @Test
  void testEmptyWorldRoundTrip() throws Exception {
    World original = new World("Test World", 100);

    World deserialized = serializeAndDeserializeWorld(original);

    assertEquals("Test World", deserialized.getName());
    assertEquals(100, deserialized.getUID());
    assertNotNull(deserialized.getZone(0));
  }

  @Test
  void testWorldWithRegions() throws Exception {
    World original = new World("World With Regions", 101);

    // Add regions to the world's zone
    Zone zone = original.getZone(0);
    for (int i = 0; i < 10; i++) {
      Region region = mapTestFixtures.createTestRegion(i * 20, i * 20, 20, 20);
      zone.addRegion(region);
    }

    testDb.commit();

    World deserialized = serializeAndDeserializeWorld(original);

    assertEquals("World With Regions", deserialized.getName());
    assertEquals(10, deserialized.getZone(0).getRegions().size());
  }

  @Test
  void testWorldUIDPreservation() throws Exception {
    int[] uids = {1, 100, 999, 12345};

    for (int uid : uids) {
      World original = new World("World-" + uid, uid);
      World deserialized = serializeAndDeserializeWorld(original);

      assertEquals(uid, deserialized.getUID());
    }
  }

  @Test
  void testWorldZoneDataIntegrity() throws Exception {
    World original = new World("Integrity Test", 102);
    Zone zone = original.getZone(0);

    // Add various regions
    zone.addRegion(mapTestFixtures.createTestRegion("r1", 0, 0, 50, 50, 0));
    zone.addRegion(mapTestFixtures.createTestRegion("r2", 60, 60, 30, 30, 1));
    zone.addRegion(mapTestFixtures.createTestRegion("r3", 100, 100, 25, 25, 2));

    testDb.commit();

    World deserialized = serializeAndDeserializeWorld(original);

    Zone deserZone = deserialized.getZone(0);
    assertEquals(3, deserZone.getRegions().size());
  }

  // ==================== Dungeon Tests ====================

  @Test
  void testEmptyDungeonRoundTrip() throws Exception {
    Dungeon original = new Dungeon("Test Dungeon", 200);

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    assertEquals("Test Dungeon", deserialized.getName());
    assertEquals(200, deserialized.getUID());
  }

  @Test
  void testDungeonWithSingleZone() throws Exception {
    Dungeon original = new Dungeon("Single Zone Dungeon", 201);
    original.addZone(0, "Level 1");

    // Add a region to the zone
    Zone zone = original.getZone(0);
    zone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    testDb.commit();

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    assertEquals("Single Zone Dungeon", deserialized.getName());
    assertNotNull(deserialized.getZone(0));
    assertEquals("Level 1", deserialized.getZoneName(0));
  }

  @Test
  void testDungeonWithMultipleZones() throws Exception {
    Dungeon original = new Dungeon("Multi-Level Dungeon", 202);

    // Add 5 zones
    for (int i = 0; i < 5; i++) {
      original.addZone(i, "Level " + (i + 1));
      Zone zone = original.getZone(i);
      zone.addRegion(mapTestFixtures.createTestRegion(0, 0, 40, 40));
    }

    testDb.commit();

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    Collection<Zone> zones = deserialized.getZones();
    assertEquals(5, zones.size());

    for (int i = 0; i < 5; i++) {
      assertNotNull(deserialized.getZone(i));
      assertEquals("Level " + (i + 1), deserialized.getZoneName(i));
    }
  }

  @Test
  void testDungeonZoneConnections() throws Exception {
    Dungeon original = new Dungeon("Connected Dungeon", 203);

    // Create 3 zones connected in a chain
    for (int i = 0; i < 3; i++) {
      original.addZone(i, "Zone " + i);
      Zone zone = original.getZone(i);
      zone.addRegion(mapTestFixtures.createTestRegion(0, 0, 30, 30));
    }

    // Connect: 0 -> 1 -> 2
    original.addConnection(0, 1);
    original.addConnection(1, 2);

    testDb.commit();

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    // Verify connections
    Collection<Integer> connectionsFrom0 = deserialized.getConnections(0);
    assertTrue(connectionsFrom0.contains(1), "Zone 0 should connect to Zone 1");

    Collection<Integer> connectionsFrom1 = deserialized.getConnections(1);
    assertTrue(connectionsFrom1.contains(2), "Zone 1 should connect to Zone 2");
  }

  @Test
  void testDungeonBidirectionalConnections() throws Exception {
    Dungeon original = new Dungeon("Bidirectional Dungeon", 204);

    original.addZone(0, "Hub");
    original.addZone(1, "North");
    original.addZone(2, "South");

    // addConnection is already bidirectional, so one call creates both directions
    original.addConnection(0, 1);
    original.addConnection(0, 2);

    testDb.commit();

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    Collection<Integer> hubConnections = deserialized.getConnections(0);
    assertEquals(2, hubConnections.size());
    assertTrue(hubConnections.contains(1));
    assertTrue(hubConnections.contains(2));

    Collection<Integer> northConnections = deserialized.getConnections(1);
    assertTrue(northConnections.contains(0));
  }

  @Test
  void testDungeonWithComplexZones() throws Exception {
    Dungeon original = new Dungeon("Complex Dungeon", 205);

    // Create 3 zones with different numbers of regions
    original.addZone(0, "Small");
    Zone small = original.getZone(0);
    for (int i = 0; i < 3; i++) {
      small.addRegion(mapTestFixtures.createTestRegion(i * 10, 0, 10, 10));
    }

    original.addZone(1, "Medium");
    Zone medium = original.getZone(1);
    for (int i = 0; i < 10; i++) {
      medium.addRegion(mapTestFixtures.createTestRegion(i * 10, 0, 10, 10));
    }

    original.addZone(2, "Large");
    Zone large = original.getZone(2);
    for (int i = 0; i < 25; i++) {
      large.addRegion(mapTestFixtures.createTestRegion((i % 5) * 10, (i / 5) * 10, 10, 10));
    }

    testDb.commit();

    Dungeon deserialized = serializeAndDeserializeDungeon(original);

    assertEquals(3, deserialized.getZone(0).getRegions().size());
    assertEquals(10, deserialized.getZone(1).getRegions().size());
    assertEquals(25, deserialized.getZone(2).getRegions().size());
  }

  @Test
  void testDungeonUIDPreservation() throws Exception {
    int[] uids = {200, 500, 999, 54321};

    for (int uid : uids) {
      Dungeon original = new Dungeon("Dungeon-" + uid, uid);
      original.addZone(0, "Level 1");

      Dungeon deserialized = serializeAndDeserializeDungeon(original);

      assertEquals(uid, deserialized.getUID());
    }
  }

  // ==================== Performance Tests ====================

  @Test
  void testWorldSerializationPerformance() throws Exception {
    World world = new World("Performance World", 300);
    Zone zone = world.getZone(0);

    // Add 100 regions
    for (int i = 0; i < 100; i++) {
      zone.addRegion(mapTestFixtures.createTestRegion(i * 10, i * 10, 10, 10));
    }

    testDb.commit();

    PerformanceHarness.MeasuredResult<World> result =
        PerformanceHarness.measure(() -> serializeAndDeserializeWorld(world));

    System.out.printf(
        "[PERF] World with 100 regions serialization: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    assertEquals(100, result.getResult().getZone(0).getRegions().size());

    assertTrue(
        result.getDurationMillis() < 500, "World serialization should complete within 500ms");
  }

  @Test
  void testDungeonSerializationPerformance() throws Exception {
    Dungeon dungeon = new Dungeon("Performance Dungeon", 400);

    // Create 10 zones with 20 regions each
    for (int z = 0; z < 10; z++) {
      dungeon.addZone(z, "Zone " + z);
      Zone zone = dungeon.getZone(z);
      for (int r = 0; r < 20; r++) {
        zone.addRegion(mapTestFixtures.createTestRegion(r * 10, r * 10, 10, 10));
      }
      // Connect zones in a chain
      if (z > 0) {
        dungeon.addConnection(z - 1, z);
      }
    }

    testDb.commit();

    PerformanceHarness.MeasuredResult<Dungeon> result =
        PerformanceHarness.measure(() -> serializeAndDeserializeDungeon(dungeon));

    System.out.printf(
        "[PERF] Dungeon with 10 zones (200 total regions) serialization: %d ms (%d ns)%n",
        result.getDurationMillis(), result.getDurationNanos());

    assertEquals(10, result.getResult().getZones().size());

    assertTrue(
        result.getDurationMillis() < 1000, "Dungeon serialization should complete within 1 second");
  }

  // ==================== Helper Methods ====================

  /** Helper method to serialize and deserialize a World. */
  private World serializeAndDeserializeWorld(World original)
      throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    original.writeExternal(oos);
    oos.flush();

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    World deserialized = new World();
    deserialized.readExternal(ois);

    return deserialized;
  }

  /** Helper method to serialize and deserialize a Dungeon. */
  private Dungeon serializeAndDeserializeDungeon(Dungeon original)
      throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    original.writeExternal(oos);
    oos.flush();

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    // Dungeon doesn't have no-arg constructor, create with dummy values that will be overwritten
    Dungeon deserialized = new Dungeon("temp", 0);
    deserialized.readExternal(ois);

    return deserialized;
  }
}
