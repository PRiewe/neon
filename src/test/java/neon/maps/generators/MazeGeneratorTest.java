package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.stream.Stream;
import neon.util.Dice;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for MazeGenerator with deterministic seeded random behavior. */
class MazeGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for standard maze generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width maze width
   * @param height maze height
   * @param sparse sparseness factor (higher = more sparse)
   * @param randomness randomness factor
   */
  record MazeScenario(long seed, int width, int height, int sparse, int randomness) {

    @Override
    public String toString() {
      return String.format(
          "seed=%d, %dx%d, sparse=%d, random=%d", seed, width, height, sparse, randomness);
    }
  }

  /**
   * Test scenario for squashed maze generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width maze width
   * @param height maze height
   * @param sparse sparseness factor
   */
  record SquashedMazeScenario(long seed, int width, int height, int sparse) {

    @Override
    public String toString() {
      return String.format("seed=%d, %dx%d, sparse=%d", seed, width, height, sparse);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<MazeScenario> mazeScenarios() {
    return Stream.of(
        new MazeScenario(42L, 21, 21, 10, 50),
        new MazeScenario(999L, 31, 31, 5, 30),
        new MazeScenario(123L, 25, 25, 15, 70),
        new MazeScenario(264L, 41, 31, 8, 40));
  }

  static Stream<SquashedMazeScenario> squashedMazeScenarios() {
    return Stream.of(
        new SquashedMazeScenario(42L, 20, 20, 10),
        new SquashedMazeScenario(999L, 30, 30, 5),
        new SquashedMazeScenario(123L, 25, 25, 15),
        new SquashedMazeScenario(264L, 35, 25, 8));
  }

  // ==================== Standard Maze Tests ====================

  @ParameterizedTest(name = "generateMaze: {0}")
  @MethodSource("mazeScenarios")
  void generateMaze_generatesValidMaze(MazeScenario scenario) {
    // Given
    MazeGenerator generator = new MazeGenerator(Dice.withSeed(scenario.seed()));

    // When
    Area maze =
        generator.generateMaze(
            scenario.width(), scenario.height(), scenario.sparse(), scenario.randomness());

    // Then: visualize
    System.out.println("Standard Maze: " + scenario);
    System.out.println(visualize(maze, scenario.width(), scenario.height()));
    System.out.println();

    // Verify
    assertAll(
        () -> assertFalse(maze.isEmpty(), "Maze should not be empty"),
        () -> assertMazeHasCells(maze, "Maze should have walkable cells"),
        () ->
            assertMazeIsConnected(
                maze, scenario.width(), scenario.height(), "Maze should be connected"));
  }

  @ParameterizedTest(name = "generateMaze determinism: {0}")
  @MethodSource("mazeScenarios")
  void generateMaze_isDeterministic(MazeScenario scenario) {
    // Given: two generators with the same seed
    MazeGenerator generator1 = new MazeGenerator(Dice.withSeed(scenario.seed()));
    MazeGenerator generator2 = new MazeGenerator(Dice.withSeed(scenario.seed()));

    // When
    Area maze1 =
        generator1.generateMaze(
            scenario.width(), scenario.height(), scenario.sparse(), scenario.randomness());
    Area maze2 =
        generator2.generateMaze(
            scenario.width(), scenario.height(), scenario.sparse(), scenario.randomness());

    // Then
    assertAreasEqual(maze1, maze2, scenario.width(), scenario.height());
  }

  // ==================== Squashed Maze Tests ====================

  @ParameterizedTest(name = "generateSquashedMaze: {0}")
  @MethodSource("squashedMazeScenarios")
  void generateSquashedMaze_generatesValidMaze(SquashedMazeScenario scenario) {
    // Given
    MazeGenerator generator = new MazeGenerator(Dice.withSeed(scenario.seed()));

    // When
    Area maze =
        generator.generateSquashedMaze(scenario.width(), scenario.height(), scenario.sparse());

    // Then: visualize
    System.out.println("Squashed Maze: " + scenario);
    System.out.println(visualize(maze, scenario.width(), scenario.height()));
    System.out.println();

    // Verify
    assertAll(
        () -> assertFalse(maze.isEmpty(), "Maze should not be empty"),
        () -> assertMazeHasCells(maze, "Maze should have walkable cells"),
        () ->
            assertMazeIsConnected(
                maze, scenario.width(), scenario.height(), "Maze should be connected"));
  }

  @ParameterizedTest(name = "generateSquashedMaze determinism: {0}")
  @MethodSource("squashedMazeScenarios")
  void generateSquashedMaze_isDeterministic(SquashedMazeScenario scenario) {
    // Given: two generators with the same seed
    MazeGenerator generator1 = new MazeGenerator(Dice.withSeed(scenario.seed()));
    MazeGenerator generator2 = new MazeGenerator(Dice.withSeed(scenario.seed()));

    // When
    Area maze1 =
        generator1.generateSquashedMaze(scenario.width(), scenario.height(), scenario.sparse());
    Area maze2 =
        generator2.generateSquashedMaze(scenario.width(), scenario.height(), scenario.sparse());

    // Then
    assertAreasEqual(maze1, maze2, scenario.width(), scenario.height());
  }

  // ==================== Assertion Helpers ====================

  private void assertMazeHasCells(Area maze, String message) {
    // Check that the maze contains at least some rectangles
    Rectangle bounds = maze.getBounds();
    assertTrue(bounds.width > 0 && bounds.height > 0, message);
  }

  private void assertMazeIsConnected(Area maze, int width, int height, String message) {
    TileConnectivityAssertions.assertAreaFullyConnected(maze, width, height, message);
  }

  private void assertAreasEqual(Area area1, Area area2, int width, int height) {
    boolean[][] grid1 = TileConnectivityAssertions.areaToGrid(area1, width, height);
    boolean[][] grid2 = TileConnectivityAssertions.areaToGrid(area2, width, height);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        assertEquals(grid1[x][y], grid2[x][y], String.format("Cell at (%d,%d) should match", x, y));
      }
    }
  }

  // ==================== Visualization ====================

  /**
   * Visualizes a maze Area as an ASCII grid.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'.' = walkable cell (part of maze)
   *   <li>'#' = wall (not part of maze)
   * </ul>
   */
  private String visualize(Area maze, int width, int height) {
    boolean[][] grid = TileConnectivityAssertions.areaToGrid(maze, width, height);

    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");

    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(grid[x][y] ? '.' : '#');
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add cell count summary
    int cellCount = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (grid[x][y]) cellCount++;
      }
    }
    sb.append("\nWalkable cells: ").append(cellCount);

    return sb.toString();
  }
}
