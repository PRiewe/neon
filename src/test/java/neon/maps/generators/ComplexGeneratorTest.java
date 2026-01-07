package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import neon.maps.MapUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for ComplexGenerator with deterministic seeded random behavior. */
class ComplexGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for sparse and packed dungeon generation.
   *
   * @param seed random seed for deterministic behavior
   * @param width dungeon width
   * @param height dungeon height
   * @param numRooms number of rooms to generate
   * @param minSize minimum room size
   * @param maxSize maximum room size
   */
  record DungeonScenario(long seed, int width, int height, int numRooms, int minSize, int maxSize) {

    @Override
    public String toString() {
      return String.format(
          "seed=%d, %dx%d, rooms=%d, size=[%d-%d]",
          seed, width, height, numRooms, minSize, maxSize);
    }
  }

  /**
   * Test scenario for BSP dungeon generation (no room count - BSP partitions space).
   *
   * @param seed random seed for deterministic behavior
   * @param width dungeon width
   * @param height dungeon height
   * @param minSize minimum room size
   * @param maxSize maximum room size
   */
  record BSPDungeonScenario(long seed, int width, int height, int minSize, int maxSize) {

    @Override
    public String toString() {
      return String.format("seed=%d, %dx%d, size=[%d-%d]", seed, width, height, minSize, maxSize);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<DungeonScenario> sparseDungeonScenarios() {
    // Use larger rooms and conservative parameters to avoid edge cases
    return Stream.of(
        new DungeonScenario(42L, 50, 40, 4, 5, 8),
        new DungeonScenario(999L, 60, 50, 5, 5, 9),
        new DungeonScenario(123L, 55, 45, 4, 6, 9));
  }

  /** Large sparse dungeon scenarios at 150x120 to stress test the generator. */
  static Stream<DungeonScenario> largeSparseDungeonScenarios() {
    return Stream.of(
        // Various seeds with different room counts and sizes
        new DungeonScenario(42L, 150, 120, 5, 5, 15),
        new DungeonScenario(123L, 150, 120, 5, 5, 15),
        new DungeonScenario(264L, 150, 120, 5, 5, 15),
        new DungeonScenario(777L, 150, 120, 5, 5, 15),
        new DungeonScenario(999L, 150, 120, 5, 5, 15),
        // More rooms
        new DungeonScenario(42L, 150, 120, 8, 5, 15),
        new DungeonScenario(123L, 150, 120, 10, 5, 15),
        // Smaller rooms
        new DungeonScenario(42L, 150, 120, 5, 5, 10),
        new DungeonScenario(264L, 150, 120, 5, 4, 8),
        // Larger rooms
        new DungeonScenario(42L, 150, 120, 5, 8, 20),
        new DungeonScenario(999L, 150, 120, 4, 10, 25));
  }

  static Stream<BSPDungeonScenario> bspDungeonScenarios() {
    return Stream.of(
        new BSPDungeonScenario(42L, 40, 30, 5, 12),
        new BSPDungeonScenario(264L, 50, 40, 6, 14),
        new BSPDungeonScenario(999L, 60, 45, 5, 15),
        new BSPDungeonScenario(123L, 45, 35, 6, 12));
  }

  static Stream<DungeonScenario> packedDungeonScenarios() {
    return Stream.of(
        new DungeonScenario(42L, 40, 30, 6, 4, 8),
        new DungeonScenario(264L, 50, 40, 8, 5, 10),
        new DungeonScenario(999L, 55, 45, 8, 5, 9),
        new DungeonScenario(777L, 45, 35, 6, 5, 8));
  }

  // ==================== Sparse Dungeon Tests ====================

  @ParameterizedTest(name = "sparse: {0}")
  @MethodSource("sparseDungeonScenarios")
  void generateSparseDungeon_generatesValidDungeon(DungeonScenario scenario) {
    // Given
    ComplexGenerator generator = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generateSparseDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());

    // Then: visualize
    System.out.println("Sparse Dungeon: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () ->
            TileConnectivityAssertions.assertFullyConnected(tiles, "Dungeon should be connected"));
  }

  @ParameterizedTest(name = "sparse determinism: {0}")
  @MethodSource("sparseDungeonScenarios")
  void generateSparseDungeon_isDeterministic(DungeonScenario scenario) {
    // Given: two generators with the same seed
    ComplexGenerator generator1 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));
    ComplexGenerator generator2 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.generateSparseDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());
    int[][] tiles2 =
        generator2.generateSparseDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Large Sparse Dungeon Tests (150x120) ====================

  @ParameterizedTest(name = "large sparse 150x120: {0}")
  @MethodSource("largeSparseDungeonScenarios")
  void generateSparseDungeon_handlesLargeDungeons(DungeonScenario scenario) {
    // Given
    ComplexGenerator generator = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generateSparseDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());

    // Then: visualize (can be commented out for large dungeons)
    // System.out.println("Large Sparse Dungeon: " + scenario);
    // System.out.println(TileVisualization.visualizeTiles(tiles));
    // System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () ->
            TileConnectivityAssertions.assertFullyConnected(tiles, "Dungeon should be connected"));
  }

  // ==================== BSP Dungeon Tests ====================

  @ParameterizedTest(name = "BSP: {0}")
  @MethodSource("bspDungeonScenarios")
  void generateBSPDungeon_generatesValidDungeon(BSPDungeonScenario scenario) {
    // Given
    ComplexGenerator generator = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generateBSPDungeon(
            scenario.width(), scenario.height(), scenario.minSize(), scenario.maxSize());

    // Then: visualize
    System.out.println("BSP Dungeon: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () ->
            TileConnectivityAssertions.assertFullyConnected(tiles, "Dungeon should be connected"));
  }

  @ParameterizedTest(name = "BSP determinism: {0}")
  @MethodSource("bspDungeonScenarios")
  void generateBSPDungeon_isDeterministic(BSPDungeonScenario scenario) {
    // Given: two generators with the same seed
    ComplexGenerator generator1 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));
    ComplexGenerator generator2 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.generateBSPDungeon(
            scenario.width(), scenario.height(), scenario.minSize(), scenario.maxSize());
    int[][] tiles2 =
        generator2.generateBSPDungeon(
            scenario.width(), scenario.height(), scenario.minSize(), scenario.maxSize());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Packed Dungeon Tests ====================

  @ParameterizedTest(name = "packed: {0}")
  @MethodSource("packedDungeonScenarios")
  void generatePackedDungeon_generatesValidDungeon(DungeonScenario scenario) {
    // Given
    ComplexGenerator generator = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles =
        generator.generatePackedDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());

    // Then: visualize
    System.out.println("Packed Dungeon: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () ->
            TileConnectivityAssertions.assertFullyConnected(tiles, "Dungeon should be connected"));
  }

  @ParameterizedTest(name = "packed determinism: {0}")
  @MethodSource("packedDungeonScenarios")
  void generatePackedDungeon_isDeterministic(DungeonScenario scenario) {
    // Given: two generators with the same seed
    ComplexGenerator generator1 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));
    ComplexGenerator generator2 = new ComplexGenerator(MapUtils.withSeed(scenario.seed()));

    // When
    int[][] tiles1 =
        generator1.generatePackedDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());
    int[][] tiles2 =
        generator2.generatePackedDungeon(
            scenario.width(),
            scenario.height(),
            scenario.numRooms(),
            scenario.minSize(),
            scenario.maxSize());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Assertion Helpers ====================

  // ==================== Visualization ====================

}
