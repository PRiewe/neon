package neon.maps;

import java.awt.Rectangle;
import neon.entities.Creature;
import neon.entities.Item;
import neon.resources.RCreature;
import neon.resources.RItem;
import neon.resources.RTerrain;

/**
 * Test fixture builders for creating map-related test objects.
 *
 * <p>Provides convenient methods for creating Regions, Zones, Worlds, and Dungeons with sensible
 * defaults for testing. This class is in the neon.maps package to access protected methods.
 */
public class MapTestFixtures {

  /**
   * Creates a basic test region with default parameters.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width width
   * @param height height
   * @return a new Region instance
   */
  public static Region createTestRegion(int x, int y, int width, int height) {
    return createTestRegion("test-region", x, y, width, height, 0);
  }

  /**
   * Creates a test region with specified parameters.
   *
   * @param id the region ID
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width width
   * @param height height
   * @param zOrder z-order layer
   * @return a new Region instance
   */
  public static Region createTestRegion(
      String id, int x, int y, int width, int height, int zOrder) {
    RTerrain terrain = new RTerrain("grass");
    return new Region(id, x, y, width, height, null, zOrder, terrain);
  }

  /**
   * Creates a test region with a specific terrain ID.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width width
   * @param height height
   * @param terrainId terrain identifier
   * @return a new Region instance
   */
  public static Region createTestRegionWithTerrain(
      int x, int y, int width, int height, String terrainId) {
    RTerrain terrain = new RTerrain(terrainId);
    return new Region("test-region", x, y, width, height, null, 0, terrain);
  }

  /**
   * Creates a test zone with the given parameters.
   *
   * <p>Note: Requires TestEngineContext to be initialized first, as Zone constructor accesses
   * Engine.getAtlas().getCache().
   *
   * @param name zone name
   * @param mapUID map UID
   * @param index zone index
   * @return a new Zone instance
   */
  public static Zone createTestZone(String name, int mapUID, int index) {
    return new Zone(name, mapUID, index);
  }

  /**
   * Creates a test zone and adds regions to it.
   *
   * @param name zone name
   * @param mapUID map UID
   * @param index zone index
   * @param regions regions to add
   * @return a new Zone instance with regions
   */
  public static Zone createTestZoneWithRegions(
      String name, int mapUID, int index, Region... regions) {
    Zone zone = new Zone(name, mapUID, index);
    for (Region region : regions) {
      zone.addRegion(region);
    }
    return zone;
  }

  /**
   * Creates a large test zone with many regions for performance testing.
   *
   * @param mapUID map UID
   * @param regionCount number of regions to create
   * @return a new Zone with regionCount regions
   */
  public static Zone createLargeZone(int mapUID, int regionCount) {
    Zone zone = new Zone("large-zone", mapUID, 0);

    // Create a grid of regions
    int gridSize = (int) Math.ceil(Math.sqrt(regionCount));
    int regionSize = 10;

    for (int i = 0; i < regionCount; i++) {
      int gridX = i % gridSize;
      int gridY = i / gridSize;
      int x = gridX * regionSize;
      int y = gridY * regionSize;

      Region region = createTestRegion("region-" + i, x, y, regionSize, regionSize, 0);
      zone.addRegion(region);
    }

    return zone;
  }

  /**
   * Creates an empty world map with the given parameters.
   *
   * @param name world name
   * @param uid world UID
   * @return a new World instance
   */
  public static World createEmptyWorld(String name, int uid) {
    return new World(name, uid);
  }

  /**
   * Creates an empty world map with default name.
   *
   * @param uid world UID
   * @return a new World instance
   */
  public static World createEmptyWorld(int uid) {
    return new World("test-world", uid);
  }

  /**
   * Creates a world map with a single region.
   *
   * @param uid world UID
   * @return a new World instance with one region
   */
  public static World createWorldWithSingleRegion(int uid) {
    World world = new World("test-world", uid);
    Region region = createTestRegion(0, 0, 100, 100);
    world.getZone(0).addRegion(region);
    return world;
  }

  /**
   * Creates a world map with multiple regions.
   *
   * @param uid world UID
   * @param regionCount number of regions to add
   * @return a new World instance with regions
   */
  public static World createWorldWithRegions(int uid, int regionCount) {
    World world = new World("test-world", uid);
    Zone zone = world.getZone(0);

    for (int i = 0; i < regionCount; i++) {
      int x = (i % 10) * 10;
      int y = (i / 10) * 10;
      Region region = createTestRegion("region-" + i, x, y, 10, 10, 0);
      zone.addRegion(region);
    }

    return world;
  }

  /**
   * Creates an empty dungeon map.
   *
   * @param name dungeon name
   * @param uid dungeon UID
   * @return a new Dungeon instance
   */
  public static Dungeon createEmptyDungeon(String name, int uid) {
    return new Dungeon(name, uid);
  }

  /**
   * Creates an empty dungeon map with default name.
   *
   * @param uid dungeon UID
   * @return a new Dungeon instance
   */
  public static Dungeon createEmptyDungeon(int uid) {
    return new Dungeon("test-dungeon", uid);
  }

  /**
   * Creates a dungeon with a specified number of zones.
   *
   * @param uid dungeon UID
   * @param zoneCount number of zones to create
   * @return a new Dungeon instance with zones
   */
  public static Dungeon createDungeonWithZones(int uid, int zoneCount) {
    Dungeon dungeon = new Dungeon("test-dungeon", uid);

    for (int i = 0; i < zoneCount; i++) {
      dungeon.addZone(i, "zone-" + i);
      // Add a simple region to the zone
      Zone zone = dungeon.getZone(i);
      Region region = createTestRegion(0, 0, 50, 50);
      zone.addRegion(region);
    }

    return dungeon;
  }

  /**
   * Creates a dungeon with connected zones (linear chain).
   *
   * @param uid dungeon UID
   * @param zoneCount number of zones
   * @return a new Dungeon with zones connected in a chain (0->1->2->...)
   */
  public static Dungeon createConnectedDungeon(int uid, int zoneCount) {
    Dungeon dungeon = createDungeonWithZones(uid, zoneCount);

    // Connect zones in a linear chain
    for (int i = 0; i < zoneCount - 1; i++) {
      dungeon.addConnection(i, i + 1);
    }

    return dungeon;
  }

  /**
   * Creates a bounding rectangle for testing spatial queries.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width width
   * @param height height
   * @return a new Rectangle
   */
  public static Rectangle createBounds(int x, int y, int width, int height) {
    return new Rectangle(x, y, width, height);
  }

  /**
   * Creates a test creature with basic parameters.
   *
   * @param id creature ID
   * @param uid creature UID
   * @param x x-coordinate
   * @param y y-coordinate
   * @return a new Creature instance
   */
  public static Creature createTestCreature(String id, long uid, int x, int y) {
    RCreature species = new RCreature(id);
    Creature creature = new Creature(id, uid, species);
    creature.getShapeComponent().setLocation(x, y);
    return creature;
  }

  /**
   * Creates a test creature with default parameters.
   *
   * @param uid creature UID
   * @return a new Creature instance at position (0, 0)
   */
  public static Creature createTestCreature(long uid) {
    return createTestCreature("test-creature", uid, 0, 0);
  }

  /**
   * Creates a test item with basic parameters.
   *
   * @param id item ID
   * @param uid item UID
   * @param x x-coordinate
   * @param y y-coordinate
   * @return a new Item instance
   */
  public static Item createTestItem(String id, long uid, int x, int y) {
    RItem resource = new RItem(id, RItem.Type.item);
    Item item = new Item(uid, resource);
    item.getShapeComponent().setLocation(x, y);
    return item;
  }

  /**
   * Creates a test item with default parameters.
   *
   * @param uid item UID
   * @return a new Item instance at position (0, 0)
   */
  public static Item createTestItem(long uid) {
    return createTestItem("test-item", uid, 0, 0);
  }
}
