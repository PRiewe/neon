/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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

import static neon.maps.generators.RoomGenerator.newExposed;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import neon.entities.Container;
import neon.entities.Creature;
import neon.entities.Door;
import neon.entities.EntityFactory;
import neon.entities.Item;
import neon.entities.property.Habitat;
import neon.maps.*;
import neon.maps.Region.Modifier;
import neon.maps.services.EntityStore;
import neon.maps.services.QuestProvider;
import neon.maps.services.ResourceProvider;
import neon.resources.RCreature;
import neon.resources.RItem;
import neon.resources.RTerrain;
import neon.resources.RZoneTheme;
import neon.util.Dice;

/**
 * Generates a single dungeon zone.
 *
 * <p>Refactored to support dependency injection for better testability and reduced coupling.
 *
 * @author mdriesen
 */
public class DungeonGenerator {
  // zone info
  private final RZoneTheme theme;
  private final Zone zone;

  // dependencies
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;
  private final QuestProvider questProvider;

  // random sources
  private final MapUtils mapUtils;
  private final Dice dice;

  // helper generators
  private final BlocksGenerator blocksGenerator;
  private final ComplexGenerator complexGenerator;
  private final CaveGenerator caveGenerator;
  private final MazeGenerator mazeGenerator;
  private final FeatureGenerator featureGenerator;

  // things
  private int[][] tiles; // information about the type of terrain
  private String[][] terrain; // terrain at that position

  /**
   * Creates a dungeon generator with dependency injection.
   *
   * @param theme the zone theme
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param questProvider the quest provider service
   */
  public DungeonGenerator(
      RZoneTheme theme,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      QuestProvider questProvider) {
    this(theme, entityStore, resourceProvider, questProvider, new MapUtils(), new Dice());
  }

  /**
   * Creates a dungeon generator with dependency injection and custom random sources.
   *
   * @param theme the zone theme
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param questProvider the quest provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public DungeonGenerator(
      RZoneTheme theme,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      QuestProvider questProvider,
      MapUtils mapUtils,
      Dice dice) {
    this.theme = theme;
    this.zone = null;
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.questProvider = questProvider;
    this.mapUtils = mapUtils;
    this.dice = dice;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.complexGenerator = new ComplexGenerator(mapUtils);
    this.caveGenerator = new CaveGenerator(dice);
    this.mazeGenerator = new MazeGenerator(dice);
    this.featureGenerator = new FeatureGenerator(mapUtils);
  }

  /**
   * Creates a dungeon generator for a specific zone with dependency injection.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param questProvider the quest provider service
   */
  public DungeonGenerator(
      Zone zone,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      QuestProvider questProvider) {
    this(zone, entityStore, resourceProvider, questProvider, new MapUtils(), new Dice());
  }

  /**
   * Creates a dungeon generator for a specific zone with dependency injection and custom random
   * sources.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param questProvider the quest provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public DungeonGenerator(
      Zone zone,
      EntityStore entityStore,
      ResourceProvider resourceProvider,
      QuestProvider questProvider,
      MapUtils mapUtils,
      Dice dice) {
    this.zone = zone;
    this.theme = zone.getTheme();
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.questProvider = questProvider;
    this.mapUtils = mapUtils;
    this.dice = dice;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.complexGenerator = new ComplexGenerator(mapUtils);
    this.caveGenerator = new CaveGenerator(dice);
    this.mazeGenerator = new MazeGenerator(dice);
    this.featureGenerator = new FeatureGenerator(mapUtils);
  }

  /**
   * Generates a zone.
   *
   * @param door the door used to enter this zone
   * @param previous the zone that contains the door used to enter this zone
   */
  public void generate(Door door, Zone previous, Atlas atlas) {
    // the map that contains this zone
    Dungeon map = (Dungeon) atlas.getMap(zone.getMap());

    // generate terrain
    generateTiles();

    // width and height of generated zone
    int width = tiles.length;
    int height = tiles[0].length;

    // create regions from terrain
    generateEngineContent(width, height);
    zone.fix();

    // place door to previous zone
    Point p = new Point(0, 0);
    do {
      p.x = dice.rollDice(1, width, -1);
      p.y = dice.rollDice(1, height, -1);
    } while (tiles[p.x][p.y] != MapUtils.FLOOR || !zone.getItems(p).isEmpty());

    Rectangle bounds = door.getShapeComponent();
    Point destPoint = new Point(bounds.x, bounds.y);
    int destMap = previous.getMap();
    int destZone = previous.getIndex();
    String doorType = theme.doors.split(",")[0];
    Door tdoor = (Door) EntityFactory.getItem(doorType, p.x, p.y, entityStore.createNewEntityUID());
    entityStore.addEntity(tdoor);
    tiles[p.x][p.y] = MapUtils.DOOR;
    tdoor.portal.setDestination(destPoint, destZone, destMap);
    tdoor.lock.open();
    zone.addItem(tdoor);
    door.portal.setDestPos(p);

    // check if a door to another zone is needed
    Collection<Integer> connections = map.getConnections(zone.getIndex());
    if (connections != null) {
      for (int to : connections) {
        ArrayList<Door> doors = new ArrayList<Door>();
        doors.add(door);
        if (to != previous.getIndex()) {
          Point pos = new Point(0, 0);
          do {
            pos.x = dice.rollDice(1, width, -1);
            pos.y = dice.rollDice(1, height, -1);
          } while (tiles[pos.x][pos.y] != MapUtils.FLOOR || !zone.getItems(pos).isEmpty());

          Door toDoor =
              (Door)
                  EntityFactory.getItem(
                      theme.doors.split(",")[0], pos.x, pos.y, entityStore.createNewEntityUID());
          entityStore.addEntity(toDoor);
          tiles[pos.x][pos.y] = MapUtils.DOOR;
          toDoor.lock.open();
          toDoor.portal.setDestination(null, to, 0);
          zone.addItem(toDoor);
        } else { // multiple doors between two zones
          for (long uid : previous.getItems()) {
            if (entityStore.getEntity(uid) instanceof Door fromDoor) {
              if (!doors.contains(fromDoor)
                  && fromDoor.portal.getDestMap() == 0
                  && fromDoor.portal.getDestZone() == zone.getIndex()) {
                Point pos = new Point(0, 0);
                do {
                  pos.x = dice.rollDice(1, width, -1);
                  pos.y = dice.rollDice(1, height, -1);
                } while (tiles[pos.x][pos.y] != MapUtils.FLOOR && !zone.getItems(pos).isEmpty());

                Door toDoor =
                    (Door)
                        EntityFactory.getItem(
                            theme.doors.split(",")[0],
                            pos.x,
                            pos.y,
                            entityStore.createNewEntityUID());
                entityStore.addEntity(toDoor);
                tiles[pos.x][pos.y] = MapUtils.DOOR;
                toDoor.lock.open();
                Rectangle fBounds = fromDoor.getShapeComponent();
                toDoor.portal.setDestination(new Point(fBounds.x, fBounds.y), to, 0);
                zone.addItem(toDoor);
                fromDoor.portal.setDestPos(pos);
                break;
              }
              doors.add(fromDoor);
            }
          }
        }
      }
    }

    // just check if a random quest object needs to be created
    String object = questProvider.getNextRequestedObject();
    if (object != null) {
      Point p1 = new Point(0, 0);
      do {
        p1.x = dice.rollDice(1, width, -1);
        p1.y = dice.rollDice(1, height, -1);
      } while (tiles[p1.x][p1.y] != MapUtils.FLOOR);
      if (resourceProvider.getResource(object) instanceof RItem) {
        Item item = EntityFactory.getItem(object, p1.x, p1.y, entityStore.createNewEntityUID());
        entityStore.addEntity(item);
        zone.addItem(item);
      } else if (resourceProvider.getResource(object) instanceof RCreature) {
        Creature creature =
            EntityFactory.getCreature(object, p1.x, p1.y, entityStore.createNewEntityUID());
        entityStore.addEntity(creature);
        zone.addCreature(creature);
      }
    }
  }

  /** Generates a single zone from a given theme. */
  public String[][] generateTiles() {
    // width and height of dungeon
    int width = mapUtils.random(theme.min, theme.max);
    int height = mapUtils.random(theme.min, theme.max);

    // base terrain without features
    tiles = generateBaseTiles(theme.type, width, height);
    terrain = makeTerrain(tiles, theme.floor.split(","));

    // scale factor for generating features, creatures and items
    double ratio = (width * height) / Math.pow(MapUtils.average(theme.min, theme.max), 2);

    // features
    generateFeatures(theme.features, ratio);

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

    return terrain;
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

  private void generateFeatures(Collection<Object[]> features, double ratio) {
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

  // to convert a string[][] into regions, items and creatures
  private void generateEngineContent(int width, int height) {
    byte layer = 0;
    int d = 0;
    String[] doors = theme.doors.split(",");

    RTerrain rt = (RTerrain) resourceProvider.getResource(theme.walls, "terrain");
    zone.addRegion(new Region(theme.walls, 0, 0, width, height, null, layer, rt));
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        String id;
        switch (tiles[x][y]) { // place correct terrain
          case MapUtils.DOOR_LOCKED:
          case MapUtils.DOOR_CLOSED:
          case MapUtils.DOOR:
            id = terrain[x][y].split(";")[0];
            d = mapUtils.random(1, doors.length) - 1;
            addDoor(id, doors[d], x, y, layer + 1);
            break;
          default:
            if (terrain[x][y] != null) {
              id = terrain[x][y].split(";")[0];
              rt = (RTerrain) resourceProvider.getResource(id, "terrain");
              zone.addRegion(new Region(id, x, y, 1, 1, null, layer + 1, rt));
            }
            break;
        }

        if (terrain[x][y] != null) { // check if items and creatures should be placed here
          String[] content = terrain[x][y].split(";");
          if (content.length > 1) {
            for (int j = 1; j < content.length; j++) {
              if (content[j].startsWith("i")) {
                addItem(content[j], x, y);
              } else if (content[j].startsWith("c")) {
                addCreature(content[j], x, y);
              }
            }
          }
        }
      }
    }
  }

  private void addDoor(String terrain, String id, int x, int y, int layer) {
    Door door = (Door) EntityFactory.getItem(id, x, y, entityStore.createNewEntityUID());
    entityStore.addEntity(door);
    if (tiles[x][y] == MapUtils.DOOR_LOCKED) {
      door.lock.setLockDC(10);
      door.lock.lock();
    } else if (tiles[x][y] == MapUtils.DOOR_CLOSED) {
      door.lock.close();
    }
    zone.addItem(door);
    RTerrain rt = (RTerrain) resourceProvider.getResource(terrain, "terrain");
    zone.addRegion(new Region(terrain, x, y, 1, 1, null, layer + 1, rt));
  }

  private void addCreature(String description, int x, int y) {
    String id = description.replace("c:", "");
    Creature creature = EntityFactory.getCreature(id, x, y, entityStore.createNewEntityUID());
    // no land creatures in water
    Rectangle bounds = creature.getShapeComponent();
    Modifier modifier = zone.getRegion(bounds.getLocation()).getMovMod();
    Habitat habitat = creature.species.habitat;
    if (habitat == Habitat.LAND && !(modifier == Modifier.NONE || modifier == Modifier.ICE)) {
      return; // place land animals only on land
    }
    entityStore.addEntity(creature);
    zone.addCreature(creature);
  }

  private void addItem(String description, int x, int y) {
    String id = description.replace("i:", "");
    Item item = EntityFactory.getItem(id, x, y, entityStore.createNewEntityUID());
    entityStore.addEntity(item);
    if (item instanceof Container) {
      for (String s : ((RItem.Container) item.resource).contents) {
        Item i = EntityFactory.getItem(s, entityStore.createNewEntityUID());
        ((Container) item).addItem(i.getUID());
        entityStore.addEntity(i);
      }
    }
    zone.addItem(item);
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
