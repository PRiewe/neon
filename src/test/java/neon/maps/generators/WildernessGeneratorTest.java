package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import neon.resources.RRegionTheme;
import neon.util.Dice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for WildernessGenerator terrain generation algorithms.
 *
 * <p>Tests focus on deterministic behavior, terrain patterns, and algorithm correctness.
 */
class WildernessGeneratorTest {

  // ==================== Configuration ====================

  /** Controls whether terrain visualizations are printed to stdout during tests. */
  private static final boolean PRINT_OUTPUT = false;

  // ==================== Scenario Records ====================

  /**
   * Test scenario for island generation (cellular automata).
   *
   * @param seed random seed for deterministic behavior
   * @param width grid width
   * @param height grid height
   * @param probability initial fill probability (0-100)
   * @param neighbors minimum neighbors to stay filled
   * @param iterations number of cellular automata iterations
   */
  record IslandScenario(
      long seed, int width, int height, int probability, int neighbors, int iterations) {
    @Override
    public String toString() {
      return String.format(
          "seed=%d, %dx%d, prob=%d%%, n=%d, iter=%d",
          seed, width, height, probability, neighbors, iterations);
    }
  }

  /**
   * Test scenario for editor-mode terrain generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width terrain width
   * @param height terrain height
   */
  record EditorScenario(long seed, int width, int height) {
    @Override
    public String toString() {
      return String.format("seed=%d, %dx%d", seed, width, height);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<IslandScenario> islandScenarios() {
    return Stream.of(
        new IslandScenario(42L, 20, 20, 45, 4, 4),
        new IslandScenario(999L, 30, 30, 50, 4, 4),
        new IslandScenario(12345L, 15, 15, 40, 4, 5),
        new IslandScenario(777L, 25, 20, 55, 4, 3),
        new IslandScenario(555L, 10, 10, 45, 4, 4));
  }

  static Stream<IslandScenario> edgeCaseIslandScenarios() {
    return Stream.of(
        new IslandScenario(42L, 5, 5, 45, 4, 4), // Small size
        new IslandScenario(999L, 3, 3, 50, 4, 2), // Minimum size
        new IslandScenario(123L, 20, 20, 0, 4, 4), // No initial fill
        new IslandScenario(456L, 20, 20, 100, 4, 4)); // Complete fill
  }

  static Stream<EditorScenario> editorScenarios() {
    return Stream.of(
        new EditorScenario(42L, 20, 20),
        new EditorScenario(999L, 30, 25),
        new EditorScenario(123L, 15, 15),
        new EditorScenario(456L, 25, 30));
  }

  // ==================== Island Generation Tests ====================

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("islandScenarios")
  void generateTerrainOnlyIslands_withVariousSeeds_generatesValidPatterns(IslandScenario scenario) {
    // Given
    var generator = createGenerator(scenario.seed());

    // When
    boolean[][] islands =
        generator.generateIslands(
            scenario.width(),
            scenario.height(),
            scenario.probability(),
            scenario.neighbors(),
            scenario.iterations());

    // Then: visualize if enabled
    if (PRINT_OUTPUT) {
      System.out.println("Island Pattern: " + scenario);
      System.out.println(TileVisualization.visualizeGrid(islands));
      System.out.println();
    }

    // Verify
    assertAll(
        () -> assertNotNull(islands, "Islands should not be null"),
        () -> assertEquals(scenario.width(), islands.length, "Width should match"),
        () -> assertEquals(scenario.height(), islands[0].length, "Height should match"));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("islandScenarios")
  void generateTerrainOnlyIslands_isDeterministic(IslandScenario scenario) {
    // Given: two generators with same seed
    var gen1 = createGenerator(scenario.seed());
    var gen2 = createGenerator(scenario.seed());

    // When
    boolean[][] islands1 =
        gen1.generateIslands(
            scenario.width(),
            scenario.height(),
            scenario.probability(),
            scenario.neighbors(),
            scenario.iterations());
    boolean[][] islands2 =
        gen2.generateIslands(
            scenario.width(),
            scenario.height(),
            scenario.probability(),
            scenario.neighbors(),
            scenario.iterations());

    // Then
    assertArrayEquals(islands1, islands2, "Same seed should produce identical islands");
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("edgeCaseIslandScenarios")
  void generateTerrainOnlyIslands_withEdgeCases_handlesCorrectly(IslandScenario scenario) {
    // Given
    var generator = createGenerator(scenario.seed());

    // When
    boolean[][] islands =
        generator.generateIslands(
            scenario.width(),
            scenario.height(),
            scenario.probability(),
            scenario.neighbors(),
            scenario.iterations());

    // Then
    assertNotNull(islands);
    assertEquals(scenario.width(), islands.length);
    assertEquals(scenario.height(), islands[0].length);

    // Check expected patterns for edge cases
    if (scenario.probability() == 0) {
      // With 0% probability and sufficient iterations, should be mostly empty
      int filledCount = countFilled(islands);
      assertTrue(
          filledCount < islands.length * islands[0].length / 4,
          "Low probability should result in mostly empty grid");
    } else if (scenario.probability() == 100) {
      // With 100% probability, should have significant fill
      int filledCount = countFilled(islands);
      assertTrue(
          filledCount > islands.length * islands[0].length / 4,
          "High probability should result in significant fill");
    }
  }

  @Test
  void generateTerrainOnlyIslands_withSmallSize_handlesEdgeNeighbors() {
    // Given: very small grid where edge cases matter
    var generator = createGenerator(42L);

    // When
    boolean[][] islands = generator.generateIslands(3, 3, 50, 4, 3);

    // Then: should handle without errors and respect boundaries
    assertNotNull(islands);
    assertEquals(3, islands.length);
    assertEquals(3, islands[0].length);
  }

  // ==================== Editor Mode Generation Tests ====================
  // Note: Full terrain generation requires RRegionTheme with type set via XML
  // These tests would need integration test setup - skipped for unit tests

  // Placeholder for future integration tests
  // @ParameterizedTest(name = "{index}: {0}")
  // @MethodSource("editorScenarios")
  void generate_TerrainOnly_editorMode_returnsTerrainArray_SKIPPED(EditorScenario scenario) {
    // Given
    var generator = createGenerator(scenario.seed());
    RRegionTheme theme = createTestTheme("grass");

    // When
    String[][] terrain =
        generator.generateTerrainOnly(
            new Rectangle(0, 0, scenario.width(), scenario.height()), theme, "grass");

    // Then: visualize if enabled
    if (PRINT_OUTPUT) {
      System.out.println("Editor Terrain: " + scenario);
      System.out.println(TileVisualization.visualizeTerrain(terrain));
      System.out.println();
    }

    // Verify
    assertAll(
        () -> assertNotNull(terrain, "Terrain should not be null"),
        () -> assertEquals(scenario.width(), terrain.length, "Width should match"),
        () -> assertEquals(scenario.height(), terrain[0].length, "Height should match"));
  }

  // @ParameterizedTest(name = "{index}: {0}")
  // @MethodSource("editorScenarios")
  void generate_TerrainOnly_editorMode_isDeterministic_SKIPPED(EditorScenario scenario) {
    // Given: two generators with same seed
    var gen1 = createGenerator(scenario.seed());
    var gen2 = createGenerator(scenario.seed());
    RRegionTheme theme = createTestTheme("grass");

    // When
    String[][] terrain1 =
        gen1.generateTerrainOnly(
            new Rectangle(0, 0, scenario.width(), scenario.height()), theme, "grass");
    String[][] terrain2 =
        gen2.generateTerrainOnly(
            new Rectangle(0, 0, scenario.width(), scenario.height()), theme, "grass");

    // Then
    TileAssertions.assertTerrainMatch(terrain1, terrain2);
  }

  // @Test
  void generate_TerrainOnly_editorMode_respectsBounds_SKIPPED() {
    // Given
    var gen1 = createGenerator(21L);

    RRegionTheme theme = createTestTheme("stone");
    Rectangle bounds = new Rectangle(10, 10, 20, 15);

    // When
    String[][] terrain = gen1.generateTerrainOnly(bounds, theme, "stone");

    // Then: should respect the bounds
    assertEquals(20, terrain.length, "Width should match bounds");
    assertEquals(15, terrain[0].length, "Height should match bounds");
  }

  // ==================== Helper Methods ====================

  /**
   * Creates a WildernessGenerator with seeded randomness for testing (editor mode).
   *
   * @param seed random seed
   * @return configured generator
   */
  private WildernessTerrainGenerator createGenerator(long seed) {
    String[][] terrain = new String[32][32]; // Default size with padding
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);
    return new WildernessTerrainGenerator(mapUtils, dice);
  }

  /**
   * Creates a WildernessGenerator with specific terrain dimensions.
   *
   * @param seed random seed
   * @param width terrain width
   * @param height terrain height
   * @return configured generator
   */
  private WildernessTerrainGenerator createGeneratorWithTerrain(long seed, int width, int height) {
    String[][] terrain = new String[width][height];
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);
    return new WildernessTerrainGenerator( mapUtils, dice);
  }

  /**
   * Creates a simple test region theme.
   *
   * @param floor floor terrain ID
   * @return test theme
   */
  private RRegionTheme createTestTheme(String floor) {
    return new RRegionTheme("test-" + floor);
  }

  /**
   * Counts filled cells in a boolean grid.
   *
   * @param grid the grid to count
   * @return number of true cells
   */
  private int countFilled(boolean[][] grid) {
    int count = 0;
      for (boolean[] booleans : grid) {
          for (boolean aBoolean : booleans) {
              if (aBoolean) count++;
          }
      }
    return count;
  }
}
