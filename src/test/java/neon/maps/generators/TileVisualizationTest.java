package neon.maps.generators;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.List;
import neon.maps.MapUtils;
import org.junit.jupiter.api.Test;

/** Unit tests for TileVisualization utility class. */
class TileVisualizationTest {

  @Test
  void visualizeTiles_withDefaultMapper_createsValidOutput() {
    // Given
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.FLOOR, MapUtils.WALL},
      {MapUtils.WALL, MapUtils.WALL, MapUtils.WALL}
    };

    // When
    String result = TileVisualization.visualizeTiles(tiles);

    // Then
    assertNotNull(result);
    assertTrue(result.contains("###"));
    assertTrue(result.contains("#.#"));
    assertTrue(result.contains("+---+"));
    assertTrue(result.contains("floor=1"));
    assertTrue(result.contains("wall=8"));
  }

  @Test
  void visualizeTiles_withCustomMapper_usesCustomMapping() {
    // Given
    int[][] tiles = {{1, 2}, {3, 4}};
    TileVisualization.TileCharMapper customMapper = tile -> (char) ('A' + tile);

    // When
    String result = TileVisualization.visualizeTiles(tiles, customMapper);

    // Then
    assertTrue(result.contains("BD")); // First column: 1->B, 3->D
    assertTrue(result.contains("CE")); // Second column: 2->C, 4->E
  }

  @Test
  void visualizeTiles_withEmptyArray_returnsEmptyIndicator() {
    // Given
    int[][] tiles = {};

    // When
    String result = TileVisualization.visualizeTiles(tiles);

    // Then
    assertEquals("+empty+", result);
  }

  @Test
  void visualizeTiles_withNullArray_returnsEmptyIndicator() {
    // Given
    int[][] tiles = null;

    // When
    String result = TileVisualization.visualizeTiles(tiles);

    // Then
    assertEquals("+empty+", result);
  }

  @Test
  void visualizeTiles_withVariousTileTypes_showsCorrectCharacters() {
    // Given: tiles[width][height] - 9 columns, 1 row
    int[][] tiles = new int[9][1];
    tiles[0][0] = MapUtils.WALL;
    tiles[1][0] = MapUtils.FLOOR;
    tiles[2][0] = MapUtils.CORRIDOR;
    tiles[3][0] = MapUtils.WALL_ROOM;
    tiles[4][0] = MapUtils.CORNER;
    tiles[5][0] = MapUtils.DOOR;
    tiles[6][0] = MapUtils.DOOR_CLOSED;
    tiles[7][0] = MapUtils.DOOR_LOCKED;
    tiles[8][0] = MapUtils.ENTRY;

    // When
    String result = TileVisualization.visualizeTiles(tiles);

    // Then
    assertTrue(result.contains("|#.~W+DdLE|"), "Should contain: " + result);
  }

  @Test
  void defaultTileChar_mapsAllKnownTypes() {
    // Test all MapUtils tile constants
    assertEquals('#', TileVisualization.defaultTileChar(MapUtils.WALL));
    assertEquals('.', TileVisualization.defaultTileChar(MapUtils.FLOOR));
    assertEquals('~', TileVisualization.defaultTileChar(MapUtils.CORRIDOR));
    assertEquals('W', TileVisualization.defaultTileChar(MapUtils.WALL_ROOM));
    assertEquals('+', TileVisualization.defaultTileChar(MapUtils.CORNER));
    assertEquals('D', TileVisualization.defaultTileChar(MapUtils.DOOR));
    assertEquals('d', TileVisualization.defaultTileChar(MapUtils.DOOR_CLOSED));
    assertEquals('L', TileVisualization.defaultTileChar(MapUtils.DOOR_LOCKED));
    assertEquals('E', TileVisualization.defaultTileChar(MapUtils.ENTRY));
  }

  @Test
  void defaultTileChar_withUnknownType_returnsQuestionMark() {
    assertEquals('?', TileVisualization.defaultTileChar(99));
    assertEquals('?', TileVisualization.defaultTileChar(-1));
  }

  // ==================== Terrain Visualization Tests ====================

  @Test
  void visualizeTerrain_withBasicTerrain_createsValidOutput() {
    // Given
    String[][] terrain = {
      {"grass", "grass", "grass"},
      {"grass", "grass", "grass"},
      {"grass", "grass", "grass"}
    };

    // When
    String result = TileVisualization.visualizeTerrain(terrain);

    // Then
    assertNotNull(result);
    assertTrue(result.contains("..."));
    assertTrue(result.contains("+---+"));
    assertTrue(result.contains("floors=9"));
    assertTrue(result.contains("creatures=0"));
    assertTrue(result.contains("items=0"));
  }

  @Test
  void visualizeTerrain_withCreatureAnnotations_showsCreatures() {
    // Given: terrain[width][height] - 3x3 grid
    String[][] terrain = new String[3][3];
    terrain[0][0] = "grass";
    terrain[1][0] = "grass;c:wolf";
    terrain[2][0] = "grass";
    terrain[0][1] = "grass";
    terrain[1][1] = "grass";
    terrain[2][1] = "grass;c:bear";
    terrain[0][2] = "grass";
    terrain[1][2] = "grass";
    terrain[2][2] = "grass";

    // When
    String result = TileVisualization.visualizeTerrain(terrain);

    // Then
    assertTrue(result.contains(".c."), "Row 0 should have creature at column 1: " + result);
    assertTrue(result.contains("..c"), "Row 1 should have creature at column 2: " + result);
    assertTrue(result.contains("creatures=2"));
  }

  @Test
  void visualizeTerrain_withItemAnnotations_showsItems() {
    // Given
    String[][] terrain = {
      {"grass;i:tree", "grass", "grass"},
      {"grass", "grass;i:rock", "grass"},
      {"grass", "grass", "grass;i:flower"}
    };

    // When
    String result = TileVisualization.visualizeTerrain(terrain);

    // Then
    assertTrue(result.contains("i.."));
    assertTrue(result.contains(".i."));
    assertTrue(result.contains("..i"));
    assertTrue(result.contains("items=3"));
  }

  @Test
  void visualizeTerrain_withNullCells_showsSpaces() {
    // Given: terrain[width][height] - 3x3 grid with nulls
    String[][] terrain = new String[3][3];
    terrain[0][0] = null;
    terrain[1][0] = "grass";
    terrain[2][0] = null;
    terrain[0][1] = "grass";
    terrain[1][1] = null;
    terrain[2][1] = "grass";
    terrain[0][2] = null;
    terrain[1][2] = "grass";
    terrain[2][2] = null;

    // When
    String result = TileVisualization.visualizeTerrain(terrain);

    // Then
    assertTrue(result.contains("| . |"), "Row 0 should be: | . |, got: " + result);
    assertTrue(result.contains("|. .|"), "Row 1 should be: |. .|, got: " + result);
    assertTrue(result.contains("floors=4")); // Only non-null cells
  }

  @Test
  void visualizeTerrain_withEmptyArray_returnsEmptyIndicator() {
    // Given
    String[][] terrain = {};

    // When
    String result = TileVisualization.visualizeTerrain(terrain);

    // Then
    assertEquals("+empty+", result);
  }

  // ==================== Grid Visualization Tests ====================

  @Test
  void visualizeGrid_withBooleanArray_createsValidOutput() {
    // Given
    boolean[][] grid = {
      {true, false, true},
      {false, true, false},
      {true, false, true}
    };

    // When
    String result = TileVisualization.visualizeGrid(grid);

    // Then
    assertTrue(result.contains("# #"));
    assertTrue(result.contains(" # "));
    assertTrue(result.contains("+---+"));
  }

  @Test
  void visualizeGrid_withAllTrue_showsAllFilled() {
    // Given
    boolean[][] grid = {{true, true}, {true, true}};

    // When
    String result = TileVisualization.visualizeGrid(grid);

    // Then
    assertTrue(result.contains("|##|"));
  }

  @Test
  void visualizeGrid_withAllFalse_showsAllEmpty() {
    // Given
    boolean[][] grid = {{false, false}, {false, false}};

    // When
    String result = TileVisualization.visualizeGrid(grid);

    // Then
    assertTrue(result.contains("|  |"));
  }

  @Test
  void visualizeGrid_withEmptyArray_returnsEmptyIndicator() {
    // Given
    boolean[][] grid = {};

    // When
    String result = TileVisualization.visualizeGrid(grid);

    // Then
    assertEquals("+empty+", result);
  }

  // ==================== Rectangle Visualization Tests ====================

  @Test
  void visualizeRectangles_withSingleRectangle_showsCorrectly() {
    // Given
    List<Rectangle> rectangles = List.of(new Rectangle(1, 1, 2, 2));

    // When
    String result = TileVisualization.visualizeRectangles(rectangles, 4, 4);

    // Then
    assertTrue(result.contains("|    |")); // Row 0: empty
    assertTrue(result.contains("| ## |")); // Row 1: filled at x=1,2
    assertTrue(result.contains("count=1"));
  }

  @Test
  void visualizeRectangles_withOverlappingRectangles_mergesCorrectly() {
    // Given
    List<Rectangle> rectangles = List.of(new Rectangle(0, 0, 2, 2), new Rectangle(1, 1, 2, 2));

    // When
    String result = TileVisualization.visualizeRectangles(rectangles, 3, 3);

    // Then
    assertTrue(result.contains("count=2"));
    // Should show merged overlap
  }

  @Test
  void visualizeRectangles_withEmptyList_showsEmptyGrid() {
    // Given
    List<Rectangle> rectangles = List.of();

    // When
    String result = TileVisualization.visualizeRectangles(rectangles, 3, 3);

    // Then
    assertTrue(result.contains("|   |"));
    assertTrue(result.contains("count=0"));
  }

  @Test
  void visualizeRectangles_withNullList_returnsEmptyIndicator() {
    // When
    String result = TileVisualization.visualizeRectangles(null, 5, 5);

    // Then
    assertEquals("+empty+", result);
  }

  @Test
  void visualizeRectangles_withOutOfBoundsRectangle_clipsCorrectly() {
    // Given: rectangle extends beyond grid
    List<Rectangle> rectangles = List.of(new Rectangle(-1, -1, 4, 4));

    // When
    String result = TileVisualization.visualizeRectangles(rectangles, 2, 2);

    // Then
    assertNotNull(result);
    assertTrue(result.contains("+--+"));
    // Should only show clipped portion
  }

  // ==================== Counting Tests ====================

  @Test
  void countTiles_withMixedTiles_countsCorrectly() {
    // Given
    int[][] tiles = {
      {MapUtils.WALL, MapUtils.WALL, MapUtils.FLOOR},
      {MapUtils.FLOOR, MapUtils.FLOOR, MapUtils.CORRIDOR},
      {MapUtils.DOOR, MapUtils.WALL, MapUtils.WALL}
    };

    // When
    int[] counts = TileVisualization.countTiles(tiles);

    // Then
    assertEquals(4, counts[MapUtils.WALL]);
    assertEquals(3, counts[MapUtils.FLOOR]);
    assertEquals(1, counts[MapUtils.CORRIDOR]);
    assertEquals(1, counts[MapUtils.DOOR]);
  }

  @Test
  void countTiles_withInvalidTiles_ignoresOutOfBounds() {
    // Given: tiles with values outside 0-15 range
    int[][] tiles = {{-1, 0, 1}, {15, 16, 99}};

    // When
    int[] counts = TileVisualization.countTiles(tiles);

    // Then
    assertEquals(16, counts.length);
    assertEquals(1, counts[0]); // Only valid tile at index 0
    assertEquals(1, counts[1]); // Only valid tile at index 1
    assertEquals(1, counts[15]); // Only valid tile at index 15
    // -1, 16, 99 should be ignored
  }

  // ==================== Format Counts Tests ====================

  @Test
  void formatCounts_withTypicalCounts_formatsCorrectly() {
    // Given
    int[] counts = new int[16];
    counts[MapUtils.FLOOR] = 100;
    counts[MapUtils.CORRIDOR] = 20;
    counts[MapUtils.WALL] = 80;
    counts[MapUtils.WALL_ROOM] = 40;
    counts[MapUtils.DOOR] = 5;
    counts[MapUtils.DOOR_CLOSED] = 3;
    counts[MapUtils.DOOR_LOCKED] = 2;

    // When
    String result = TileVisualization.formatCounts(counts);

    // Then
    assertTrue(result.contains("floor=100"));
    assertTrue(result.contains("corridor=20"));
    assertTrue(result.contains("wall=80"));
    assertTrue(result.contains("room_wall=40"));
    assertTrue(result.contains("doors=10")); // 5 + 3 + 2
  }

  @Test
  void formatCounts_withZeros_showsZeros() {
    // Given
    int[] counts = new int[16]; // All zeros

    // When
    String result = TileVisualization.formatCounts(counts);

    // Then
    assertTrue(result.contains("floor=0"));
    assertTrue(result.contains("corridor=0"));
    assertTrue(result.contains("wall=0"));
    assertTrue(result.contains("room_wall=0"));
    assertTrue(result.contains("doors=0"));
  }
}
