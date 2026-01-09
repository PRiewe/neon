package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for FeatureGenerator with deterministic seeded random behavior. */
class FeatureGeneratorTest {

  private static final String GRASS = "grass";
  private static final String WATER = "water";

  // ==================== Scenario Records ====================

  /**
   * Test scenario for lake generation.
   *
   * @param seed random seed for deterministic behavior
   * @param terrainWidth terrain array width
   * @param terrainHeight terrain array height
   * @param boundsX lake bounds x
   * @param boundsY lake bounds y
   * @param boundsWidth lake bounds width
   * @param boundsHeight lake bounds height
   */
  record LakeScenario(
      long seed,
      int terrainWidth,
      int terrainHeight,
      int boundsX,
      int boundsY,
      int boundsWidth,
      int boundsHeight) {

    Rectangle bounds() {
      return new Rectangle(boundsX, boundsY, boundsWidth, boundsHeight);
    }

    @Override
    public String toString() {
      return String.format(
          "seed=%d, terrain=%dx%d, bounds=(%d,%d,%d,%d)",
          seed, terrainWidth, terrainHeight, boundsX, boundsY, boundsWidth, boundsHeight);
    }
  }

  /**
   * Test scenario for river generation.
   *
   * @param seed random seed for deterministic behavior
   * @param terrainWidth terrain array width
   * @param terrainHeight terrain array height
   * @param riverSize width of the river
   */
  record RiverScenario(long seed, int terrainWidth, int terrainHeight, int riverSize) {

    @Override
    public String toString() {
      return String.format(
          "seed=%d, terrain=%dx%d, size=%d", seed, terrainWidth, terrainHeight, riverSize);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<LakeScenario> lakeScenarios() {
    return Stream.of(
        new LakeScenario(42L, 50, 50, 10, 10, 30, 30),
        new LakeScenario(999L, 60, 40, 15, 5, 25, 25),
        new LakeScenario(123L, 40, 60, 5, 15, 20, 30),
        new LakeScenario(264L, 80, 80, 20, 20, 40, 40));
  }

  static Stream<RiverScenario> riverScenarios() {
    return Stream.of(
        new RiverScenario(42L, 50, 50, 3),
        new RiverScenario(999L, 60, 40, 5),
        new RiverScenario(123L, 40, 60, 4),
        new RiverScenario(264L, 80, 80, 6));
  }

  // ==================== Lake Tests ====================

  @ParameterizedTest(name = "generateLake: {0}")
  @MethodSource("lakeScenarios")
  void generateLake_generatesValidLake(LakeScenario scenario) {
    // Given
    FeatureGenerator generator = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    String[][] terrain = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);

    // When
    generator.generateLake(terrain, WATER, scenario.bounds());

    // Then: visualize
    System.out.println("Lake: " + scenario);
    System.out.println(TileVisualization.visualizeTerrain(terrain));
    System.out.println();

    // Verify
    assertAll(
        () -> assertFeatureExists(terrain, WATER, "Lake should have water tiles"),
        () ->
            assertFeatureWithinBounds(
                terrain, WATER, scenario.bounds(), "Lake should be within bounds"));
  }

  @ParameterizedTest(name = "generateLake determinism: {0}")
  @MethodSource("lakeScenarios")
  void generateLake_isDeterministic(LakeScenario scenario) {
    // Given: two generators with the same seed
    FeatureGenerator generator1 = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    FeatureGenerator generator2 = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    String[][] terrain1 = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);
    String[][] terrain2 = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);

    // When
    generator1.generateLake(terrain1, WATER, scenario.bounds());
    generator2.generateLake(terrain2, WATER, scenario.bounds());

    // Then
    assertTerrainEquals(terrain1, terrain2);
  }

  // ==================== River Tests ====================

  @ParameterizedTest(name = "generateRiver: {0}")
  @MethodSource("riverScenarios")
  void generateRiver_generatesValidRiver(RiverScenario scenario) {
    // Given
    FeatureGenerator generator = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    String[][] terrain = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);
    int[][] tiles = new int[scenario.terrainWidth()][scenario.terrainHeight()];

    // When
    generator.generateRiver(terrain, tiles, WATER, scenario.riverSize());

    // Then: visualize
    System.out.println("River: " + scenario);
    System.out.println(TileVisualization.visualizeTerrain(terrain));
    System.out.println();

    // Verify
    assertFeatureExists(terrain, WATER, "River should have water tiles");
  }

  @ParameterizedTest(name = "generateRiver determinism: {0}")
  @MethodSource("riverScenarios")
  void generateRiver_isDeterministic(RiverScenario scenario) {
    // Given: two generators with the same seed
    FeatureGenerator generator1 = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    FeatureGenerator generator2 = new FeatureGenerator(MapUtils.withSeed(scenario.seed()));
    String[][] terrain1 = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);
    String[][] terrain2 = createTerrain(scenario.terrainWidth(), scenario.terrainHeight(), GRASS);
    int[][] tiles1 = new int[scenario.terrainWidth()][scenario.terrainHeight()];
    int[][] tiles2 = new int[scenario.terrainWidth()][scenario.terrainHeight()];

    // When
    generator1.generateRiver(terrain1, tiles1, WATER, scenario.riverSize());
    generator2.generateRiver(terrain2, tiles2, WATER, scenario.riverSize());

    // Then
    assertTerrainEquals(terrain1, terrain2);
  }

  // ==================== Helper Methods ====================

  private String[][] createTerrain(int width, int height, String defaultType) {
    String[][] terrain = new String[width][height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        terrain[x][y] = defaultType;
      }
    }
    return terrain;
  }

  // ==================== Assertion Helpers ====================

  private void assertFeatureExists(String[][] terrain, String type, String message) {
    boolean found = false;
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[x].length; y++) {
        if (type.equals(terrain[x][y])) {
          found = true;
          break;
        }
      }
      if (found) break;
    }
    assertTrue(found, message);
  }

  private void assertFeatureWithinBounds(
      String[][] terrain, String type, Rectangle bounds, String message) {
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[x].length; y++) {
        if (type.equals(terrain[x][y])) {
          assertTrue(
              bounds.contains(x, y),
              message + String.format(" - found %s at (%d,%d) outside bounds", type, x, y));
        }
      }
    }
  }

  private void assertTerrainEquals(String[][] terrain1, String[][] terrain2) {
    assertEquals(terrain1.length, terrain2.length, "Terrain width should match");
    for (int x = 0; x < terrain1.length; x++) {
      assertEquals(terrain1[x].length, terrain2[x].length, "Terrain height should match at x=" + x);
      for (int y = 0; y < terrain1[x].length; y++) {
        assertEquals(
            terrain1[x][y], terrain2[x][y], String.format("Terrain at (%d,%d) should match", x, y));
      }
    }
  }

  // ==================== Visualization ====================

}
