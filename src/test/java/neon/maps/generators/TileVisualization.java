package neon.maps.generators;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;
import neon.maps.MapUtils;

/**
 * Utility class for visualizing map tiles and terrain as ASCII art for debugging and test output.
 *
 * <p>This class centralizes visualization logic previously duplicated across multiple test files.
 * It provides methods for visualizing:
 *
 * <ul>
 *   <li>Integer tile arrays (dungeons, caves, etc.)
 *   <li>String terrain arrays (wilderness, regions)
 *   <li>Boolean grids (mazes)
 *   <li>Rectangle collections (blocks)
 * </ul>
 */
public final class TileVisualization {

  private TileVisualization() {
    // Utility class - prevent instantiation
  }

  // ==================== Tile Visualization ====================

  /**
   * Visualizes tiles as an ASCII grid with default tile-to-character mapping.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'#' = WALL
   *   <li>'.' = FLOOR
   *   <li>'~' = CORRIDOR
   *   <li>'W' = WALL_ROOM
   *   <li>'+' = CORNER
   *   <li>'D' = DOOR (open)
   *   <li>'d' = DOOR_CLOSED
   *   <li>'L' = DOOR_LOCKED
   *   <li>'E' = ENTRY
   *   <li>'?' = Unknown
   * </ul>
   *
   * @param tiles the tile array to visualize
   * @return ASCII art representation with border and summary statistics
   */
  public static String visualizeTiles(int[][] tiles) {
    return visualizeTiles(tiles, TileVisualization::defaultTileChar);
  }

  /**
   * Visualizes tiles as an ASCII grid with custom tile-to-character mapping.
   *
   * @param tiles the tile array to visualize
   * @param mapper custom function to map tile values to characters
   * @return ASCII art representation with border and summary statistics
   */
  public static String visualizeTiles(int[][] tiles, TileCharMapper mapper) {
    if (tiles == null || tiles.length == 0) {
      return "+empty+";
    }

    int width = tiles.length;
    int height = tiles[0].length;

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(mapper.map(tiles[x][y]));
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add tile count summary
    int[] counts = countTiles(tiles);
    sb.append("\n").append(formatCounts(counts));

    return sb.toString();
  }

  /**
   * Default tile-to-character mapping for dungeon/cave tiles.
   *
   * @param tile the tile type constant from MapUtils
   * @return character representation of the tile
   */
  public static char defaultTileChar(int tile) {
    return switch (tile) {
      case MapUtils.WALL -> '#';
      case MapUtils.FLOOR -> '.';
      case MapUtils.WALL_ROOM -> 'W';
      case MapUtils.CORNER -> '+';
      case MapUtils.CORRIDOR -> '~';
      case MapUtils.DOOR -> 'D';
      case MapUtils.DOOR_CLOSED -> 'd';
      case MapUtils.DOOR_LOCKED -> 'L';
      case MapUtils.ENTRY -> 'E';
      default -> '?';
    };
  }

  // ==================== Terrain Visualization ====================

  /**
   * Visualizes String terrain arrays as ASCII art.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>' ' (space) = null/empty terrain
   *   <li>'c' = contains creature annotation (";c:")
   *   <li>'i' = contains item annotation (";i:")
   *   <li>'.' = floor terrain
   * </ul>
   *
   * @param terrain the terrain array to visualize
   * @return ASCII art representation with border and summary statistics
   */
  public static String visualizeTerrain(String[][] terrain) {
    if (terrain == null || terrain.length == 0) {
      return "+empty+";
    }

    int width = terrain.length;
    int height = terrain[0].length;

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        if (terrain[x][y] == null) {
          sb.append(' ');
        } else if (terrain[x][y].contains(";c:")) {
          sb.append('c');
        } else if (terrain[x][y].contains(";i:")) {
          sb.append('i');
        } else {
          sb.append('.');
        }
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add summary statistics
    int floorCount = 0, creatureCount = 0, itemCount = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (terrain[x][y] != null) {
          floorCount++;
          if (terrain[x][y].contains(";c:")) creatureCount++;
          if (terrain[x][y].contains(";i:")) itemCount++;
        }
      }
    }
    sb.append(
        String.format(
            "\nTerrain: %dx%d, floors=%d, creatures=%d, items=%d",
            width, height, floorCount, creatureCount, itemCount));

    return sb.toString();
  }

  // ==================== Grid Visualization (for mazes) ====================

  /**
   * Visualizes an Area as ASCII art (for maze generation).
   *
   * <p>Converts the Area to a boolean grid and visualizes it.
   *
   * @param area the Area to visualize
   * @param width grid width
   * @param height grid height
   * @return ASCII art representation with border
   */
  public static String visualizeArea(Area area, int width, int height) {
    boolean[][] grid = TileConnectivityAssertions.areaToGrid(area, width, height);
    return visualizeGrid(grid);
  }

  /**
   * Visualizes boolean grid as ASCII art (for maze generation).
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'#' = true (filled)
   *   <li>' ' = false (empty)
   * </ul>
   *
   * @param grid the boolean grid to visualize
   * @return ASCII art representation with border
   */
  public static String visualizeGrid(boolean[][] grid) {
    if (grid == null || grid.length == 0) {
      return "+empty+";
    }

    int width = grid.length;
    int height = grid[0].length;

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(grid[x][y] ? '#' : ' ');
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    return sb.toString();
  }

  // ==================== Rectangle Visualization ====================

  /**
   * Visualizes a collection of rectangles overlaid on a grid.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'#' = inside a rectangle
   *   <li>' ' = outside all rectangles
   * </ul>
   *
   * @param rectangles the rectangles to visualize
   * @param width total grid width
   * @param height total grid height
   * @return ASCII art representation with border
   */
  public static String visualizeRectangles(List<Rectangle> rectangles, int width, int height) {
    if (rectangles == null) {
      return "+empty+";
    }

    boolean[][] grid = new boolean[width][height];

    // Mark all rectangle positions
    for (Rectangle rect : rectangles) {
      for (int x = rect.x; x < rect.x + rect.width && x < width; x++) {
        for (int y = rect.y; y < rect.y + rect.height && y < height; y++) {
          if (x >= 0 && y >= 0) {
            grid[x][y] = true;
          }
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(grid[x][y] ? '#' : ' ');
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    sb.append(String.format("\nRectangles: count=%d", rectangles.size()));

    return sb.toString();
  }

  // ==================== Utility Methods ====================

  /**
   * Counts tiles by type in the given tile array.
   *
   * @param tiles the tile array to analyze
   * @return array where index is tile type and value is count (size 16)
   */
  public static int[] countTiles(int[][] tiles) {
    int[] counts = new int[16];
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        int tile = tiles[x][y];
        if (tile >= 0 && tile < counts.length) {
          counts[tile]++;
        }
      }
    }
    return counts;
  }

  /**
   * Formats tile counts as a summary string.
   *
   * @param counts tile count array from countTiles()
   * @return formatted summary string
   */
  public static String formatCounts(int[] counts) {
    return String.format(
        "Tiles: floor=%d, corridor=%d, wall=%d, room_wall=%d, doors=%d",
        counts[MapUtils.FLOOR],
        counts[MapUtils.CORRIDOR],
        counts[MapUtils.WALL],
        counts[MapUtils.WALL_ROOM],
        counts[MapUtils.DOOR] + counts[MapUtils.DOOR_CLOSED] + counts[MapUtils.DOOR_LOCKED]);
  }

  // ==================== Functional Interface ====================

  /**
   * Functional interface for custom tile-to-character mapping.
   *
   * <p>Allows tests to provide custom visualization for their specific tile types.
   */
  @FunctionalInterface
  public interface TileCharMapper {
    /**
     * Maps a tile value to its character representation.
     *
     * @param tile the tile type constant
     * @return character to display for this tile
     */
    char map(int tile);
  }
}
