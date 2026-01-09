package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.stream.Stream;
import neon.maps.MapUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for RoomGenerator with deterministic seeded random behavior. */
class RoomGeneratorTest {

  // ==================== Scenario Records ====================

  /**
   * Test scenario for room generation.
   *
   * @param seed random seed for deterministic behavior
   * @param roomX x position of room
   * @param roomY y position of room
   * @param roomWidth room width
   * @param roomHeight room height
   */
  record RoomScenario(long seed, int roomX, int roomY, int roomWidth, int roomHeight) {

    /** Total map width including margin for walls. */
    int mapWidth() {
      return roomX + roomWidth + 2;
    }

    /** Total map height including margin for walls. */
    int mapHeight() {
      return roomY + roomHeight + 2;
    }

    Rectangle toRectangle() {
      return new Rectangle(roomX, roomY, roomWidth, roomHeight);
    }

    @Override
    public String toString() {
      return String.format(
          "seed=%d, room@(%d,%d) %dx%d", seed, roomX, roomY, roomWidth, roomHeight);
    }
  }

  // ==================== Scenario Providers ====================

  static Stream<RoomScenario> roomScenarios() {
    return Stream.of(
        new RoomScenario(42L, 1, 1, 6, 4),
        new RoomScenario(264L, 2, 2, 8, 6),
        new RoomScenario(999L, 1, 1, 10, 8),
        new RoomScenario(123L, 3, 2, 5, 5));
  }

  static Stream<RoomScenario> polyRoomScenarios() {
    return Stream.of(
        new RoomScenario(42L, 1, 1, 8, 6),
        new RoomScenario(264L, 2, 2, 10, 8),
        new RoomScenario(999L, 1, 1, 12, 10),
        new RoomScenario(777L, 2, 2, 8, 8));
  }

  static Stream<RoomScenario> caveRoomScenarios() {
    return Stream.of(
        new RoomScenario(42L, 2, 2, 10, 8),
        new RoomScenario(264L, 2, 2, 12, 10),
        new RoomScenario(999L, 3, 3, 14, 12),
        new RoomScenario(123L, 2, 2, 10, 10));
  }

  // ==================== Simple Room Tests ====================

  @ParameterizedTest(name = "makeRoom: {0}")
  @MethodSource("roomScenarios")
  void makeRoom_generatesValidRoom(RoomScenario scenario) {
    // Given
    RoomGenerator generator = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    Rectangle roomBounds = scenario.toRectangle();

    // When
    RoomGenerator.Room room = generator.makeRoom(tiles, roomBounds);

    // Then: visualize
    System.out.println("makeRoom: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertNotNull(room, "Should return a Room"),
        () -> assertNotNull(room.getBounds(), "Room should have bounds"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Room should have floor tiles"),
        () -> assertRoomWallsExist(tiles, "Room should have walls"),
        () -> assertCornersExist(tiles, "Room should have corners"));
  }

  @ParameterizedTest(name = "makeRoom determinism: {0}")
  @MethodSource("roomScenarios")
  void makeRoom_isDeterministic(RoomScenario scenario) {
    // Given: two generators with the same seed
    RoomGenerator generator1 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    RoomGenerator generator2 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles1 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    int[][] tiles2 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());

    // When
    generator1.makeRoom(tiles1, scenario.toRectangle());
    generator2.makeRoom(tiles2, scenario.toRectangle());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Poly Room Tests ====================

  @ParameterizedTest(name = "makePolyRoom: {0}")
  @MethodSource("polyRoomScenarios")
  void makePolyRoom_generatesValidRoom(RoomScenario scenario) {
    // Given
    RoomGenerator generator = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    Rectangle roomBounds = scenario.toRectangle();

    // When
    RoomGenerator.Room room = generator.makePolyRoom(tiles, roomBounds);

    // Then: visualize
    System.out.println("makePolyRoom: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertNotNull(room, "Should return a Room"),
        () -> assertNotNull(room.getBounds(), "Room should have bounds"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Poly room should have floor tiles"));
  }

  @ParameterizedTest(name = "makePolyRoom determinism: {0}")
  @MethodSource("polyRoomScenarios")
  void makePolyRoom_isDeterministic(RoomScenario scenario) {
    // Given: two generators with the same seed
    RoomGenerator generator1 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    RoomGenerator generator2 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles1 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    int[][] tiles2 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());

    // When
    generator1.makePolyRoom(tiles1, scenario.toRectangle());
    generator2.makePolyRoom(tiles2, scenario.toRectangle());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Cave Room Tests ====================

  @ParameterizedTest(name = "makeCaveRoom: {0}")
  @MethodSource("caveRoomScenarios")
  void makeCaveRoom_generatesValidRoom(RoomScenario scenario) {
    // Given
    RoomGenerator generator = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    Rectangle roomBounds = scenario.toRectangle();

    // When
    RoomGenerator.Room room = generator.makeCaveRoom(tiles, roomBounds);

    // Then: visualize
    System.out.println("makeCaveRoom: " + scenario);
    System.out.println(TileVisualization.visualizeTiles(tiles));
    System.out.println();

    // Verify
    assertAll(
        () -> assertNotNull(room, "Should return a Room"),
        () -> assertNotNull(room.getBounds(), "Room should have bounds"),
        () -> TileAssertions.assertFloorTilesExist(tiles, "Cave room should have floor tiles"));
  }

  @ParameterizedTest(name = "makeCaveRoom determinism: {0}")
  @MethodSource("caveRoomScenarios")
  void makeCaveRoom_isDeterministic(RoomScenario scenario) {
    // Given: two generators with the same seed
    RoomGenerator generator1 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    RoomGenerator generator2 = new RoomGenerator(MapUtils.withSeed(scenario.seed()));
    int[][] tiles1 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());
    int[][] tiles2 = createTilesArray(scenario.mapWidth(), scenario.mapHeight());

    // When
    generator1.makeCaveRoom(tiles1, scenario.toRectangle());
    generator2.makeCaveRoom(tiles2, scenario.toRectangle());

    // Then
    TileAssertions.assertTilesMatch(tiles1, tiles2);
  }

  // ==================== Helper Methods ====================

  private int[][] createTilesArray(int width, int height) {
    int[][] tiles = new int[width][height];
    // Initialize all tiles as WALL (default is 0 which equals WALL)
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        tiles[x][y] = MapUtils.WALL;
      }
    }
    return tiles;
  }

  // ==================== Assertion Helpers ====================

  private void assertRoomWallsExist(int[][] tiles, String message) {
    boolean hasRoomWall = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (tiles[x][y] == MapUtils.WALL_ROOM) {
          hasRoomWall = true;
          break;
        }
      }
      if (hasRoomWall) break;
    }
    assertTrue(hasRoomWall, message);
  }

  private void assertCornersExist(int[][] tiles, String message) {
    boolean hasCorner = false;
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (tiles[x][y] == MapUtils.CORNER) {
          hasCorner = true;
          break;
        }
      }
      if (hasCorner) break;
    }
    assertTrue(hasCorner, message);
  }

  // ==================== Visualization ====================

}
