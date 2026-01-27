package neon.maps.generators;

import static neon.maps.generators.RoomGenerator.newExposed;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import neon.maps.MapUtils;
import neon.resources.RZoneTheme;
import neon.util.Dice;

public class DungeonTileGenerator {
  private final RZoneTheme theme;
  // helper generators
  private final BlocksGenerator blocksGenerator;
  private final ComplexGenerator complexGenerator;
  private final CaveGenerator caveGenerator;
  private final MazeGenerator mazeGenerator;
  private final FeatureGenerator featureGenerator;

  // random sources
  private final MapUtils mapUtils;
  private final Dice dice;

  public DungeonTileGenerator(RZoneTheme theme, MapUtils mapUtils, Dice dice) {
    this.theme = theme;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.complexGenerator = new ComplexGenerator(mapUtils);
    this.mapUtils = mapUtils;
    this.caveGenerator = new CaveGenerator(dice);
    this.mazeGenerator = new MazeGenerator(dice);
    this.featureGenerator = new FeatureGenerator(mapUtils);
    this.dice = dice;
  }

  public DungeonTileGenerator(RZoneTheme theme) {
    this(theme, new MapUtils(), new Dice());
  }

  public record DungeonLayout(int[][] tiles, String[][] terrain) {}

  /** Generates a single zone from a given theme. */
  public DungeonLayout generateTiles() {
    // width and height of dungeon
    int width = mapUtils.random(theme.min, theme.max);
    int height = mapUtils.random(theme.min, theme.max);

    // base terrain without features
    int[][] tiles = generateBaseTiles(theme.type, width, height);
    String[][] terrain = makeTerrain(tiles, theme.floor.split(","));

    // scale factor for generating features, creatures and items
    double ratio = (width * height) / Math.pow(MapUtils.average(theme.min, theme.max), 2);

    // features
    generateFeatures(theme.features, ratio, terrain, tiles);

    // creatures
    for (String creature : theme.creatures.keySet()) {
      for (int i = (int) (dice.rollDice("1d" + theme.creatures.get(creature)) * ratio);
          i > 0;
          i--) {
        Point p = new Point(0, 0);
        do {
          p.x = dice.rollDice(1, width, -1);
          p.y = dice.rollDice(1, height, -1);
        } while (tiles[p.x][p.y] != MapUtils.FLOOR);

        terrain[p.x][p.y] = terrain[p.x][p.y] + ";c:" + creature;
      }
    }

    // items
    for (String item : theme.items.keySet()) {
      for (int i = (int) (dice.rollDice("1d" + theme.items.get(item)) * ratio); i > 0; i--) {
        Point p = new Point(0, 0);
        do {
          p.x = dice.rollDice(1, width, -1);
          p.y = dice.rollDice(1, height, -1);
        } while (tiles[p.x][p.y] != MapUtils.FLOOR);

        terrain[p.x][p.y] = terrain[p.x][p.y] + ";i:" + item;
      }
    }
    DungeonLayout layout = new DungeonLayout(tiles, terrain);
    return layout;
  }

  /**
   * Generates base tiles for a dungeon of the given type. Package-private for testing.
   *
   * @param type dungeon type (cave, pits, maze, mine, bsp, packed, or default/sparse)
   * @param width dungeon width
   * @param height dungeon height
   * @return 2D array of tile types
   */
  int[][] generateBaseTiles(String type, int width, int height) {
    int[][] tiles = new int[width][height];
    switch (type) {
      case "cave":
        tiles = makeTiles(mazeGenerator.generateSquashedMaze(width, height, 3), width, height);
        break;
      case "pits":
        tiles = caveGenerator.generateOpenCave(width, height, 3);
        break;
      case "maze":
        tiles = makeTiles(mazeGenerator.generateMaze(width, height, 3, 50), width, height);
        break;
      case "mine":
        Area mine = mazeGenerator.generateSquashedMaze(width, height, 12);
        mine.add(mazeGenerator.generateMaze(width, height, 12, 40));
        tiles = makeTiles(mine, width, height);
        break;
      case "bsp":
        tiles = complexGenerator.generateBSPDungeon(width, height, 5, 8);
        break;
      case "packed":
        tiles = complexGenerator.generatePackedDungeon(width, height, 10, 4, 7);
        break;
      default:
        tiles = complexGenerator.generateSparseDungeon(width, height, 5, 5, 15);
        break;
    }

    return tiles;
  }

  private void generateFeatures(
      Collection<Object[]> features, double ratio, String[][] terrain, int[][] tiles) {
    int width = terrain.length;
    int height = terrain[0].length;
    for (Object[] feature : features) {
      int s = (int) (feature[2]);
      String t = feature[0].toString();
      int n = (int) feature[3] * 100;
      if (n > 100) {
        n = mapUtils.random(0, (int) (n * ratio / 100));
      } else {
        n = (mapUtils.random(0, (int) (n * ratio)) > 50) ? 1 : 0;
      }

      if (feature[1].equals("lake")) { // large patch that just overwrites everything
        int size = 100 / s;
        ArrayList<Rectangle> lakes =
            blocksGenerator.createSparseRectangles(
                width, height, width / size, height / size, 2, n);
        for (Rectangle r : lakes) { // place lake
          featureGenerator.generateLake(terrain, t, r);
        }
      } else if (feature[1].equals("patch")) { // patch that only overwrites floor tiles
        // place patches
        ArrayList<Rectangle> patches =
            blocksGenerator.createSparseRectangles(width, height, s, s, 2, n);
        for (Rectangle r : patches) {
          Polygon patch = mapUtils.randomPolygon(r, 16);
          for (int x = r.x; x < r.x + r.width; x++) {
            for (int y = r.y; y < r.y + r.height; y++) {
              if (patch.contains(x, y) && tiles[x][y] == MapUtils.FLOOR) {
                terrain[x][y] = t;
              }
            }
          }
        }
      } else if (feature[1].equals("chunk")) { // patch that only overwrites wall tiles
        ArrayList<Rectangle> chunks =
            blocksGenerator.createSparseRectangles(width, height, s, s, 2, n);
        for (Rectangle chunk : chunks) {
          for (int x = chunk.x; x < chunk.x + chunk.width; x++) {
            for (int y = chunk.y; y < chunk.y + chunk.height; y++) {
              if (tiles[x][y] == MapUtils.WALL
                  || tiles[x][y] == MapUtils.WALL_ROOM
                  || tiles[x][y] == MapUtils.CORNER
                  || tiles[x][y] == MapUtils.ENTRY) {
                terrain[x][y] = t;
              }
            }
          }
        }
      } else if (feature[1].equals("stain")) { // patch that only overwrites exposed wall tiles
        ArrayList<Rectangle> stains =
            blocksGenerator.createSparseRectangles(width, height, s, s, 2, n);
        for (Rectangle stain : stains) {
          for (int x = stain.x; x < stain.x + stain.width; x++) {
            for (int y = stain.y; y < stain.y + stain.height; y++) {
              if ((tiles[x][y] == MapUtils.WALL
                      || tiles[x][y] == MapUtils.WALL_ROOM
                      || tiles[x][y] == MapUtils.CORNER
                      || tiles[x][y] == MapUtils.ENTRY)
                  && exposed(tiles, x, y)) {
                terrain[x][y] = t;
              }
            }
          }
        }
      } else if (feature[1].equals("river")) {
        while (n-- > 0) { // apparently first >, then --
          featureGenerator.generateRiver(terrain, tiles, t, s);
        }
      }
    }
  }

  private static boolean exposed(int[][] tiles, int x, int y) {
    return newExposed(tiles, x, y);
  }

  private static int[][] makeTiles(Area area, int width, int height) {
    int[][] tiles = new int[width][height];
    for (int j = 0; j < height; j++) {
      for (int i = 0; i < width; i++) {
        if (area.contains(i, j)) {
          tiles[i][j] = MapUtils.FLOOR;
        } else {
          tiles[i][j] = MapUtils.WALL;
        }
      }
    }
    return tiles;
  }

  private String[][] makeTerrain(int[][] tiles, String[] floors) {
    String[][] terrain = new String[tiles.length][tiles[0].length];

    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[0].length; y++) {
        int f = mapUtils.random(0, floors.length - 1);

        switch (tiles[x][y]) {
          case MapUtils.CORRIDOR:
          case MapUtils.FLOOR:
          case MapUtils.DOOR:
          case MapUtils.DOOR_CLOSED:
          case MapUtils.DOOR_LOCKED:
            terrain[x][y] = floors[f];
            break;
        }
      }
    }

    return terrain;
  }
}
