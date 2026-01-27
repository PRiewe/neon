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

import java.awt.*;
import java.util.*;
import neon.core.GameContext;
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
  private final GameContext gameContext;
  private final EntityFactory entityFactory;

  // random sources
  private final MapUtils mapUtils;
  private final Dice dice;
  private final DungeonTileGenerator dungeonTileGenerator;
  int[][] tiles;
  String[][] terrain;

  /**
   * Creates a dungeon generator with dependency injection.
   *
   * @param theme the zone theme
   * @param questProvider the quest provider service
   */
  public DungeonGenerator(RZoneTheme theme, QuestProvider questProvider, GameContext gameContext) {
    this(theme, questProvider, gameContext, new MapUtils(), new Dice());
  }

  /**
   * Creates a dungeon generator with dependency injection and custom random sources.
   *
   * @param theme the zone theme
   * @param questProvider the quest provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public DungeonGenerator(
      RZoneTheme theme,
      QuestProvider questProvider,
      GameContext gameContext,
      MapUtils mapUtils,
      Dice dice) {
    this.theme = theme;
    this.gameContext = gameContext;
    this.zone = null;
    this.entityStore = gameContext.getStore();
    this.resourceProvider = gameContext.getResources();
    this.entityFactory = new EntityFactory(gameContext);
    this.questProvider = questProvider;
    this.dungeonTileGenerator = new DungeonTileGenerator(theme, mapUtils, dice);
    this.mapUtils = mapUtils;
    this.dice = dice;
  }

  /**
   * Creates a dungeon generator for a specific zone with dependency injection.
   *
   * @param zone the zone to generate
   * @param questProvider the quest provider service
   */
  public DungeonGenerator(Zone zone, QuestProvider questProvider, GameContext gameContext) {
    this(zone, questProvider, gameContext, new MapUtils(), new Dice());
  }

  /**
   * Creates a dungeon generator for a specific zone with dependency injection and custom random
   * sources.
   *
   * @param zone the zone to generate
   * @param questProvider the quest provider service
   * @param mapUtils the MapUtils instance for random operations
   * @param dice the Dice instance for random operations
   */
  public DungeonGenerator(
      Zone zone,
      QuestProvider questProvider,
      GameContext gameContext,
      MapUtils mapUtils,
      Dice dice) {
    this.zone = zone;
    this.theme = zone.getTheme();
    this.entityStore = gameContext.getStore();
    this.resourceProvider = gameContext.getResources();
    this.questProvider = questProvider;
    this.gameContext = gameContext;
    this.entityFactory = new EntityFactory(gameContext);
    this.dungeonTileGenerator = new DungeonTileGenerator(theme, mapUtils, dice);

    this.mapUtils = mapUtils;
    this.dice = dice;
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
    var layout = dungeonTileGenerator.generateTiles();
    tiles = layout.tiles();
    terrain = layout.terrain();

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
    Door tdoor = (Door) entityFactory.getItem(doorType, p.x, p.y, entityStore.createNewEntityUID());
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
                  entityFactory.getItem(
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
                        entityFactory.getItem(
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
        Item item = entityFactory.getItem(object, p1.x, p1.y, entityStore.createNewEntityUID());
        entityStore.addEntity(item);
        zone.addItem(item);
      } else if (resourceProvider.getResource(object) instanceof RCreature) {
        Creature creature =
            entityFactory.getCreature(object, p1.x, p1.y, entityStore.createNewEntityUID());
        entityStore.addEntity(creature);
        zone.addCreature(creature);
      }
    }
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
    Door door = (Door) entityFactory.getItem(id, x, y, entityStore.createNewEntityUID());
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
    Creature creature = entityFactory.getCreature(id, x, y, entityStore.createNewEntityUID());
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
    Item item = entityFactory.getItem(id, x, y, entityStore.createNewEntityUID());
    entityStore.addEntity(item);
    if (item instanceof Container) {
      for (String s : ((RItem.Container) item.resource).contents) {
        Item i = entityFactory.getItem(s, entityStore.createNewEntityUID());
        ((Container) item).addItem(i.getUID());
        entityStore.addEntity(i);
      }
    }
    zone.addItem(item);
  }
}
