package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.Area;
import java.util.LinkedList;
import java.util.Queue;
import neon.maps.MapUtils;

/**
 * Utility class for asserting connectivity in tile-based maps.
 *
 * <p>This class provides methods to verify that all walkable tiles in a dungeon or map are
 * connected and reachable from any starting walkable tile. It uses breadth-first search (BFS) flood
 * fill algorithm to count reachable tiles.
 *
 * <p>Primary use case is testing dungeon generators to ensure they don't create isolated areas.
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * int[][] tiles = generator.generate(width, height);
 * TileConnectivityAssertions.assertFullyConnected(tiles, "Dungeon should be fully connected");
 * }</pre>
 *
 * @see MapUtils
 */
public final class TileConnectivityAssertions {

  /** Private constructor to prevent instantiation of utility class. */
  private TileConnectivityAssertions() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Asserts that all walkable tiles in the given tile array are connected.
   *
   * <p>This method counts all walkable tiles, finds the first walkable tile as a starting point,
   * then performs a BFS flood fill to count all reachable tiles. If the counts don't match, the
   * assertion fails indicating the map has disconnected areas.
   *
   * @param tiles 2D array of tile types (indexed as tiles[x][y])
   * @param message descriptive message to include in assertion failure
   * @throws AssertionError if not all walkable tiles are connected
   * @throws NullPointerException if tiles is null or contains null rows
   */
  public static void assertFullyConnected(int[][] tiles, String message) {
    if (tiles == null || tiles.length == 0) {
      fail(message + " - tiles array is null or empty");
      return;
    }

    // Count walkable tiles and find first walkable tile as starting point
    int floorCount = 0;
    int[] startPos = findFirstWalkableTile(tiles);

    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (isWalkable(tiles[x][y])) {
          floorCount++;
        }
      }
    }

    if (floorCount == 0) {
      fail(message + " - no walkable tiles found");
      return;
    }

    if (startPos == null) {
      fail(message + " - could not find starting walkable tile");
      return;
    }

    // Flood fill from start position using BFS
    int reachable = countReachableTiles(tiles, startPos[0], startPos[1]);
    assertEquals(floorCount, reachable, message + " - not all walkable tiles are connected");
  }

  /**
   * Counts the number of walkable tiles reachable from a starting position using BFS.
   *
   * <p>This method performs a breadth-first search starting from the given coordinates, counting
   * all walkable tiles that can be reached by moving horizontally or vertically (not diagonally).
   *
   * @param tiles 2D array of tile types (indexed as tiles[x][y])
   * @param startX x-coordinate of starting position
   * @param startY y-coordinate of starting position
   * @return number of walkable tiles reachable from the starting position
   * @throws NullPointerException if tiles is null or contains null rows
   * @throws ArrayIndexOutOfBoundsException if start coordinates are out of bounds
   */
  public static int countReachableTiles(int[][] tiles, int startX, int startY) {
    int width = tiles.length;
    int height = tiles[0].length;
    boolean[][] visited = new boolean[width][height];
    Queue<int[]> queue = new LinkedList<>();
    queue.add(new int[] {startX, startY});
    visited[startX][startY] = true;
    int count = 0;

    // Four cardinal directions: left, right, up, down
    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    while (!queue.isEmpty()) {
      int[] current = queue.poll();
      count++;

      for (int[] dir : directions) {
        int nx = current[0] + dir[0];
        int ny = current[1] + dir[1];

        if (nx >= 0
            && nx < width
            && ny >= 0
            && ny < height
            && !visited[nx][ny]
            && isWalkable(tiles[nx][ny])) {
          visited[nx][ny] = true;
          queue.add(new int[] {nx, ny});
        }
      }
    }

    return count;
  }

  /**
   * Finds the coordinates of the first walkable tile in the array.
   *
   * <p>Scans the tile array from top-left (0,0) to bottom-right, returning the coordinates of the
   * first walkable tile found.
   *
   * @param tiles 2D array of tile types (indexed as tiles[x][y])
   * @return array of [x, y] coordinates of first walkable tile, or null if none found
   * @throws NullPointerException if tiles is null or contains null rows
   */
  public static int[] findFirstWalkableTile(int[][] tiles) {
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (isWalkable(tiles[x][y])) {
          return new int[] {x, y};
        }
      }
    }
    return null;
  }

  /**
   * Checks if a tile type is walkable (can be traversed by entities).
   *
   * <p>Walkable tiles include: floors, corridors, doors (open, closed, and locked).
   *
   * @param tile tile type constant from {@link MapUtils}
   * @return true if the tile is walkable, false otherwise
   * @see MapUtils#FLOOR
   * @see MapUtils#CORRIDOR
   * @see MapUtils#DOOR
   * @see MapUtils#DOOR_CLOSED
   * @see MapUtils#DOOR_LOCKED
   */
  public static boolean isWalkable(int tile) {
    return tile == MapUtils.FLOOR
        || tile == MapUtils.CORRIDOR
        || tile == MapUtils.DOOR
        || tile == MapUtils.DOOR_CLOSED
        || tile == MapUtils.DOOR_LOCKED;
  }

  /**
   * Asserts that all cells in an Area are connected.
   *
   * <p>This method is used for testing maze generators that produce Area objects. It converts the
   * Area to a boolean grid and verifies all cells are reachable via flood fill.
   *
   * @param area the Area to test for connectivity
   * @param width width of the grid to test
   * @param height height of the grid to test
   * @param message descriptive message to include in assertion failure
   * @throws AssertionError if not all cells in the area are connected
   */
  public static void assertAreaFullyConnected(Area area, int width, int height, String message) {
    boolean[][] grid = areaToGrid(area, width, height);

    // Count total cells in the area
    int totalCells = 0;
    int startX = -1, startY = -1;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (grid[x][y]) {
          totalCells++;
          if (startX < 0) {
            startX = x;
            startY = y;
          }
        }
      }
    }

    if (totalCells == 0) {
      fail(message + " - no cells found in area");
      return;
    }

    // Flood fill from start position to count reachable cells
    int reachable = countReachableCells(grid, startX, startY, width, height);
    assertEquals(totalCells, reachable, message + " - not all cells are connected");
  }

  /**
   * Converts an Area to a boolean grid.
   *
   * <p>Each cell in the grid is true if the center point of that cell is contained within the Area.
   *
   * @param area the Area to convert
   * @param width width of the grid
   * @param height height of the grid
   * @return boolean grid where true indicates the cell is in the area
   */
  public static boolean[][] areaToGrid(Area area, int width, int height) {
    boolean[][] grid = new boolean[width][height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        grid[x][y] = area.contains(x + 0.5, y + 0.5);
      }
    }
    return grid;
  }

  /**
   * Counts the number of cells reachable from a starting position in a boolean grid using BFS.
   *
   * @param grid boolean grid where true indicates walkable cells
   * @param startX x-coordinate of starting position
   * @param startY y-coordinate of starting position
   * @param width width of the grid
   * @param height height of the grid
   * @return number of cells reachable from the starting position
   */
  private static int countReachableCells(
      boolean[][] grid, int startX, int startY, int width, int height) {
    boolean[][] visited = new boolean[width][height];
    Queue<int[]> queue = new LinkedList<>();
    queue.add(new int[] {startX, startY});
    visited[startX][startY] = true;
    int count = 0;

    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    while (!queue.isEmpty()) {
      int[] current = queue.poll();
      count++;

      for (int[] dir : directions) {
        int nx = current[0] + dir[0];
        int ny = current[1] + dir[1];

        if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[nx][ny] && grid[nx][ny]) {
          visited[nx][ny] = true;
          queue.add(new int[] {nx, ny});
        }
      }
    }

    return count;
  }
}
