package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import neon.maps.MapUtils;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TileConnectivityAssertions}. */
class TileConnectivityAssertionsTest {

  @Test
  void testIsWalkable_floor() {
    assertTrue(TileConnectivityAssertions.isWalkable(MapUtils.FLOOR));
  }

  @Test
  void testIsWalkable_corridor() {
    assertTrue(TileConnectivityAssertions.isWalkable(MapUtils.CORRIDOR));
  }

  @Test
  void testIsWalkable_door() {
    assertTrue(TileConnectivityAssertions.isWalkable(MapUtils.DOOR));
  }

  @Test
  void testIsWalkable_doorClosed() {
    assertTrue(TileConnectivityAssertions.isWalkable(MapUtils.DOOR_CLOSED));
  }

  @Test
  void testIsWalkable_doorLocked() {
    assertTrue(TileConnectivityAssertions.isWalkable(MapUtils.DOOR_LOCKED));
  }

  @Test
  void testIsWalkable_wall() {
    assertFalse(TileConnectivityAssertions.isWalkable(MapUtils.WALL));
  }

  @Test
  void testIsWalkable_wallRoom() {
    assertFalse(TileConnectivityAssertions.isWalkable(MapUtils.WALL_ROOM));
  }

  @Test
  void testFindFirstWalkableTile_found() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    int[] result = TileConnectivityAssertions.findFirstWalkableTile(tiles);
    assertNotNull(result);
    assertEquals(1, result[0]);
    assertEquals(1, result[1]);
  }

  @Test
  void testFindFirstWalkableTile_notFound() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL}
    };

    int[] result = TileConnectivityAssertions.findFirstWalkableTile(tiles);
    assertNull(result);
  }

  @Test
  void testCountReachableTiles_singleTile() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    int count = TileConnectivityAssertions.countReachableTiles(tiles, 1, 1);
    assertEquals(1, count);
  }

  @Test
  void testCountReachableTiles_connectedArea() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    int count = TileConnectivityAssertions.countReachableTiles(tiles, 1, 1);
    assertEquals(4, count);
  }

  @Test
  void testCountReachableTiles_partiallyConnected() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    // Starting from connected area should only count 2 tiles
    int count = TileConnectivityAssertions.countReachableTiles(tiles, 1, 1);
    assertEquals(2, count);
  }

  @Test
  void testCountReachableTiles_withCorridors() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.CORRIDOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    int count = TileConnectivityAssertions.countReachableTiles(tiles, 1, 1);
    assertEquals(3, count);
  }

  @Test
  void testCountReachableTiles_withDoors() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.DOOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    int count = TileConnectivityAssertions.countReachableTiles(tiles, 1, 1);
    assertEquals(3, count);
  }

  @Test
  void testAssertFullyConnected_success() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    assertDoesNotThrow(
        () ->
            TileConnectivityAssertions.assertFullyConnected(
                tiles, "Connected dungeon should pass"));
  }

  @Test
  void testAssertFullyConnected_failure() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class,
        () ->
            TileConnectivityAssertions.assertFullyConnected(
                tiles, "Disconnected dungeon should fail"));
  }

  @Test
  void testAssertFullyConnected_emptyArray() {
    int[][] tiles = new int[0][0];

    assertThrows(
        AssertionError.class,
        () -> TileConnectivityAssertions.assertFullyConnected(tiles, "Empty array should fail"));
  }

  @Test
  void testAssertFullyConnected_nullArray() {
    assertThrows(
        AssertionError.class,
        () -> TileConnectivityAssertions.assertFullyConnected(null, "Null array should fail"));
  }

  @Test
  void testAssertFullyConnected_noWalkableTiles() {
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL}
    };

    assertThrows(
        AssertionError.class,
        () ->
            TileConnectivityAssertions.assertFullyConnected(
                tiles, "No walkable tiles should fail"));
  }

  @Test
  void testConstructor_throwsException() {
    try {
      // Use reflection to invoke private constructor
      var constructor = TileConnectivityAssertions.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      constructor.newInstance();
      fail("Constructor should throw AssertionError");
    } catch (Exception e) {
      // Reflection wraps the exception in InvocationTargetException
      assertEquals(AssertionError.class, e.getCause().getClass());
      assertEquals("Utility class should not be instantiated", e.getCause().getMessage());
    }
  }
}
