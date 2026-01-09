package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import neon.maps.MapUtils;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TileAssertions}. */
class TileAssertionsTest {

  @Test
  void testAssertTilesMatch_identical() {
    int[][] tiles1 = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };
    int[][] tiles2 = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(() -> TileAssertions.assertTilesMatch(tiles1, tiles2));
  }

  @Test
  void testAssertTilesMatch_differentContent() {
    int[][] tiles1 = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };
    int[][] tiles2 = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertThrows(AssertionError.class, () -> TileAssertions.assertTilesMatch(tiles1, tiles2));
  }

  @Test
  void testAssertTilesMatch_differentWidth() {
    int[][] tiles1 = {{MapUtils.WALL}, {MapUtils.FLOOR}};
    int[][] tiles2 = {{MapUtils.WALL}};

    assertThrows(AssertionError.class, () -> TileAssertions.assertTilesMatch(tiles1, tiles2));
  }

  @Test
  void testAssertTerrainMatch_identical() {
    String[][] terrain1 = {
      {"grass", "stone"},
      {"dirt", null}
    };
    String[][] terrain2 = {
      {"grass", "stone"},
      {"dirt", null}
    };

    assertDoesNotThrow(() -> TileAssertions.assertTerrainMatch(terrain1, terrain2));
  }

  @Test
  void testAssertTerrainMatch_different() {
    String[][] terrain1 = {
      {"grass", "stone"},
      {"dirt", null}
    };
    String[][] terrain2 = {
      {"grass", "stone"},
      {"sand", null}
    };

    assertThrows(AssertionError.class, () -> TileAssertions.assertTerrainMatch(terrain1, terrain2));
  }

  @Test
  void testAssertFloorTilesExist_hasFloor() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(() -> TileAssertions.assertFloorTilesExist(tiles, "Should have floor"));
  }

  @Test
  void testAssertFloorTilesExist_noFloor() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class, () -> TileAssertions.assertFloorTilesExist(tiles, "Should fail"));
  }

  @Test
  void testAssertTileTypeExists_exists() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.CORRIDOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(
        () ->
            TileAssertions.assertTileTypeExists(tiles, MapUtils.CORRIDOR, "Should have corridor"));
  }

  @Test
  void testAssertTileTypeExists_notExists() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class,
        () -> TileAssertions.assertTileTypeExists(tiles, MapUtils.CORRIDOR, "Should fail"));
  }

  @Test
  void testAssertWalkableTilesExist_hasWalkable() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(
        () -> TileAssertions.assertWalkableTilesExist(tiles, "Should have walkable"));
  }

  @Test
  void testAssertWalkableTilesExist_noWalkable() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class, () -> TileAssertions.assertWalkableTilesExist(tiles, "Should fail"));
  }

  @Test
  void testAssertFloorTerrainExists_hasTerrain() {
    String[][] terrain = {
      {"wall", "wall"},
      {"grass", "wall"}
    };

    assertDoesNotThrow(
        () -> TileAssertions.assertFloorTerrainExists(terrain, "grass,dirt", "Should have grass"));
  }

  @Test
  void testAssertFloorTerrainExists_noTerrain() {
    String[][] terrain = {
      {"wall", "wall"},
      {"stone", "wall"}
    };

    assertThrows(
        AssertionError.class,
        () -> TileAssertions.assertFloorTerrainExists(terrain, "grass,dirt", "Should fail"));
  }

  @Test
  void testAssertFloorTerrainExists_withSemicolon() {
    String[][] terrain = {
      {"wall", "wall"},
      {"grass;variant=2", "wall"}
    };

    assertDoesNotThrow(
        () ->
            TileAssertions.assertFloorTerrainExists(
                terrain, "grass,dirt", "Should have grass (ignoring variant)"));
  }

  @Test
  void testAssertRoomWallsExist_hasRoomWalls() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL_ROOM},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(() -> TileAssertions.assertRoomWallsExist(tiles, "Should have room walls"));
  }

  @Test
  void testAssertRoomWallsExist_noRoomWalls() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class, () -> TileAssertions.assertRoomWallsExist(tiles, "Should fail"));
  }

  @Test
  void testAssertCornersExist_hasCorners() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.CORNER},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertDoesNotThrow(() -> TileAssertions.assertCornersExist(tiles, "Should have corners"));
  }

  @Test
  void testAssertCornersExist_noCorners() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class, () -> TileAssertions.assertCornersExist(tiles, "Should fail"));
  }

  @Test
  void testConstructor_throwsException() {
    try {
      var constructor = TileAssertions.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      constructor.newInstance();
      fail("Constructor should throw AssertionError");
    } catch (Exception e) {
      assertEquals(AssertionError.class, e.getCause().getClass());
      assertEquals("Utility class should not be instantiated", e.getCause().getMessage());
    }
  }
}
