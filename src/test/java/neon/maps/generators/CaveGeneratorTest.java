package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import neon.util.Dice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for CaveGenerator with deterministic seeded random behavior. */
class CaveGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for open cave generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width cave width
   * @param height cave height
   * @param sparseness how sparse the cave should be (higher = more open)
   */
  record CaveScenario(long seed, int width, int height, int sparseness) {

    @Override
    public String toString() {
      return String.format("seed=%d, %dx%d, sparse=%d", seed, width, height, sparseness);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<CaveScenario> caveScenarios() {
    return Stream.of(
        new CaveScenario(42L, 30, 30, 3),
        new CaveScenario(999L, 40, 30, 2),
        new CaveScenario(123L, 35, 35, 4),
        new CaveScenario(264L, 50, 40, 3));
  }

  // ==================== Open Cave Tests ====================

  @ParameterizedTest(name = "generateOpenCave: {0}")
  @MethodSource("caveScenarios")
  void generateOpenCave_generatesValidCave(CaveScenario scenario) {
    // Given
    CaveGenerator generator = new CaveGenerator(Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());

    // Then: visualize
    System.out.println("Open Cave: " + scenario);
    System.out.println(visualize(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Cave width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Cave height should match"),
        () -> assertFloorTilesExist(tiles, "Cave should have floor tiles"),
        () -> assertCaveIsConnected(tiles, "Cave should be connected"));
  }

  @ParameterizedTest(name = "generateOpenCave determinism: {0}")
  @MethodSource("caveScenarios")
  void generateOpenCave_isDeterministic(CaveScenario scenario) {
    // Given: two generators with the same seed
    CaveGenerator generator1 = new CaveGenerator(Dice.withSeed(scenario.seed()));
    CaveGenerator generator2 = new CaveGenerator(Dice.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());
    int[][] tiles2 =
        generator2.generateOpenCave(scenario.width(), scenario.height(), scenario.sparseness());

    // Then
    assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Assertion Helpers ====================

  private void assertFloorTilesExist(int[][] tiles, String message) {
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

  private void assertCaveIsConnected(int[][] tiles, String message) {
    // Count floor tiles and verify flood fill reaches all of them
    int floorCount = 0;
    int startX = -1, startY = -1;

    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (tiles[x][y] == MapUtils.FLOOR) {
          floorCount++;
          if (startX < 0) {
            startX = x;
            startY = y;
          }
        }
      }
    }

    if (floorCount == 0) {
      fail(message + " - no floor tiles found");
      return;
    }

    // Flood fill from start position using BFS (iterative to avoid stack overflow)
    int reachable = floodFillCount(tiles, startX, startY);
    assertEquals(floorCount, reachable, message + " - not all floor tiles are connected");
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
            && tiles[nx][ny] == MapUtils.FLOOR) {
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
    int floorCount = 0;
    int wallCount = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (tiles[x][y] == MapUtils.FLOOR) floorCount++;
        else if (tiles[x][y] == MapUtils.WALL) wallCount++;
      }
    }
    sb.append("\nTiles: floor=").append(floorCount).append(", wall=").append(wallCount);

    return sb.toString();
  }

  private char tileChar(int tile) {
    return switch (tile) {
      case MapUtils.WALL -> '#';
      case MapUtils.FLOOR -> '.';
      default -> '?';
    };
  }
}
