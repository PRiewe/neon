package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for BlocksGenerator with deterministic seeded random behavior. */
class BlocksGeneratorTest {

  private static final char[] MARKERS = {'#', '@', '*', '+', '%', '&', '=', '~'};

  /**
   * Test scenario parameters for sparse rectangle generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width bounding area width
   * @param height bounding area height
   * @param minSize minimum rectangle dimension
   * @param maxSize maximum rectangle dimension
   * @param ratio maximum aspect ratio
   * @param numRectangles number of rectangles to generate
   */
  record SparseScenario(
      long seed, int width, int height, int minSize, int maxSize, double ratio, int numRectangles) {

    @Override
    public String toString() {
      return String.format(
          "seed=%d, %dx%d, size=[%d-%d], ratio=%.1f, n=%d",
          seed, width, height, minSize, maxSize, ratio, numRectangles);
    }
  }

  static Stream<SparseScenario> sparseRectangleScenarios() {
    return Stream.of(
        new SparseScenario(42L, 20, 10, 3, 5, 2.0, 4),
        new SparseScenario(264L, 20, 10, 3, 5, 2.0, 4),
        new SparseScenario(264L, 20, 10, 3, 6, 1.5, 4),
        new SparseScenario(999L, 30, 15, 4, 8, 2.0, 6),
        new SparseScenario(123L, 15, 15, 10, 10, 1.0, 8));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sparseRectangleScenarios")
  void createSparseRectangles_generatesValidNonOverlappingRectangles(SparseScenario scenario) {
    // Given
    BlocksGenerator generator = new BlocksGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    ArrayList<Rectangle> rectangles =
        generator.createSparseRectangles(
            scenario.width(),
            scenario.height(),
            scenario.minSize(),
            scenario.maxSize(),
            scenario.ratio(),
            scenario.numRectangles());

    // Then: visualize
    System.out.println("Scenario: " + scenario);
    System.out.println(visualize(rectangles, scenario.width(), scenario.height()));
    System.out.println();

    // Verify
    assertAll(
        () -> assertFalse(rectangles.isEmpty(), "Should generate at least one rectangle"),
        () -> assertRectanglesWithinBounds(rectangles, scenario.width(), scenario.height()),
        () ->
            assertRectanglesMeetSizeConstraints(rectangles, scenario.minSize(), scenario.maxSize()),
        () -> assertRectanglesDoNotOverlap(rectangles));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sparseRectangleScenarios")
  void createSparseRectangles_isDeterministic(SparseScenario scenario) {
    // Given: two generators with the same seed
    BlocksGenerator generator1 = new BlocksGenerator(MapUtils.withSeed(scenario.seed()));
    BlocksGenerator generator2 = new BlocksGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    ArrayList<Rectangle> result1 =
        generator1.createSparseRectangles(
            scenario.width(),
            scenario.height(),
            scenario.minSize(),
            scenario.maxSize(),
            scenario.ratio(),
            scenario.numRectangles());
    ArrayList<Rectangle> result2 =
        generator2.createSparseRectangles(
            scenario.width(),
            scenario.height(),
            scenario.minSize(),
            scenario.maxSize(),
            scenario.ratio(),
            scenario.numRectangles());

    // Then
    assertEquals(result1.size(), result2.size(), "Same seed should produce same count");
    for (int i = 0; i < result1.size(); i++) {
      assertEquals(result1.get(i), result2.get(i), "Rectangle " + i + " should match");
    }
  }

  // --- Assertion helpers ---

  private void assertRectanglesWithinBounds(
      ArrayList<Rectangle> rectangles, int width, int height) {
    for (int i = 0; i < rectangles.size(); i++) {
      Rectangle r = rectangles.get(i);
      int idx = i;
      assertAll(
          "Rectangle " + idx + " bounds",
          () -> assertTrue(r.x >= 0, "x >= 0"),
          () -> assertTrue(r.y >= 0, "y >= 0"),
          () -> assertTrue(r.x + r.width <= width, "x + width <= " + width),
          () -> assertTrue(r.y + r.height <= height, "y + height <= " + height));
    }
  }

  private void assertRectanglesMeetSizeConstraints(
      ArrayList<Rectangle> rectangles, int minSize, int maxSize) {
    for (int i = 0; i < rectangles.size(); i++) {
      Rectangle r = rectangles.get(i);
      int idx = i;
      assertAll(
          "Rectangle " + idx + " size",
          () -> assertTrue(r.width >= minSize, "width >= " + minSize),
          () -> assertTrue(r.height >= minSize, "height >= " + minSize),
          () -> assertTrue(r.width <= maxSize, "width <= " + maxSize),
          () -> assertTrue(r.height <= maxSize, "height <= " + maxSize));
    }
  }

  private void assertRectanglesDoNotOverlap(ArrayList<Rectangle> rectangles) {
    for (int i = 0; i < rectangles.size(); i++) {
      for (int j = i + 1; j < rectangles.size(); j++) {
        Rectangle r1 = rectangles.get(i);
        Rectangle r2 = rectangles.get(j);
        assertFalse(
            r1.intersects(r2), String.format("Rectangles %d and %d should not overlap", i, j));
      }
    }
  }

  // --- Visualization ---

  /**
   * Visualizes rectangles as an ASCII grid.
   *
   * <p>Example output:
   *
   * <pre>
   * +--------------------+
   * |    ####            |
   * |    ####   #####    |
   * |    ####   #####    |
   * |           #####    |
   * |  @@@               |
   * |  @@@               |
   * +--------------------+
   * Rectangles: 2
   *   [#] x=4, y=0, w=4, h=4
   *   [@] x=2, y=4, w=3, h=2
   * </pre>
   */
  private String visualize(ArrayList<Rectangle> rectangles, int width, int height) {
    char[][] grid = new char[height][width];

    // Initialize with empty space
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        grid[y][x] = '.';
      }
    }

    // Fill rectangles with different characters
    for (int i = 0; i < rectangles.size(); i++) {
      Rectangle r = rectangles.get(i);
      char marker = MARKERS[i % MARKERS.length];
      for (int y = r.y; y < r.y + r.height && y < height; y++) {
        for (int x = r.x; x < r.x + r.width && x < width; x++) {
          grid[y][x] = marker;
        }
      }
    }

    // Build string representation
    StringBuilder sb = new StringBuilder();
    sb.append("+").append("-".repeat(width)).append("+\n");
    for (int y = 0; y < height; y++) {
      sb.append("|");
      for (int x = 0; x < width; x++) {
        sb.append(grid[y][x]);
      }
      sb.append("|\n");
    }
    sb.append("+").append("-".repeat(width)).append("+");

    // Add rectangle details
    sb.append("\nRectangles: ").append(rectangles.size());
    for (int i = 0; i < rectangles.size(); i++) {
      Rectangle r = rectangles.get(i);
      sb.append(
          String.format(
              "\n  [%c] x=%d, y=%d, w=%d, h=%d",
              MARKERS[i % MARKERS.length], r.x, r.y, r.width, r.height));
    }

    return sb.toString();
  }
}
