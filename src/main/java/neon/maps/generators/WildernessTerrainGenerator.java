package neon.maps.generators;

import java.awt.*;
import java.util.ArrayList;
import neon.core.UIStorage;
import neon.entities.Item;
import neon.entities.ItemFactory;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.services.ResourceProvider;
import neon.resources.RRegionTheme;
import neon.resources.RTerrain;
import neon.util.Dice;
import org.jdom2.Element;

public class WildernessTerrainGenerator {

  private final MapUtils mapUtils;
  private final Dice dice;
  private final ResourceProvider resourceProvider;
  // helper generators
  private final BlocksGenerator blocksGenerator;
  private final CaveGenerator caveGenerator;
  private final ItemFactory itemFactory;

  public WildernessTerrainGenerator(UIStorage dataStore) {
    this(new MapUtils(), new Dice(), dataStore);
  }

  public WildernessTerrainGenerator(MapUtils mapUtils, Dice dice, UIStorage dataStore) {
    this.mapUtils = mapUtils;
    this.dice = dice;
    this.resourceProvider = dataStore.getResources();
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.caveGenerator = new CaveGenerator(dice);
    this.itemFactory = new ItemFactory(dataStore.getResourceManageer());
  }

  public String[][] generate(Rectangle r, RRegionTheme theme, String base) {
    String[][] terrain = new String[r.width + 2][r.height + 2];
    return generate(r, theme, base, terrain);
  }

  public String[][] generate(Rectangle r, RRegionTheme theme, String base, String[][] terrain) {
    // generate terrain
    generateTerrain(r.width, r.height, theme, base, terrain);

    // generate fauna
    addVegetation(r.width, r.height, theme, base, terrain);
    return terrain;
  }

  public void generateTerrain(
      int width, int height, RRegionTheme theme, String base, String[][] terrain) {
    // create terrain and vegetation
    switch (theme.type) {
      case CHAOTIC -> generateSwamp(width, height, theme, terrain);
      case PLAIN -> generateForest(width, height, theme, terrain);
      case RIDGES -> generateRidges(width, height, theme, terrain);
      case TERRACE -> generateTerraces(width, height, theme, terrain);
      default -> {}
    }

    // blend into neighboring region
    makeBorder(base, terrain);

    // add features
    addFeatures(width, height, theme, terrain);
  }

  private void addFeatures(int width, int height, RRegionTheme theme, String[][] terrain) {
    double ratio = (width * height) / 10000d;
    for (Element feature : theme.features) {
      int n = (int) Float.parseFloat(feature.getAttributeValue("n")) * 100;
      if (n > 100) {
        n = mapUtils.random(0, (int) (n * ratio / 100));
      } else {
        n = (mapUtils.random(0, (int) (n * ratio)) > 50) ? 1 : 0;
      }
      if (feature.getText().equals("lake")) { // large patch that just overwrites everything
        int size = 100 / Integer.parseInt(feature.getAttributeValue("s"));
        ArrayList<Rectangle> lakes =
            blocksGenerator.createSparseRectangles(
                width, height, width / size, height / size, 2, n);
        for (Rectangle r : lakes) {
          // place lake
          Polygon lake = mapUtils.randomPolygon(r, (r.width + r.height) / 2);
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              if (lake.contains(x, y)) {
                terrain[y + 1][x + 1] = feature.getAttributeValue("t");
              }
            }
          }
        }
      }
    }
  }

  private void addTerrain(int x, int y, int width, int height, String type, String[][] terrain) {
    for (int i = y; i < y + height; i++) {
      for (int j = x; j < x + width; j++) {
        terrain[i][j] = type;
      }
    }
  }

  // from http://www.evilscience.co.uk/?p=53
  private boolean[][] generateIslands(int width, int height, int p, int n, int i) {
    boolean[][] map = new boolean[width][height];

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // p: initial chance that a cell contains something
        map[x][y] = (mapUtils.random(0, 100) < p);
      }
    }

    // iterate i times
    for (; i > 0; i--) {
      int x = mapUtils.random(0, width - 1);
      int y = mapUtils.random(0, height - 1);
      // approximately Conway's game of life with n neighbors
      map[x][y] = (filledNeighbours(x, y, map) > n);
    }

    return map;
  }

  private int filledNeighbours(int x, int y, boolean[][] map) {
    int c = 0;
    for (int i = Math.max(0, x - 1); i < Math.min(x + 2, map.length); i++) {
      for (int j = Math.max(0, y - 1); j < Math.min(y + 2, map[0].length); j++) {
        if (map[i][j]) {
          c++;
        }
      }
    }
    return c;
  }

  protected void generateSwamp(int width, int height, RRegionTheme theme, String[][] terrain) {
    boolean[][] tiles = generateIslands(width, height, 20, 4, 5000);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (tiles[x][y]) {
          terrain[y + 1][x + 1] = theme.floor;
        }
      }
    }
  }

  protected void generateTerraces(int width, int height, RRegionTheme theme, String[][] terrain) {
    int[][] tiles = caveGenerator.generateOpenCave(width, height, 3);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (tiles[x][y] > 0) {
          terrain[y + 1][x + 1] = theme.floor;
        }
      }
    }
  }

  protected void generateForest(int width, int height, RRegionTheme theme, String[][] terrain) {}

  protected void generateRidges(int width, int height, RRegionTheme theme, String[][] terrain) {
    // TODO: Replace with proper noise generation (AnimalStripe from jtexgen not available)
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Simple placeholder - generates ridges based on coordinate pattern
        if ((x + y * 3) % 10 < 5) {
          terrain[y + 1][x + 1] = theme.floor;
        }
      }
    }
  }

  public void addVegetation(
      int width, int height, RRegionTheme theme, String base, String[][] terrain) {
    if (!theme.vegetation.isEmpty()) {
      String[][] fauna = new String[width][height];
      for (String id : theme.vegetation.keySet()) {
        int abundance = theme.vegetation.get(id);
        Item dummy = itemFactory.getItem(id, 0);
        int size = dummy.getShapeComponent().width; // size van boom in rekening brengen
        int ratio = (width / size) * (height / size);
        boolean[][] fill = generateIslands(width / size, height / size, abundance, 5, ratio / size);
        for (int i = 0; i < fill.length; i++) {
          for (int j = 0; j < fill[0].length; j++) {
            if (fill[i][j]) {
              fauna[i * size + mapUtils.random(0, size - 1)][
                      j * size + mapUtils.random(0, size - 1)] =
                  id;
            }
          }
        }
      }

      for (int i = 0; i < fauna.length; i++) {
        for (int j = 0; j < fauna[i].length; j++) {
          String region = terrain[j + 1][i + 1] == null ? base : terrain[j + 1][i + 1];
          RTerrain rt = (RTerrain) resourceProvider.getResource(region, "terrain");
          if (fauna[i][j] != null && rt.modifier != Region.Modifier.SWIM) {
            String t = (terrain[j + 1][i + 1] != null ? terrain[j + 1][i + 1] : "");
            terrain[j + 1][i + 1] = t + ";i:" + fauna[i][j];
          }
        }
      }
    }
  }

  private void makeBorder(String type, String[][] terrain) {
    int width = terrain[0].length - 2;
    int height = terrain.length - 2;

    if (terrain[0][1] != null) { // top
      // overlap
      int h = 0;
      for (int i = 0; i < width; i++) {
        if (!terrain[0][i + 1].equals(type)) {
          if (h > 0) {
            addTerrain(i + 1, 1, 1, h, terrain[0][i + 1], terrain);
          }

          double c = mapUtils.randomSource().nextDouble();
          if (c > 0.7 && h < height / 10) {
            h++;
          } else if (c < 0.3 && h > 0) {
            h--;
          }
        }
      }
    }

    if (terrain[height + 1][1] != null) { // bottom
      // overlap
      int h = 0;
      for (int i = 0; i < width; i++) {
        if (!terrain[height + 1][i + 1].equals(type)) {
          if (h > 0) {
            addTerrain(i + 1, height - h + 1, 1, h, terrain[height + 1][i + 1], terrain);
          }

          double c = mapUtils.randomSource().nextDouble();
          if (c > 0.7 && h < height / 10) {
            h++;
          } else if (c < 0.3 && h > 0) {
            h--;
          }
        }
      }
    }

    if (terrain[1][0] != null) { // left
      // overlap
      int w = 0;
      for (int i = 0; i < height; i++) {
        if (!terrain[i + 1][0].equals(type)) {
          if (w > 0) {
            addTerrain(1, i + 1, w, 1, terrain[i + 1][0], terrain);
          }

          double c = mapUtils.randomSource().nextDouble();
          if (c > 0.7 && w < width / 10) {
            w++;
          } else if (c < 0.3 && w > 0) {
            w--;
          }
        }
      }
    }

    if (terrain[1][width + 1] != null) { // right
      // overlap
      int w = 0;
      for (int i = 0; i < height; i++) {
        if (!type.equals(terrain[i][width + 1])) {
          if (w > 0) {
            addTerrain(width - w + 1, i + 1, w, 1, terrain[i + 1][width + 1], terrain);
          }

          double c = mapUtils.randomSource().nextDouble();
          if (c > 0.7 && w < width / 10) {
            w++;
          } else if (c < 0.3 && w > 0) {
            w--;
          }
        }
      }
    }
  }
}
