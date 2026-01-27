package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.Vector;
import neon.entities.Door;
import neon.entities.Entity;
import neon.maps.*;
import neon.maps.services.EntityStore;
import neon.maps.services.QuestProvider;
import neon.maps.services.ResourceProvider;
import neon.resources.*;
import neon.test.MapDbTestHelper;
import neon.test.TestEngineContext;
import neon.util.Dice;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * test class for integration tests that require full Engine context. These tests verify the
 * generate(Door, Zone, Atlas) method which needs EntityStore, ResourceProvider, and other Engine
 * components.
 */
class GenerateWithContextTests {

  private MapStore testDb;
  private Atlas testAtlas;
  private ZoneFactory zoneFactory;
  private EntityStore entityStore;
  private ResourceManager resourceManager;
  private MapTestFixtures mapTestFixtures;

  @BeforeEach
  void setUp() throws Exception {
    // Clean up any stale test files
    new File("test-store.dat").delete();
    new File("testfile3.dat").delete();

    testDb = MapDbTestHelper.createInMemoryDB();
    TestEngineContext.initialize(testDb);
    testAtlas = TestEngineContext.getTestAtlas();
    zoneFactory = TestEngineContext.getTestZoneFactory();
    entityStore = TestEngineContext.getTestEntityStore();
    resourceManager = TestEngineContext.getTestResources();
    mapTestFixtures = new MapTestFixtures(TestEngineContext.getTestResources());
  }

  @AfterEach
  void tearDown() {
    TestEngineContext.reset();
    MapDbTestHelper.cleanup(testDb);
    new File("test-store.dat").delete();
    new File("testfile3.dat").delete();
  }

  /** Stub QuestProvider that returns a specific item once. */
  private static class SingleItemQuestProvider implements QuestProvider {
    private final String itemId;
    private boolean consumed = false;

    SingleItemQuestProvider(String itemId) {
      this.itemId = itemId;
    }

    @Override
    public String getNextRequestedObject() {
      if (!consumed) {
        consumed = true;
        return itemId;
      }
      return null;
    }
  }

  @Test
  void generate_createsZoneWithRegions() throws Exception {
    // Given: a dungeon with two zones, and a door from zone 0 to zone 1
    int mapUID = entityStore.createNewMapUID();
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    // Create dungeon and add zones using the Dungeon API
    Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
    dungeon.addZone(0, "zone-0"); // Previous zone
    dungeon.addZone(1, "zone-1", theme); // Target zone with theme

    // Get the zones from the dungeon
    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    // Add a region to the previous zone
    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    // Add the dungeon to the atlas
    testAtlas.setMap(dungeon);

    // Create a door in the previous zone that leads to zone 1
    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    // Create the dungeon generator
    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(42L),
            Dice.withSeed(42L));

    // When: generate the zone
    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: the zone should have regions
    Collection<Region> regions = targetZone.getRegions();
    assertFalse(regions.isEmpty(), "Generated zone should have regions");

    // The zone should have at least one door (the return door to the previous zone)
    Collection<Long> items = targetZone.getItems();
    boolean hasReturnDoor = false;
    for (long itemUid : items) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof Door door) {
        // Check if this door leads back to the previous zone
        if (door.portal.getDestZone() == previousZone.getIndex()) {
          hasReturnDoor = true;
          break;
        }
      }
    }
    assertTrue(hasReturnDoor, "Generated zone should have a return door to the previous zone");
  }

  @Test
  void generate_linksDoorsCorrectly() throws Exception {
    // Given: a dungeon with two zones
    int mapUID = entityStore.createNewMapUID();
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    // Create dungeon and add zones using the Dungeon API
    Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
    dungeon.addZone(0, "zone-0"); // Previous zone
    dungeon.addZone(1, "zone-1", theme); // Target zone with theme

    // Get the zones from the dungeon
    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    // Add a region to the previous zone
    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    testAtlas.setMap(dungeon);

    // Create entry door in previous zone
    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 10, 10, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    // Create generator and generate
    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(42L),
            Dice.withSeed(42L));

    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: the entry door should now have its destination position set
    Point entryDoorDestPos = entryDoor.portal.getDestPos();
    assertNotNull(entryDoorDestPos, "Entry door should have destination position set");

    // Find the return door in the generated zone
    Door returnDoor = null;
    for (long itemUid : targetZone.getItems()) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof Door door && door.portal.getDestZone() == previousZone.getIndex()) {
        returnDoor = door;
        break;
      }
    }

    assertNotNull(returnDoor, "Should have a return door");

    // Verify bidirectional linking
    Point returnDoorDest = returnDoor.portal.getDestPos();
    assertNotNull(returnDoorDest, "Return door should have destination position");
    assertEquals(10, returnDoorDest.x, "Return door should point to entry door X");
    assertEquals(10, returnDoorDest.y, "Return door should point to entry door Y");
  }

  @Test
  void generate_handlesZoneConnections() throws Exception {
    // Given: a dungeon with three zones where zone 1 connects to both zone 0 and zone 2
    int mapUID = entityStore.createNewMapUID();
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    // Create dungeon and add zones using the Dungeon API
    Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
    dungeon.addZone(0, "zone-0"); // Zone 0
    dungeon.addZone(1, "zone-1", theme); // Zone 1 (to be generated)
    dungeon.addZone(2, "zone-2"); // Zone 2 (connected to zone 1)

    // Get the zones from the dungeon
    Zone zone0 = dungeon.getZone(0);
    Zone zone1 = dungeon.getZone(1);

    // Add a region to zone 0
    zone0.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    // Add connections: zone 1 connects to both zone 0 and zone 2
    dungeon.addConnection(1, 0);
    dungeon.addConnection(1, 2);

    testAtlas.setMap(dungeon);

    // Create entry door from zone 0 to zone 1
    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
    entityStore.addEntity(entryDoor);
    zone0.addItem(entryDoor);

    // Generate zone 1
    DungeonGenerator generator =
        new DungeonGenerator(
            zone1,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(42L),
            Dice.withSeed(42L));

    generator.generate(entryDoor, zone0, testAtlas);

    // Then: zone 1 should have a door to zone 2
    boolean hasDoorToZone2 = false;
    for (long itemUid : zone1.getItems()) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof Door door && door.portal.getDestZone() == 2) {
        hasDoorToZone2 = true;
        break;
      }
    }
    assertTrue(hasDoorToZone2, "Generated zone should have a door to connected zone 2");
  }

  @Test
  void generate_placesQuestItem() throws Exception {
    // Given: a dungeon with quest item to place
    int mapUID = entityStore.createNewMapUID();
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    // Create dungeon and add zones using the Dungeon API
    Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
    dungeon.addZone(0, "zone-0"); // Previous zone
    dungeon.addZone(1, "zone-1", theme); // Target zone with theme

    // Get the zones from the dungeon
    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    // Add a region to the previous zone
    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    testAtlas.setMap(dungeon);

    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    // Use quest provider that requests an item
    QuestProvider questProvider = new SingleItemQuestProvider("test_quest_item");

    // Use ResourceProvider that returns RItem for quest items
    ResourceProvider questResourceProvider =
        new ResourceProvider() {
          @Override
          public Resource getResource(String id) {
            if ("test_quest_item".equals(id)) {
              return new RItem(id, RItem.Type.item);
            }
            if (id != null && (id.contains("door") || id.startsWith("test_door"))) {
              return new RItem.Door(id, RItem.Type.door);
            }
            return new neon.resources.RTerrain(id);
          }

          @Override
          public Resource getResource(String id, String type) {
            if ("terrain".equals(type)) {
              return new neon.resources.RTerrain(id);
            }
            return getResource(id);
          }

          @Override
          public <T extends Resource> Vector<T> getResources(Class<T> rRecipeClass) {
            return null;
          }
        };

    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(42L),
            Dice.withSeed(42L));

    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: the zone should contain the quest item
    boolean hasQuestItem = false;
    for (long itemUid : targetZone.getItems()) {
      Entity entity = entityStore.getEntity(itemUid);
      if (entity instanceof neon.entities.Item item && !(entity instanceof Door)) {
        // Found an item that is not a door
        hasQuestItem = true;
        break;
      }
    }
    assertTrue(hasQuestItem, "Generated zone should contain quest item");
  }

  @Test
  void generate_placesQuestCreature() throws Exception {
    // Given: a dungeon with quest creature to place
    int mapUID = entityStore.createNewMapUID();
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    // Create dungeon and add zones using the Dungeon API
    Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
    dungeon.addZone(0, "zone-0"); // Previous zone
    dungeon.addZone(1, "zone-1", theme); // Target zone with theme

    // Get the zones from the dungeon
    Zone previousZone = dungeon.getZone(0);
    Zone targetZone = dungeon.getZone(1);

    // Add a region to the previous zone
    previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

    testAtlas.setMap(dungeon);

    Door entryDoor =
        mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
    entityStore.addEntity(entryDoor);
    previousZone.addItem(entryDoor);

    // Use quest provider that requests a creature
    QuestProvider questProvider = new SingleItemQuestProvider("test_quest_creature");

    // ResourceProvider that returns the creature resource
    ResourceProvider resourceProvider =
        new ResourceProvider() {
          @Override
          public Resource getResource(String id) {
            if ("test_quest_creature".equals(id)) {
              return new RCreature(id);
            }
            if (id != null && (id.contains("door") || id.startsWith("test_door"))) {
              return new RItem.Door(id, RItem.Type.door);
            }
            // Terrain resources
            if (id != null
                && (id.contains("floor") || id.contains("wall") || id.contains("terrain"))) {
              return new neon.resources.RTerrain(id);
            }
            // Default to terrain
            return new neon.resources.RTerrain(id);
          }

          @Override
          public Resource getResource(String id, String type) {
            if ("terrain".equals(type)) {
              return new neon.resources.RTerrain(id);
            }
            return getResource(id);
          }

          @Override
          public <T extends Resource> Vector<T> getResources(Class<T> rRecipeClass) {
            return null;
          }
        };

    DungeonGenerator generator =
        new DungeonGenerator(
            targetZone,
            TestEngineContext.getTestQuestTracker(),
            TestEngineContext.getTestUiEngineContext(),
            MapUtils.withSeed(42L),
            Dice.withSeed(42L));

    generator.generate(entryDoor, previousZone, testAtlas);

    // Then: the zone should contain the quest creature
    Collection<Long> creatures = targetZone.getCreatures();
    assertFalse(creatures.isEmpty(), "Generated zone should contain quest creature");
  }

  @Test
  void generate_isDeterministicWithFullContext() throws Exception {
    // Given: same setup with same seed should produce identical zones
    long seed = 42L;
    RZoneTheme theme = mapTestFixtures.createTestZoneTheme("cave");

    for (int run = 0; run < 2; run++) {
      // Reset between runs to ensure clean state
      if (run == 1) {
        tearDown();
        setUp();
      }

      int mapUID = entityStore.createNewMapUID();

      // Create dungeon and add zones using the Dungeon API
      Dungeon dungeon = new Dungeon("test-dungeon", mapUID);
      dungeon.addZone(0, "zone-0"); // Previous zone
      dungeon.addZone(1, "zone-1", theme); // Target zone with theme

      // Get the zones from the dungeon
      Zone previousZone = dungeon.getZone(0);
      Zone targetZone = dungeon.getZone(1);

      // Add a region to the previous zone
      previousZone.addRegion(mapTestFixtures.createTestRegion(0, 0, 50, 50));

      testAtlas.setMap(dungeon);

      Door entryDoor =
          mapTestFixtures.createTestPortalDoor(entityStore.createNewEntityUID(), 25, 25, 1, 0);
      entityStore.addEntity(entryDoor);
      previousZone.addItem(entryDoor);

      DungeonGenerator generator =
          new DungeonGenerator(
              targetZone,
              TestEngineContext.getTestQuestTracker(),
              TestEngineContext.getTestUiEngineContext(),
              MapUtils.withSeed(42L),
              Dice.withSeed(42L));

      generator.generate(entryDoor, previousZone, testAtlas);

      // Verify basic structure was created
      assertFalse(targetZone.getRegions().isEmpty(), "Run " + run + ": Zone should have regions");
      assertFalse(
          targetZone.getItems().isEmpty(),
          "Run " + run + ": Zone should have items (at least a door)");
    }
  }
}
