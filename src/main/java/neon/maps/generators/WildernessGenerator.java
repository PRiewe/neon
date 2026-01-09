/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.maps.generators;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import neon.entities.*;
import neon.entities.property.Habitat;
import neon.maps.Decomposer;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.Zone;
import neon.maps.services.EntityStore;
import neon.maps.services.ResourceProvider;
import neon.resources.RItem;
import neon.resources.RRegionTheme;
import neon.resources.RTerrain;
import neon.util.Dice;

/**
 * Generates a piece of wilderness. The following types are supported:
 *
 * <ul>
 *   <li>plain
 *   <li>ridges
 *   <li>terraces
 *   <li>chaotic
 * </ul>
 *
 * @author mdriesen
 */
public class WildernessGenerator {
  private Zone zone;
  private String[][] terrain; // general terrain info
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;

  // random sources
  private final MapUtils mapUtils;
  private final Dice dice;

  // helper generators
  private final BlocksGenerator blocksGenerator;
  private final CaveGenerator caveGenerator;

  /**
   * Creates a wilderness generator with dependency injection for engine use.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   */
  public WildernessGenerator(
      Zone zone, EntityStore entityStore, ResourceProvider resourceProvider) {
    this(zone, entityStore, resourceProvider, new MapUtils(), new Dice());
  }

  /**
   * Creates a wilderness generator with dependency injection and custom random sources.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public WildernessGenerator(
      Zone zone,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      MapUtils mapUtils,
      Dice dice) {
    this.zone = zone;
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.mapUtils = mapUtils;
    this.dice = dice;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.caveGenerator = new CaveGenerator(dice);
  }

  /**
   * Creates a wilderness generator with dependency injection for editor use.
   *
   * @param terrain the terrain array
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   */
  public WildernessGenerator(
      String[][] terrain, EntityStore entityStore, ResourceProvider resourceProvider) {
    this(terrain, entityStore, resourceProvider, new MapUtils(), new Dice());
  }

  /**
   * Creates a wilderness generator with dependency injection for editor use and custom random
   * sources.
   *
   * @param terrain the terrain array
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public WildernessGenerator(
      String[][] terrain,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      MapUtils mapUtils,
      Dice dice) {
    this.terrain = terrain;
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.mapUtils = mapUtils;
    this.dice = dice;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.caveGenerator = new CaveGenerator(dice);
  }

  /** Generates a piece of wilderness using the supplied parameters. */
  public void generate(Region region, RRegionTheme theme) {
    // check if other regions are already on top of this region
    Collection<Region> regions = zone.getRegions(region.getBounds());
    if (!isOnTop(region, regions)) { // if there are still regions above this region
      decompose(region, regions, theme);
    } else if (region.getWidth() > 100
        || region.getHeight() > 100) { // check if region is not too large
      divide(region, theme);
    } else { // if small enough, generate region
      terrain = new String[region.getHeight() + 2][region.getWidth() + 2]; // [rows][columns]
      if (region.getY() > 0) { // top of map
        for (int i = 0; i < region.getWidth(); i++) {
          terrain[0][i + 1] =
              zone.getRegion(new Point(region.getX() + i, region.getY() - 1)).getTextureType();
        }
      }
      if (region.getY() + region.getHeight() < zone.getHeight() - 1) { // bottom
        for (int i = 0; i < region.getWidth(); i++) {
          terrain[region.getHeight() + 1][i + 1] =
              zone.getRegion(new Point(region.getX() + i, region.getY() + region.getHeight()))
                  .getTextureType();
        }
      }
      if (region.getX() > 0) { // left
        for (int i = 0; i < region.getHeight(); i++) {
          terrain[i + 1][0] =
              zone.getRegion(new Point(region.getX() - 1, region.getY() + i)).getTextureType();
        }
      }
      if (region.getX() + region.getWidth() < zone.getWidth() - 1) { // right
        for (int i = 0; i < region.getHeight(); i++) {
          terrain[i + 1][region.getWidth() + 1] =
              zone.getRegion(new Point(region.getX() + region.getWidth(), region.getY() + i))
                  .getTextureType();
        }
      }

      // generate terrain
      generateTerrain(region.getWidth(), region.getHeight(), theme, region.getTextureType());

      // add vegetation if needed
      addVegetation(region.getWidth(), region.getHeight(), theme, region.getTextureType());

      // add creatures
      addCreatures(
          region.getX(),
          region.getY(),
          region.getWidth(),
          region.getHeight(),
          theme,
          region.getTextureType());

      // convert all info in terrain to regions
      generateEngineContent(region);
    }
  }

  public String[][] generate(Rectangle r, RRegionTheme theme, String base) {
    // generate terrain
    generateTerrain(r.width, r.height, theme, base);

    // generate fauna
    addVegetation(r.width, r.height, theme, base);
    return terrain;
  }

  private boolean isOnTop(Region region, Collection<Region> regions) {
    for (Region r : regions) {
      if (r.getZ() > region.getZ()) {
        return false;
      }
    }
    return true;
  }

  private void decompose(Region region, Collection<Region> regions, RRegionTheme theme) {
    // cut region into pieces that don't overlap with overlying regions
    zone.removeRegion(region);
    Area area = new Area(region.getBounds());
    for (Region r : regions) {
      if (r.getZ() > region.getZ()) {
        area.subtract(new Area(r.getBounds()));
      }
    }

    int i = 0;
    while (i < 5 && !area.isEmpty()) {
      i++; // hope that the area is not too complicated
      Collection<Rectangle> pieces = Decomposer.split(area);
      for (Rectangle r : pieces) {
        if (area.contains(r)) {
          RTerrain rt = (RTerrain) resourceProvider.getResource(region.getTextureType(), "terrain");
          zone.addRegion(
              new Region(
                  region.getTextureType(), r.x, r.y, r.width, r.height, theme, region.getZ(), rt));
          area.subtract(new Area(r));
        }
      }
    }
  }

  private void divide(Region region, RRegionTheme theme) {
    String texture = region.getTextureType();

    // split into smaller non-fixed pieces of same size
    int newWidth =
        (region.getWidth() > region.getHeight()) ? region.getWidth() / 2 : region.getWidth();
    int newHeight =
        (region.getWidth() > region.getHeight()) ? region.getHeight() : region.getHeight() / 2;
    zone.removeRegion(region);

    RTerrain rt = (RTerrain) resourceProvider.getResource(texture, "terrain");
    zone.addRegion(
        new Region(
            texture, region.getX(), region.getY(), newWidth, newHeight, theme, region.getZ(), rt));

    int dw = region.getWidth() % 2;
    int dh = region.getHeight() % 2;

    if (region.getWidth() > region.getHeight()) {
      zone.addRegion(
          new Region(
              texture,
              region.getX() + newWidth,
              region.getY(),
              newWidth + dw,
              newHeight,
              theme,
              region.getZ(),
              rt));
    } else {
      zone.addRegion(
          new Region(
              texture,
              region.getX(),
              region.getY() + newHeight,
              newWidth,
              newHeight + dh,
              theme,
              region.getZ(),
              rt));
    }
  }

  private void generateTerrain(int width, int height, RRegionTheme theme, String base) {
    // create terrain and vegetation
    switch (theme.type) {
      case CHAOTIC:
        generateSwamp(width, height, theme);
        break;
      case PLAIN:
        generateForest(width, height, theme);
        break;
      case RIDGES:
        generateRidges(width, height, theme);
        break;
      case TERRACE:
        generateTerraces(width, height, theme);
        break;
      default:
        break;
    }

    // blend into neighboring region
    makeBorder(base);

    // add features
    addFeatures(width, height, theme);
  }

  private void addFeatures(int width, int height, RRegionTheme theme) {
    double ratio = (width * height) / 10000d;
    for (RRegionTheme.Feature feature : theme.features) {
      int n = (int) Float.parseFloat(feature.n) * 100;
      if (n > 100) {
        n = mapUtils.random(0, (int) (n * ratio / 100));
      } else {
        n = (mapUtils.random(0, (int) (n * ratio)) > 50) ? 1 : 0;
      }
      if (feature.value.equals("lake")) { // large patch that just overwrites everything
        int size = 100 / Integer.parseInt(feature.s);
        ArrayList<Rectangle> lakes =
            blocksGenerator.createSparseRectangles(
                width, height, width / size, height / size, 2, n);
        for (Rectangle r : lakes) {
          // place lake
          Polygon lake = mapUtils.randomPolygon(r, (r.width + r.height) / 2);
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              if (lake.contains(x, y)) {
                terrain[y + 1][x + 1] = feature.t;
              }
            }
          }
        }
      }
    }
  }

  private void addCreatures(
      int rx, int ry, int width, int height, RRegionTheme theme, String base) {
    double ratio = (double) width * height / 10000;
    for (String id : theme.creatures.keySet()) {
      for (int i = (int) (dice.rollDice(1, theme.creatures.get(id), 0) * ratio); i > 0; i--) {
        int x = dice.rollDice(1, width, -1);
        int y = dice.rollDice(1, height, -1);

        String t = terrain[y + 1][x + 1] == null ? base : terrain[y + 1][x + 1].split(";")[0];
        String region = t.isEmpty() ? base : t;

        Creature creature =
            EntityFactory.getCreature(id, rx + x, ry + y, entityStore.createNewEntityUID());
        RTerrain terrain = (RTerrain) resourceProvider.getResource(region, "terrain");
        // Only spawn creatures if their habitat matches the terrain
        // LAND creatures should NOT spawn in SWIM terrain
        boolean isWaterTerrain = terrain.modifier == Region.Modifier.SWIM;
        boolean isLandCreature = creature.species.habitat == Habitat.LAND;
        boolean canSpawn = !(isWaterTerrain && isLandCreature);

        if (canSpawn) {
          entityStore.addEntity(creature);
          zone.addCreature(creature);
        }
      }
    }
  }

  private void generateEngineContent(Region region) {
    // smaller pieces of terrain
    for (int i = 0; i < region.getWidth(); i++) {
      for (int j = 0; j < region.getHeight(); j++) {
        if (terrain[j + 1][i + 1] != null) {
          String[] data = terrain[j + 1][i + 1].split(";");
          for (String entry : data) {
            if (entry.startsWith("i:")) {
              String id = entry.replace("i:", "");
              long uid = entityStore.createNewEntityUID();
              Item item = EntityFactory.getItem(id, region.getX() + i, region.getY() + j, uid);
              entityStore.addEntity(item);
              if (item instanceof Container) {
                for (String s : ((RItem.Container) item.resource).contents) {
                  Item content = EntityFactory.getItem(s, entityStore.createNewEntityUID());
                  ((Container) item).addItem(content.getUID());
                  entityStore.addEntity(content);
                }
              }
              zone.addItem(item);
            } else if (entry.startsWith("c:")) {
              String id = entry.replace("c:", "");
              long uid = entityStore.createNewEntityUID();
              Creature creature =
                  EntityFactory.getCreature(id, region.getX() + i, region.getY() + j, uid);
              entityStore.addEntity(creature);
              zone.addCreature(creature);
            } else if (!entry.isEmpty() && !entry.equals(region.getTextureType())) {
              RTerrain terrain = (RTerrain) resourceProvider.getResource(entry, "terrain");
              zone.addRegion(
                  new Region(
                      entry,
                      region.getX() + i,
                      region.getY() + j,
                      1,
                      1,
                      null,
                      region.getZ() + 1,
                      terrain));
            }
          }
        }
      }
    }
  }

  protected void generateTerraces(int width, int height, RRegionTheme theme) {
    int[][] tiles = caveGenerator.generateOpenCave(width, height, 3);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (tiles[x][y] > 0) {
          terrain[y + 1][x + 1] = theme.floor;
        }
      }
    }
  }

  protected void generateForest(int width, int height, RRegionTheme theme) {}

  protected void generateRidges(int width, int height, RRegionTheme theme) {
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

  private void addVegetation(int width, int height, RRegionTheme theme, String base) {
    if (!theme.vegetation.isEmpty()) {
      String[][] fauna = new String[width][height];
      for (String id : theme.vegetation.keySet()) {
        int abundance = theme.vegetation.get(id);
        Item dummy = EntityFactory.getItem(id, 0);
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

  private void makeBorder(String type) {
    int width = terrain[0].length - 2;
    int height = terrain.length - 2;

    if (terrain[0][1] != null) { // top
      // overlap
      int h = 0;
      for (int i = 0; i < width; i++) {
        if (!terrain[0][i + 1].equals(type)) {
          if (h > 0) {
            addTerrain(i + 1, 1, 1, h, terrain[0][i + 1]);
          }

          double c = mapUtils.getRandomSource().nextDouble();
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
            addTerrain(i + 1, height - h + 1, 1, h, terrain[height + 1][i + 1]);
          }

          double c = mapUtils.getRandomSource().nextDouble();
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
            addTerrain(1, i + 1, w, 1, terrain[i + 1][0]);
          }

          double c = mapUtils.getRandomSource().nextDouble();
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
            addTerrain(width - w + 1, i + 1, w, 1, terrain[i + 1][width + 1]);
          }

          double c = mapUtils.getRandomSource().nextDouble();
          if (c > 0.7 && w < width / 10) {
            w++;
          } else if (c < 0.3 && w > 0) {
            w--;
          }
        }
      }
    }
  }

  private void addTerrain(int x, int y, int width, int height, String type) {
    for (int i = y; i < y + height; i++) {
      for (int j = x; j < x + width; j++) {
        terrain[i][j] = type;
      }
    }
  }

  // from http://www.evilscience.co.uk/?p=53
  boolean[][] generateIslands(int width, int height, int p, int n, int i) {
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

  protected void generateSwamp(int width, int height, RRegionTheme theme) {
    boolean[][] tiles = generateIslands(width, height, 20, 4, 5000);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (tiles[x][y]) {
          terrain[y + 1][x + 1] = theme.floor;
        }
      }
    }
  }
}
