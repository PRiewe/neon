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
    System.out.println(visualize(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () -> assertConnectedDungeon(tiles, "Dungeon should be connected"));
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
    assertTilesMatch(tiles1, tiles2);
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
    System.out.println(visualize(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () -> assertConnectedDungeon(tiles, "Dungeon should be connected"));
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
    assertTilesMatch(tiles1, tiles2);
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
    System.out.println(visualize(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Tiles width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Tiles height should match"),
        () -> assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () -> assertConnectedDungeon(tiles, "Dungeon should be connected"));
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
    assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Assertion Helpers ====================

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

  private void assertConnectedDungeon(int[][] tiles, String message) {
    // Count floor tiles and verify flood fill reaches all of them
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

    // Flood fill from start position
    boolean[][] visited = new boolean[tiles.length][tiles[0].length];
    int reachable = floodFillCount(tiles, visited, startX, startY);

    assertEquals(floorCount, reachable, message + " - not all walkable tiles are connected");
  }

  private boolean isWalkable(int tile) {
    return tile == MapUtils.FLOOR
        || tile == MapUtils.CORRIDOR
        || tile == MapUtils.DOOR
        || tile == MapUtils.DOOR_CLOSED
        || tile == MapUtils.DOOR_LOCKED;
  }

  private int floodFillCount(int[][] tiles, boolean[][] visited, int x, int y) {
    if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length) {
      return 0;
    }
    if (visited[x][y] || !isWalkable(tiles[x][y])) {
      return 0;
    }

    visited[x][y] = true;
    int count = 1;
    count += floodFillCount(tiles, visited, x - 1, y);
    count += floodFillCount(tiles, visited, x + 1, y);
    count += floodFillCount(tiles, visited, x, y - 1);
    count += floodFillCount(tiles, visited, x, y + 1);
    return count;
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
   *   <li>'D' = DOOR (open)
   *   <li>'d' = DOOR_CLOSED
   *   <li>'L' = DOOR_LOCKED
   *   <li>'E' = ENTRY
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
    int[] counts = new int[16]; // Room for all tile types
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
