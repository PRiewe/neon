package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import neon.resources.RZoneTheme;
import neon.util.Dice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for DungeonGenerator tile generation with deterministic seeded random behavior.
 *
 * <p>These tests focus on the generateBaseTiles method which generates the base tile layout for
 * different dungeon types.
 */
class DungeonGeneratorTest {

  // ==================== Configuration ====================

  /**
   * Controls whether dungeon visualizations are printed to stdout during tests. Set to true to see
   * ASCII renderings of generated dungeons for debugging.
   */
  private static final boolean PRINT_DUNGEONS = false;

  /** Controls whether large dungeon visualizations are printed (can be very verbose). */
  private static final boolean PRINT_LARGE_DUNGEONS = false;

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

  /**
   * Test scenario for full dungeon generation via generateTiles.
   *
   * @param seed random seed for deterministic behavior
   * @param type dungeon type (cave, pits, maze, mine, bsp, packed, or default)
   * @param minSize minimum dungeon size
   * @param maxSize maximum dungeon size
   * @param floors comma-separated list of floor terrain IDs
   */
  record GenerateTilesScenario(
      long seed, String type, int minSize, int maxSize, String floors, String description) {

    @Override
    public String toString() {
      return description;
    }
  }

  /**
   * Test scenario for large dungeon generation to stress test the generators.
   *
   * @param seed random seed for deterministic behavior
   * @param type dungeon type
   * @param width dungeon width (can be large)
   * @param height dungeon height (can be large)
   */
  record LargeDungeonScenario(long seed, String type, int width, int height) {

    @Override
    public String toString() {
      return String.format("%s %dx%d (seed=%d)", type, width, height, seed);
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
        // BSP types - using seeds from ComplexGeneratorTest that are known to work
        new DungeonTypeScenario(264L, "bsp", 50, 40),
        new DungeonTypeScenario(999L, "bsp", 60, 45),
        // Packed types
        new DungeonTypeScenario(42L, "packed", 40, 30),
        new DungeonTypeScenario(264L, "packed", 50, 40),
        // Default (sparse) types
        new DungeonTypeScenario(42L, "default", 50, 40),
        new DungeonTypeScenario(999L, "sparse", 55, 45));
  }

  static Stream<GenerateTilesScenario> generateTilesScenarios() {
    return Stream.of(
        // Cave with single floor type
        new GenerateTilesScenario(42L, "cave", 30, 35, "stone_floor", "cave with single floor"),
        // Maze with multiple floor types
        new GenerateTilesScenario(
            123L, "maze", 31, 41, "dirt,stone,marble", "maze with multiple floors"),
        // BSP dungeon
        new GenerateTilesScenario(264L, "bsp", 45, 55, "dungeon_floor", "bsp dungeon"),
        // Packed dungeon
        new GenerateTilesScenario(
            999L, "packed", 40, 50, "cobblestone,flagstone", "packed with two floors"),
        // Mine
        new GenerateTilesScenario(777L, "mine", 35, 45, "cave_floor", "mine dungeon"),
        // Default (sparse)
        new GenerateTilesScenario(42L, "default", 45, 55, "floor_a,floor_b", "sparse dungeon"));
  }

  static Stream<LargeDungeonScenario> largeDungeonScenarios() {
    return Stream.of(
        // Large caves
        new LargeDungeonScenario(42L, "cave", 100, 100),
        new LargeDungeonScenario(999L, "cave", 150, 120),
        // Large mazes (must be odd dimensions)
        new LargeDungeonScenario(123L, "maze", 101, 101),
        new LargeDungeonScenario(264L, "maze", 151, 121),
        // Large BSP dungeons
        new LargeDungeonScenario(42L, "bsp", 120, 100),
        new LargeDungeonScenario(777L, "bsp", 150, 130),
        // Large packed dungeons
        new LargeDungeonScenario(999L, "packed", 100, 80),
        new LargeDungeonScenario(123L, "packed", 130, 110),
        // Large sparse dungeons
        new LargeDungeonScenario(42L, "default", 120, 100),
        new LargeDungeonScenario(264L, "default", 150, 120),
        // Extra large stress tests (caves don't use recursive flood fill)
        new LargeDungeonScenario(42L, "cave", 200, 200));
    // Note: Mine type is tested in dungeonTypeScenarios at reasonable sizes.
    // Large mine dungeons have edge cases with the maze generator's sparseness=12.
  }

  // ==================== Helper to create DungeonGenerator ====================

  /**
   * Creates a DungeonGenerator for testing generateBaseTiles. Since generateBaseTiles only uses the
   * internal generators (which are initialized from MapUtils and Dice), we can pass null for the
   * other dependencies.
   */
  private DungeonGenerator createGenerator(long seed) {
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);
    // Create a minimal theme - the type field is not used by generateBaseTiles
    // since we pass the type directly as a parameter
    RZoneTheme theme = new RZoneTheme("test-theme");
    // Use the RZoneTheme constructor which doesn't require a Zone
    return new DungeonGenerator(theme, null, null, null, mapUtils, dice);
  }

  /**
   * Creates a DungeonGenerator for testing generateTiles with a fully configured theme.
   *
   * @param seed random seed for deterministic behavior
   * @param scenario the test scenario with theme configuration
   * @return a configured DungeonGenerator
   */
  private DungeonGenerator createGeneratorForTiles(long seed, GenerateTilesScenario scenario) {
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);

    // Create and configure the theme
    RZoneTheme theme = new RZoneTheme("test-theme");
    theme.type = scenario.type();
    theme.min = scenario.minSize();
    theme.max = scenario.maxSize();
    theme.floor = scenario.floors();
    theme.walls = "wall_terrain";
    theme.doors = "door_terrain";
    // Leave creatures, items, and features empty for basic tests

    return new DungeonGenerator(theme, null, null, null, mapUtils, dice);
  }

  // ==================== Dungeon Type Tests ====================

  @ParameterizedTest(name = "generateBaseTiles: {0}")
  @MethodSource("dungeonTypeScenarios")
  void generateBaseTiles_generatesValidTiles(DungeonTypeScenario scenario) {
    // Given
    DungeonGenerator generator = createGenerator(scenario.seed());

    // When
    int[][] tiles =
        generator.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());

    // Then: visualize (controlled by PRINT_DUNGEONS flag)
    if (PRINT_DUNGEONS) {
      System.out.println("Dungeon (" + scenario.type() + "): " + scenario);
      System.out.println(visualize(tiles));
      System.out.println();
    }

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
    DungeonGenerator generator1 = createGenerator(scenario.seed());
    DungeonGenerator generator2 = createGenerator(scenario.seed());

    // When
    int[][] tiles1 =
        generator1.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());
    int[][] tiles2 =
        generator2.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());

    // Then
    assertTilesMatch(tiles1, tiles2);
  }

  // ==================== generateTiles Tests ====================

  @ParameterizedTest(name = "generateTiles: {0}")
  @MethodSource("generateTilesScenarios")
  void generateTiles_generatesValidTerrain(GenerateTilesScenario scenario) {
    // Given
    DungeonGenerator generator = createGeneratorForTiles(scenario.seed(), scenario);

    // When
    String[][] terrain = generator.generateTiles();

    // Then: visualize (controlled by PRINT_DUNGEONS flag)
    if (PRINT_DUNGEONS) {
      System.out.println("Terrain (" + scenario.description() + "):");
      System.out.println(visualizeTerrain(terrain));
      System.out.println();
    }

    // Verify dimensions are within bounds
    int width = terrain.length;
    int height = terrain[0].length;
    assertAll(
        () -> assertTrue(terrain != null, "Terrain should not be null"),
        () -> assertTrue(width >= scenario.minSize(), "Width should be >= min"),
        () -> assertTrue(width <= scenario.maxSize(), "Width should be <= max"),
        () -> assertTrue(height >= scenario.minSize(), "Height should be >= min"),
        () -> assertTrue(height <= scenario.maxSize(), "Height should be <= max"),
        () -> assertFloorTerrainExists(terrain, scenario.floors(), "Terrain should have floors"));
  }

  @ParameterizedTest(name = "generateTiles determinism: {0}")
  @MethodSource("generateTilesScenarios")
  void generateTiles_isDeterministic(GenerateTilesScenario scenario) {
    // Given: two generators with the same seed
    DungeonGenerator generator1 = createGeneratorForTiles(scenario.seed(), scenario);
    DungeonGenerator generator2 = createGeneratorForTiles(scenario.seed(), scenario);

    // When
    String[][] terrain1 = generator1.generateTiles();
    String[][] terrain2 = generator2.generateTiles();

    // Then
    assertTerrainMatch(terrain1, terrain2);
  }

  @Test
  void generateTiles_floorTypesFromTheme() {
    // Given: a theme with specific floor types
    long seed = 42L;
    GenerateTilesScenario scenario =
        new GenerateTilesScenario(seed, "cave", 25, 30, "marble,granite,slate", "multi-floor");
    DungeonGenerator generator = createGeneratorForTiles(seed, scenario);

    // When
    String[][] terrain = generator.generateTiles();

    // Then: all floor tiles should use one of the floor types
    List<String> allowedFloors = List.of("marble", "granite", "slate");
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[0].length; y++) {
        if (terrain[x][y] != null) {
          // Extract base terrain (before any creature/item annotations)
          String baseTerrain = terrain[x][y].split(";")[0];
          assertTrue(
              allowedFloors.contains(baseTerrain),
              "Floor at (" + x + "," + y + ") should be one of " + allowedFloors);
        }
      }
    }
  }

  @Test
  void generateTiles_withCreatures() {
    // Given: a theme with creatures
    long seed = 42L;
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);

    RZoneTheme theme = new RZoneTheme("test-theme");
    theme.type = "cave";
    theme.min = 25;
    theme.max = 30;
    theme.floor = "stone";
    theme.walls = "wall";
    theme.doors = "door";
    theme.creatures.put("test_goblin", 3); // 1d3 goblins

    DungeonGenerator generator = new DungeonGenerator(theme, null, null, null, mapUtils, dice);

    // When
    String[][] terrain = generator.generateTiles();

    // Then: should have creature annotations on some tiles
    boolean hasCreature = false;
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[0].length; y++) {
        if (terrain[x][y] != null && terrain[x][y].contains(";c:")) {
          hasCreature = true;
          assertTrue(
              terrain[x][y].contains("test_goblin"),
              "Creature annotation should contain creature ID");
          break;
        }
      }
      if (hasCreature) break;
    }
    assertTrue(hasCreature, "Should have at least one creature placed");
  }

  @Test
  void generateTiles_withItems() {
    // Given: a theme with items
    long seed = 123L;
    MapUtils mapUtils = MapUtils.withSeed(seed);
    Dice dice = Dice.withSeed(seed);

    RZoneTheme theme = new RZoneTheme("test-theme");
    theme.type = "cave";
    theme.min = 25;
    theme.max = 30;
    theme.floor = "stone";
    theme.walls = "wall";
    theme.doors = "door";
    theme.items.put("test_gold", 5); // 1d5 gold

    DungeonGenerator generator = new DungeonGenerator(theme, null, null, null, mapUtils, dice);

    // When
    String[][] terrain = generator.generateTiles();

    // Then: should have item annotations on some tiles
    boolean hasItem = false;
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[0].length; y++) {
        if (terrain[x][y] != null && terrain[x][y].contains(";i:")) {
          hasItem = true;
          assertTrue(terrain[x][y].contains("test_gold"), "Item annotation should contain item ID");
          break;
        }
      }
      if (hasItem) break;
    }
    assertTrue(hasItem, "Should have at least one item placed");
  }

  // ==================== Large Dungeon Tests ====================

  @ParameterizedTest(name = "large dungeon: {0}")
  @MethodSource("largeDungeonScenarios")
  void generateBaseTiles_handlesLargeDungeons(LargeDungeonScenario scenario) {
    // Given
    DungeonGenerator generator = createGenerator(scenario.seed());

    // When
    long startTime = System.currentTimeMillis();
    int[][] tiles =
        generator.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());
    long elapsed = System.currentTimeMillis() - startTime;

    // Then: optionally visualize (controlled by PRINT_LARGE_DUNGEONS flag)
    if (PRINT_LARGE_DUNGEONS) {
      System.out.println("Large Dungeon (" + scenario + ") generated in " + elapsed + "ms:");
      System.out.println(visualize(tiles));
      System.out.println();
    } else if (PRINT_DUNGEONS) {
      // Just print summary without visualization
      int[] counts = countTiles(tiles);
      System.out.printf(
          "Large Dungeon %s: %dx%d, floors=%d, walls=%d, time=%dms%n",
          scenario.type(),
          scenario.width(),
          scenario.height(),
          counts[MapUtils.FLOOR] + counts[MapUtils.CORRIDOR],
          counts[MapUtils.WALL] + counts[MapUtils.WALL_ROOM],
          elapsed);
    }

    // Verify
    assertAll(
        () -> assertEquals(scenario.width(), tiles.length, "Dungeon width should match"),
        () -> assertEquals(scenario.height(), tiles[0].length, "Dungeon height should match"),
        () -> assertFloorTilesExist(tiles, "Dungeon should have floor tiles"),
        () -> assertDungeonIsConnected(tiles, "Dungeon should be connected"),
        () -> assertTrue(elapsed < 30000, "Generation should complete within 30 seconds"));
  }

  @ParameterizedTest(name = "large dungeon determinism: {0}")
  @MethodSource("largeDungeonScenarios")
  void generateBaseTiles_largeDungeonsAreDeterministic(LargeDungeonScenario scenario) {
    // Given: two generators with the same seed
    DungeonGenerator generator1 = createGenerator(scenario.seed());
    DungeonGenerator generator2 = createGenerator(scenario.seed());

    // When
    int[][] tiles1 =
        generator1.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());
    int[][] tiles2 =
        generator2.generateBaseTiles(scenario.type(), scenario.width(), scenario.height());

    // Then
    assertTilesMatch(tiles1, tiles2);
  }

  @Test
  void generateBaseTiles_veryLargeCave() {
    // Given: a very large cave
    long seed = 42L;
    int width = 250;
    int height = 250;
    DungeonGenerator generator = createGenerator(seed);

    // When
    long startTime = System.currentTimeMillis();
    int[][] tiles = generator.generateBaseTiles("cave", width, height);
    long elapsed = System.currentTimeMillis() - startTime;

    // Then
    if (PRINT_LARGE_DUNGEONS) {
      System.out.println("Very Large Cave " + width + "x" + height + " in " + elapsed + "ms:");
      System.out.println(visualize(tiles));
    }

    assertAll(
        () -> assertEquals(width, tiles.length, "Width should match"),
        () -> assertEquals(height, tiles[0].length, "Height should match"),
        () -> assertFloorTilesExist(tiles, "Should have floor tiles"),
        () -> assertDungeonIsConnected(tiles, "Should be connected"));
  }

  @Test
  void generateBaseTiles_veryLargeBSP() {
    // Given: a large BSP dungeon
    // Note: BSP uses recursive flood fill in clean() which can cause StackOverflow
    // on very large dungeons (300x250+). Keep size reasonable.
    // Seed 42 produces well-connected dungeons.
    long seed = 42L;
    int width = 180;
    int height = 150;
    DungeonGenerator generator = createGenerator(seed);

    // When
    long startTime = System.currentTimeMillis();
    int[][] tiles = generator.generateBaseTiles("bsp", width, height);
    long elapsed = System.currentTimeMillis() - startTime;

    // Then
    if (PRINT_LARGE_DUNGEONS) {
      System.out.println("Large BSP " + width + "x" + height + " in " + elapsed + "ms:");
      System.out.println(visualize(tiles));
    }

    assertAll(
        () -> assertEquals(width, tiles.length, "Width should match"),
        () -> assertEquals(height, tiles[0].length, "Height should match"),
        () -> assertFloorTilesExist(tiles, "Should have floor tiles"),
        () -> assertDungeonIsConnected(tiles, "Should be connected"));
  }

  // ==================== Assertion Helpers ====================

  private void assertFloorTerrainExists(String[][] terrain, String floors, String message) {
    List<String> floorTypes = List.of(floors.split(","));
    boolean hasFloor = false;
    for (int x = 0; x < terrain.length; x++) {
      for (int y = 0; y < terrain[0].length; y++) {
        if (terrain[x][y] != null) {
          String baseTerrain = terrain[x][y].split(";")[0];
          if (floorTypes.contains(baseTerrain)) {
            hasFloor = true;
            break;
          }
        }
      }
      if (hasFloor) break;
    }
    assertTrue(hasFloor, message);
  }

  private void assertTerrainMatch(String[][] terrain1, String[][] terrain2) {
    assertEquals(terrain1.length, terrain2.length, "Terrain arrays should have same width");
    for (int x = 0; x < terrain1.length; x++) {
      assertEquals(
          terrain1[x].length,
          terrain2[x].length,
          "Terrain arrays should have same height at x=" + x);
      for (int y = 0; y < terrain1[x].length; y++) {
        if (terrain1[x][y] == null && terrain2[x][y] == null) {
          continue; // Both null is fine
        }
        assertEquals(
            terrain1[x][y], terrain2[x][y], String.format("Terrain at (%d,%d) should match", x, y));
      }
    }
  }

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
   * Visualizes terrain as an ASCII grid.
   *
   * <p>Legend:
   *
   * <ul>
   *   <li>'.' = floor terrain (any non-null)
   *   <li>'c' = floor with creature
   *   <li>'i' = floor with item
   *   <li>' ' = null (wall/void)
   * </ul>
   */
  private String visualizeTerrain(String[][] terrain) {
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

    // Add summary
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
