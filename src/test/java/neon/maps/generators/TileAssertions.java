package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import neon.maps.MapUtils;

/**
 * Utility class for common tile array assertions in map generator tests.
 *
 * <p>This class provides methods for comparing tile arrays, checking for the existence of specific
 * tile types, and validating terrain strings. These utilities help reduce code duplication across
 * different generator test classes.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * int[][] tiles1 = generator1.generateBaseTiles(width, height);
 * int[][] tiles2 = generator2.generateBaseTiles(width, height);
 * TileAssertions.assertTilesMatch(tiles1, tiles2);
 * TileAssertions.assertFloorTilesExist(tiles1, "Should have floor tiles");
 * }</pre>
 *
 * @see MapUtils
 */
public final class TileAssertions {

  /** Private constructor to prevent instantiation of utility class. */
  private TileAssertions() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Asserts that two tile arrays are deeply equal.
   *
   * <p>Compares dimensions and contents of two 2D tile arrays, failing with a descriptive message
   * if any differences are found.
   *
   * @param tiles1 first tile array
   * @param tiles2 second tile array
   * @throws AssertionError if arrays differ in dimension or content
   */
  public static void assertTilesMatch(int[][] tiles1, int[][] tiles2) {
    assertEquals(tiles1.length, tiles2.length, "Tile arrays should have same width");
    for (int x = 0; x < tiles1.length; x++) {
      assertEquals(
          tiles1[x].length, tiles2[x].length, "Tile arrays should have same height at x=" + x);
      for (int y = 0; y < tiles1[x].length; y++) {
        assertEquals(
            tiles1[x][y], tiles2[x][y], String.format("Tile at (%d,%d) should match", x, y));
      }
    }
  }

  /**
   * Asserts that two terrain arrays are deeply equal.
   *
   * <p>Compares dimensions and contents of two 2D terrain string arrays. Null values in both arrays
   * at the same position are considered equal.
   *
   * @param terrain1 first terrain array
   * @param terrain2 second terrain array
   * @throws AssertionError if arrays differ in dimension or content
   */
  public static void assertTerrainMatch(String[][] terrain1, String[][] terrain2) {
    assertEquals(terrain1.length, terrain2.length, "Terrain arrays should have same width");
    for (int x = 0; x < terrain1.length; x++) {
      assertEquals(
          terrain1[x].length,
          terrain2[x].length,
          "Terrain arrays should have same height at x=" + x);
      for (int y = 0; y < terrain1[x].length; y++) {
        if (terrain1[x][y] == null && terrain2[x][y] == null) {
          continue; // Both null is fine
        }
        assertEquals(
            terrain1[x][y], terrain2[x][y], String.format("Terrain at (%d,%d) should match", x, y));
      }
    }
  }

  /**
   * Asserts that at least one floor tile exists in the tile array.
   *
   * <p>Searches for any tile with type {@link MapUtils#FLOOR}.
   *
   * @param tiles tile array to check
   * @param message assertion failure message
   * @throws AssertionError if no floor tiles are found
   */
  public static void assertFloorTilesExist(int[][] tiles, String message) {
    boolean hasFloor = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (tiles[x][y] == MapUtils.FLOOR) {
          hasFloor = true;
          break;
        }
      }
      if (hasFloor) break;
    }
    assertTrue(hasFloor, message);
  }

  /**
   * Asserts that at least one tile of the specified type exists in the tile array.
   *
   * <p>Generic method for checking existence of any tile type.
   *
   * @param tiles tile array to check
   * @param tileType tile type constant from {@link MapUtils}
   * @param message assertion failure message
   * @throws AssertionError if no tiles of the specified type are found
   */
  public static void assertTileTypeExists(int[][] tiles, int tileType, String message) {
    boolean found = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (tiles[x][y] == tileType) {
          found = true;
          break;
        }
      }
      if (found) break;
    }
    assertTrue(found, message);
  }

  /**
   * Asserts that at least one walkable tile exists in the tile array.
   *
   * <p>Searches for any walkable tile (floor, corridor, or door) using {@link
   * TileConnectivityAssertions#isWalkable(int)}.
   *
   * @param tiles tile array to check
   * @param message assertion failure message
   * @throws AssertionError if no walkable tiles are found
   */
  public static void assertWalkableTilesExist(int[][] tiles, String message) {
    boolean hasWalkable = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (TileConnectivityAssertions.isWalkable(tiles[x][y])) {
          hasWalkable = true;
          break;
        }
      }
      if (hasWalkable) break;
    }
    assertTrue(hasWalkable, message);
  }

  /**
   * Asserts that at least one floor terrain of the specified types exists in the terrain array.
   *
   * <p>The floors parameter is a comma-separated list of terrain type names (e.g.,
   * "grass,stone,dirt"). Each terrain cell may contain additional data after a semicolon which is
   * ignored for comparison purposes.
   *
   * @param terrain terrain string array to check
   * @param floors comma-separated list of acceptable floor terrain types
   * @param message assertion failure message
   * @throws AssertionError if no matching floor terrain is found
   */
  public static void assertFloorTerrainExists(String[][] terrain, String floors, String message) {
    List<String> floorTypes = List.of(floors.split(","));
    boolean hasFloor = false;
    for (int x = 0; x < terrain.length && !hasFloor; x++) {
      for (int y = 0; y < terrain[0].length && !hasFloor; y++) {
        if (terrain[x][y] != null) {
          String baseTerrain = terrain[x][y].split(";")[0];
          if (floorTypes.contains(baseTerrain)) {
            hasFloor = true;
          }
        }
      }
    }
    assertTrue(hasFloor, message);
  }

  /**
   * Asserts that at least one room wall tile exists in the tile array.
   *
   * <p>Searches for tiles with type {@link MapUtils#WALL_ROOM}.
   *
   * @param tiles tile array to check
   * @param message assertion failure message
   * @throws AssertionError if no room wall tiles are found
   */
  public static void assertRoomWallsExist(int[][] tiles, String message) {
    assertTileTypeExists(tiles, MapUtils.WALL_ROOM, message);
  }

  /**
   * Asserts that at least one corner tile exists in the tile array.
   *
   * <p>Searches for tiles with type {@link MapUtils#CORNER}.
   *
   * @param tiles tile array to check
   * @param message assertion failure message
   * @throws AssertionError if no corner tiles are found
   */
  public static void assertCornersExist(int[][] tiles, String message) {
    assertTileTypeExists(tiles, MapUtils.CORNER, message);
  }
}
