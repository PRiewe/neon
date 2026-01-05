package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import neon.util.Dice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for DungeonGenerator tile generation with deterministic seeded random behavior.
 *
 * <p>Since DungeonGenerator has many dependencies (Zone, EntityStore, etc.), these tests focus on
 * the tile generation logic by using a test subclass that exposes the generateBaseTiles method.
 */
class DungeonGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for dungeon tile generation.
   *
   * @param seed random seed for deterministic behavior
   * @param type dungeon type (cave, pits, maze, mine, bsp, packed, or default)
   * @param width dungeon width
   * @param height dungeon height
   */
  record DungeonTypeScenario(long seed, String type, int width, int height) {

    @Override
    public String toString() {
      return String.format("seed=%d, type=%s, %dx%d", seed, type, width, height);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<DungeonTypeScenario> dungeonTypeScenarios() {
    return Stream.of(
        // Cave types
        new DungeonTypeScenario(42L, "cave", 30, 30),
        new DungeonTypeScenario(999L, "cave", 40, 35),
        // Pits types
        new DungeonTypeScenario(42L, "pits", 30, 30),
        new DungeonTypeScenario(123L, "pits", 35, 35),
        // Maze types
        new DungeonTypeScenario(42L, "maze", 31, 31),
        new DungeonTypeScenario(264L, "maze", 41, 31),
        // Mine types
        new DungeonTypeScenario(42L, "mine", 35, 35),
        new DungeonTypeScenario(999L, "mine", 45, 40),
        // BSP types (need larger dimensions for room placement)
        new DungeonTypeScenario(42L, "bsp", 50, 40),
        new DungeonTypeScenario(123L, "bsp", 60, 50),
        // Packed types
        new DungeonTypeScenario(42L, "packed", 40, 30),
        new DungeonTypeScenario(264L, "packed", 50, 40),
        // Default (sparse) types
        new DungeonTypeScenario(42L, "default", 50, 40),
        new DungeonTypeScenario(999L, "sparse", 55, 45));
  }

  // ==================== Dungeon Type Tests ====================

  @ParameterizedTest(name = "generateBaseTiles: {0}")
  @MethodSource("dungeonTypeScenarios")
  void generateBaseTiles_generatesValidTiles(DungeonTypeScenario scenario) {
    // Given
    TestableDungeonGenerator generator =
        new TestableDungeonGenerator(
            MapUtils.withSeed(scenario.seed()), Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.testGenerateBaseTiles(scenario.type(), scenario.width(), scenario.height());

    // Then: visualize
    System.out.println("Dungeon (" + scenario.type() + "): " + scenario);
    System.out.println(visualize(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Dungeon width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Dungeon height should match"),
        () -> assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () -> assertDungeonIsConnected(tiles, "Dungeon should be connected"));
  }

  @ParameterizedTest(name = "generateBaseTiles determinism: {0}")
  @MethodSource("dungeonTypeScenarios")
  void generateBaseTiles_isDeterministic(DungeonTypeScenario scenario) {
    // Given: two generators with the same seed
    TestableDungeonGenerator generator1 =
        new TestableDungeonGenerator(
            MapUtils.withSeed(scenario.seed()), Dice.withSeed(scenario.seed()));
    TestableDungeonGenerator generator2 =
        new TestableDungeonGenerator(
            MapUtils.withSeed(scenario.seed()), Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.testGenerateBaseTiles(scenario.type(), scenario.width(), scenario.height());
    int[][] tiles2 =
        generator2.testGenerateBaseTiles(scenario.type(), scenario.width(), scenario.height());

    // Then
    assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Test Helper Class ====================

  /**
   * A test subclass that exposes the tile generation logic without requiring all the complex
   * dependencies of the full DungeonGenerator.
   */
  static class TestableDungeonGenerator {
    private final MapUtils mapUtils;
    private final Dice dice;
    private final BlocksGenerator blocksGenerator;
    private final ComplexGenerator complexGenerator;
    private final CaveGenerator caveGenerator;
    private final MazeGenerator mazeGenerator;

    TestableDungeonGenerator(MapUtils mapUtils, Dice dice) {
      this.mapUtils = mapUtils;
      this.dice = dice;
      this.blocksGenerator = new BlocksGenerator(mapUtils);
      this.complexGenerator = new ComplexGenerator(mapUtils);
      this.caveGenerator = new CaveGenerator(dice);
      this.mazeGenerator = new MazeGenerator(dice);
    }

    /**
     * Generates base tiles for a dungeon of the given type. This mirrors the logic in
     * DungeonGenerator.generateBaseTiles().
     */
    int[][] testGenerateBaseTiles(String type, int width, int height) {
      int[][] tiles = new int[width][height];
      switch (type) {
        case "cave":
          tiles = makeTiles(mazeGenerator.generateSquashedMaze(width, height, 3), width, height);
          break;
        case "pits":
          tiles = caveGenerator.generateOpenCave(width, height, 3);
          break;
        case "maze":
          tiles = makeTiles(mazeGenerator.generateMaze(width, height, 3, 50), width, height);
          break;
        case "mine":
          java.awt.geom.Area mine = mazeGenerator.generateSquashedMaze(width, height, 12);
          mine.add(mazeGenerator.generateMaze(width, height, 12, 40));
          tiles = makeTiles(mine, width, height);
          break;
        case "bsp":
          tiles = complexGenerator.generateBSPDungeon(width, height, 5, 12);
          break;
        case "packed":
          tiles = complexGenerator.generatePackedDungeon(width, height, 10, 4, 7);
          break;
        default:
          tiles = complexGenerator.generateSparseDungeon(width, height, 5, 5, 15);
          break;
      }
      return tiles;
    }

    private int[][] makeTiles(java.awt.geom.Area area, int width, int height) {
      int[][] tiles = new int[width][height];
      for (int x = 0; x < tiles.length; x++) {
        for (int y = 0; y < tiles[x].length; y++) {
          if (area.contains(x, y)) {
            tiles[x][y] = MapUtils.FLOOR;
          } else {
            tiles[x][y] = MapUtils.WALL;
          }
        }
      }
      return tiles;
    }
  }

  // ==================== Assertion Helpers ====================

  private void assertFloorTilesExist(int[][] tiles, String message) {
    boolean hasFloor = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (isWalkable(tiles[x][y])) {
          hasFloor = true;
          break;
        }
      }
      if (hasFloor) break;
    }
    assertTrue(hasFloor, message);
  }

  private void assertDungeonIsConnected(int[][] tiles, String message) {
    // Count walkable tiles and verify flood fill reaches all of them
    int floorCount = 0;
    int startX = -1, startY = -1;

    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (isWalkable(tiles[x][y])) {
          floorCount++;
          if (startX < 0) {
            startX = x;
            startY = y;
          }
        }
      }
    }

    if (floorCount == 0) {
      fail(message + " - no walkable tiles found");
      return;
    }

    // Flood fill from start position using BFS
    int reachable = floodFillCount(tiles, startX, startY);
    assertEquals(floorCount, reachable, message + " - not all walkable tiles are connected");
  }

  private boolean isWalkable(int tile) {
    return tile == MapUtils.FLOOR
        || tile == MapUtils.CORRIDOR
        || tile == MapUtils.DOOR
        || tile == MapUtils.DOOR_CLOSED
        || tile == MapUtils.DOOR_LOCKED;
  }

  private int floodFillCount(int[][] tiles, int startX, int startY) {
    int width = tiles.length;
    int height = tiles[0].length;
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

  private void assertTilesMatch(int[][] tiles1, int[][] tiles2) {
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

  // ==================== Visualization ====================

  /**
   * Visualizes tiles as an ASCII grid.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'#' = WALL
   *   <li>'.' = FLOOR
   *   <li>'~' = CORRIDOR
   *   <li>'W' = WALL_ROOM
   *   <li>'+' = CORNER
   *   <li>'D' = DOOR
   * </ul>
   */
  private String visualize(int[][] tiles) {
    int width = tiles.length;
    int height = tiles[0].length;

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(tileChar(tiles[x][y]));
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add tile count summary
    int[] counts = countTiles(tiles);
    sb.append("\nTiles: ");
    sb.append(
        String.format(
            "floor=%d, corridor=%d, wall=%d, room_wall=%d, doors=%d",
            counts[MapUtils.FLOOR],
            counts[MapUtils.CORRIDOR],
            counts[MapUtils.WALL],
            counts[MapUtils.WALL_ROOM],
            counts[MapUtils.DOOR] + counts[MapUtils.DOOR_CLOSED] + counts[MapUtils.DOOR_LOCKED]));

    return sb.toString();
  }

  private char tileChar(int tile) {
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

  private int[] countTiles(int[][] tiles) {
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
}
