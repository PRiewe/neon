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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import neon.maps.MapUtils;

/**
 * Generator for complex dungeon layouts including sparse, BSP, and packed dungeons.
 *
 * @author mdriesen
 */
public class ComplexGenerator {
  private final MapUtils mapUtils;
  private final BlocksGenerator blocksGenerator;
  private final RoomGenerator roomGenerator;

  /** Creates a new ComplexGenerator with default (non-deterministic) random behavior. */
  public ComplexGenerator() {
    this(new MapUtils());
  }

  /**
   * Creates a new ComplexGenerator with a specific MapUtils instance for deterministic testing.
   *
   * @param mapUtils the MapUtils instance to use for random operations
   */
  public ComplexGenerator(MapUtils mapUtils) {
    this.mapUtils = mapUtils;
    this.blocksGenerator = new BlocksGenerator(mapUtils);
    this.roomGenerator = new RoomGenerator(mapUtils);
  }

  /**
   * Generates a sparse dungeon layout.
   *
   * @param width dungeon width
   * @param height dungeon height
   * @param n number of rooms
   * @param rMin minimum room size
   * @param rMax maximum room size
   * @return 2D array of tile types
   */
  protected int[][] generateSparseDungeon(int width, int height, int n, int rMin, int rMax) {
    // create a few things
    int[][] tiles = new int[width][height];
    ArrayList<RoomGenerator.Room> rooms = new ArrayList<RoomGenerator.Room>();
    for (Rectangle room :
        blocksGenerator.createSparseRectangles(width - 1, height - 1, rMin, rMax, 2, n)) {
      if (room.width > 14 || room.height > 14) {
        rooms.add(roomGenerator.makeCaveRoom(tiles, room));
      } else if (room.width > 9 || room.height > 9) {
        rooms.add(roomGenerator.makePolyRoom(tiles, room));
      } else {
        rooms.add(roomGenerator.makeRoom(tiles, room));
      }
    }

    // connect rooms: choose starting room, then take room and tunnel, etc.
    Collections.shuffle(rooms, mapUtils.randomSource().getRandom()); // shuffle a bit first
    Area area = null;
    for (RoomGenerator.Room room : rooms) {
      if (area == null) {
        area = new Area(room.getBounds());
      } else {
        connect(tiles, room.getBounds(), area);
        area.add(new Area(room.getBounds()));
      }
    }

    // remove suspicious things
    clean(tiles);
    return tiles;
  }

  /**
   * Generates a BSP (Binary Space Partitioning) dungeon layout.
   *
   * @param width dungeon width
   * @param height dungeon height
   * @param rMin minimum room size
   * @param rMax maximum room size
   * @return 2D array of tile types
   */
  protected int[][] generateBSPDungeon(int width, int height, int rMin, int rMax) {
    ArrayList<Rectangle> rooms =
        blocksGenerator.createBSPRectangles(width - 1, height - 1, rMin, rMax);
    Collections.shuffle(rooms, mapUtils.randomSource().getRandom());
    int[][] tiles = new int[width][height];
    Area area = new Area();

    // place all rooms
    for (Rectangle room : rooms) {
      roomGenerator.makeRoom(tiles, room);
    }

    // merge a few rooms into polygons
    boolean[] wasted = new boolean[rooms.size()];
    // try to connect 10%
    while (MapUtils.amount(wasted, true) < wasted.length / 10) {
      int index = mapUtils.random(0, wasted.length - 1);
      if (!wasted[index]) {
        wasted[index] = combine(rooms.get(index), rooms, tiles);
      }
    }

    // connect rooms: choose starting room, then take room and tunnel, etc.
    for (Rectangle room : rooms) {
      if (!area.isEmpty() && !wasted[rooms.indexOf(room)]) { // tunnel to already existing area
        connect(tiles, room, area);
      }
      area.add(new Area(room)); // and then add current room
    }

    // remove suspicious things
    clean(tiles);

    return tiles;
  }

  /**
   * Generates a packed dungeon layout.
   *
   * @param width dungeon width
   * @param height dungeon height
   * @param n number of rooms
   * @param rMin minimum room size
   * @param rMax maximum room size
   * @return 2D array of tile types
   */
  protected int[][] generatePackedDungeon(int width, int height, int n, int rMin, int rMax) {
    ArrayList<Rectangle> rooms =
        blocksGenerator.createPackedRectangles(width - 1, height - 1, rMin, rMax, 2, n);
    int[][] tiles = new int[width][height];
    Area area = new Area();

    // place all rooms
    for (Rectangle room : rooms) {
      roomGenerator.makeRoom(tiles, room);
    }

    // merge a few rooms into polygons
    boolean[] wasted = new boolean[rooms.size()];
    while (mapUtils.randomSource().nextDouble() < 0.5) {
      int index = mapUtils.random(0, wasted.length - 1);
      if (!wasted[index]) {
        wasted[index] = combine(rooms.get(index), rooms, tiles);
      }
    }

    // connect rooms: choose starting room, then take room and tunnel, etc.
    for (Rectangle room : rooms) {
      if (!area.isEmpty() && !wasted[rooms.indexOf(room)]) { // tunnel to already existing area
        connect(tiles, room, area);
      }
      area.add(new Area(room)); // and then add current room
    }

    // remove suspicious things
    clean(tiles);

    return tiles;
  }

  private static boolean isFloor(int i) {
    return i == MapUtils.FLOOR || i == MapUtils.CORRIDOR || isDoor(i);
  }

  private static boolean isDoor(int i) {
    return i == MapUtils.DOOR || i == MapUtils.DOOR_LOCKED || i == MapUtils.DOOR_CLOSED;
  }

  private static boolean combine(Rectangle room, ArrayList<Rectangle> rooms, int[][] tiles) {
    for (Rectangle rec : rooms) {
      if (rec != room && rec.intersects(room)) {
        Rectangle inter = rec.intersection(room);
        if ((inter.width > 1
                && rec.width != room.width
                && (room.x == rec.x || room.x + room.width == rec.x + rec.width))
            || (inter.height > 1
                && rec.height != room.height
                && (room.y == rec.y || room.y + room.height == rec.y + rec.height))) {
          for (int x = inter.x; x < inter.x + inter.width; x++) {
            for (int y = inter.y; y < inter.y + inter.height; y++) {
              if (tiles[x][y] == MapUtils.WALL_ROOM) {
                tiles[x][y] = MapUtils.FLOOR;
              }
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  private boolean goHorizontal(int[][] tiles, Rectangle source, Rectangle dest, Area area) {
    while (source.x != dest.x) { // not at same width yet
      int dir = (source.x < dest.x) ? 1 : -1; // which direction to go
      source.x += dir;
      if (tiles[source.x][source.y] == MapUtils.ENTRY
          || tiles[source.x][source.y] == MapUtils.CORNER) {
        source.x -= dir; // obstacle found
        return false;
      } else if (tiles[source.x][source.y] == MapUtils.CORRIDOR) {
        return true;
      } else if (tiles[source.x][Math.max(0, source.y - 1)] == MapUtils.CORRIDOR
          || tiles[source.x][Math.min(tiles[0].length - 1, source.y + 1)] == MapUtils.CORRIDOR) {
        // encountered another corridor
        tiles[source.x][source.y] = MapUtils.TEMP;
        return true;
      } else if (tiles[source.x][source.y] != MapUtils.FLOOR) {
        if (tiles[source.x][source.y] == MapUtils.WALL_ROOM) { // room wall breached
          tiles[source.x][source.y + 1] = MapUtils.ENTRY;
          tiles[source.x][source.y] = mapUtils.random(MapUtils.DOOR, MapUtils.DOOR_LOCKED);
          tiles[source.x][source.y - 1] = MapUtils.ENTRY;
          if (area.contains(source)) {
            return true;
          }
        } else {
          tiles[source.x][source.y] = MapUtils.TEMP;
        }
      } else { // if a floor tile is encountered
        if (area.contains(source)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean goVertical(int[][] tiles, Rectangle source, Rectangle dest, Area area) {
    while (source.y != dest.y) { // not at same height yet
      int dir = (source.y < dest.y) ? 1 : -1;
      source.y += dir;
      if (tiles[source.x][source.y] == MapUtils.ENTRY
          || tiles[source.x][source.y] == MapUtils.CORNER) {
        source.y -= dir; // obstacle found
        return false;
      } else if (tiles[source.x][source.y] == MapUtils.CORRIDOR) {
        return true;
      } else if (tiles[Math.min(tiles.length - 1, source.x + 1)][source.y] == MapUtils.CORRIDOR
          || tiles[Math.max(0, source.x - 1)][source.y]
              == MapUtils.CORRIDOR) { // encountered another corridor
        tiles[source.x][source.y] = MapUtils.TEMP;
        return true;
      } else if (tiles[source.x][source.y] != MapUtils.FLOOR) {
        if (tiles[source.x][source.y] == MapUtils.WALL_ROOM) { // room wall breached
          tiles[source.x - 1][source.y] = MapUtils.ENTRY;
          tiles[source.x][source.y] = mapUtils.random(MapUtils.DOOR, MapUtils.DOOR_LOCKED);
          tiles[source.x + 1][source.y] = MapUtils.ENTRY;
          if (area.contains(source)) {
            return true;
          }
        } else {
          tiles[source.x][source.y] = MapUtils.TEMP;
        }
      } else { // if a floor tile is encountered
        if (area.contains(source)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isRoomWall(int i) {
    return i == MapUtils.CORNER || i == MapUtils.WALL_ROOM || i == MapUtils.ENTRY;
  }

  private boolean connect(int[][] tiles, Rectangle room, Area area) {
    boolean ok = false;
    int loop = 10;
    do {
      Rectangle source = new Rectangle((int) room.getCenterX(), (int) room.getCenterY(), 1, 1);
      Rectangle dest = new Rectangle(1, 1);
      do { // find point within already done area
        dest.x = mapUtils.random(1, tiles.length - 1);
        dest.y = mapUtils.random(1, tiles[dest.x].length - 1);
      } while (!area.contains(dest));

      if (loop % 2 > 0) { // occasionally start in the other direction
        ok = goHorizontal(tiles, source, dest, area) || goVertical(tiles, source, dest, area);
      } else {
        ok = goVertical(tiles, source, dest, area) || goHorizontal(tiles, source, dest, area);
      }
      for (int x = 0; x < tiles.length; x++) { // replace temp corridors
        for (int y = 0; y < tiles[x].length; y++) {
          if (tiles[x][y] == MapUtils.TEMP) {
            tiles[x][y] =
                ok ? MapUtils.CORRIDOR : MapUtils.WALL; // temp becomes real corridor if ok
          }
          if (room.contains(x, y)
              && (isDoor(tiles[x][y]) || tiles[x][y] == MapUtils.ENTRY)
              && !ok) {
            tiles[x][y] = MapUtils.WALL_ROOM; // entry and door become wall again if not ok
          }
        }
      }
    } while (!ok && loop-- > 0);
    return ok;
  }

  private void clean(int[][] tiles) {
    // find and remove unconnected pieces
    int[][] fill = new int[tiles.length][tiles[0].length];
    for (int x = 1; x < tiles.length - 1; x++) {
      for (int y = 1; y < tiles[x].length - 1; y++) {
        if (isFloor(tiles[x][y])) {
          fill[x][y] = MapUtils.FLOOR;
        }
      }
    }
    Point start = new Point(1, 1); // start position
    do { // find point within dungeon
      start.x = mapUtils.random(1, tiles.length - 1);
      start.y = mapUtils.random(1, tiles[start.x].length - 1);
    } while (tiles[start.x][start.y] != MapUtils.FLOOR);
    floodFill(fill, start.x, start.y, MapUtils.FLOOR, MapUtils.TEMP);
    for (int x = 1; x < fill.length - 1; x++) {
      for (int y = 1; y < fill[x].length - 1; y++) {
        if (fill[x][y] == MapUtils.FLOOR) {
          tiles[x][y] = MapUtils.WALL;
        }
      }
    }

    // other suspicious things
    for (int x = 1; x < tiles.length - 1; x++) {
      for (int y = 1; y < tiles[x].length - 1; y++) {
        // hole in room wall
        if ((isRoomWall(tiles[x][y + 1]) && isRoomWall(tiles[x][y - 1]))
            && (isFloor(tiles[x + 1][y]) != isFloor(tiles[x - 1][y]))) {
          tiles[x][y] = MapUtils.WALL_ROOM;
        }
        if ((isRoomWall(tiles[x + 1][y]) && isRoomWall(tiles[x - 1][y]))
            && (isFloor(tiles[x][y + 1]) != isFloor(tiles[x][y - 1]))) {
          tiles[x][y] = MapUtils.WALL_ROOM;
        }

        // doors next to each other
        if (isDoor(tiles[x][y])
            && (isDoor(tiles[x + 1][y])
                || isDoor(tiles[x - 1][y])
                || isDoor(tiles[x][y + 1])
                || isDoor(tiles[x][y - 1]))) {
          tiles[x][y] = MapUtils.FLOOR;
        }

        // two doors between adjacent rooms
        if (isRoomWall(tiles[x][y])
            && isFloor(tiles[x][y + 1])
            && isFloor(tiles[x][y - 1])
            && isFloor(tiles[x + 1][y])
            && isFloor(tiles[x - 1][y])) {
          if (mapUtils.randomSource().nextDouble()
              < 0.5) { // to not always pick the left or top door
            if (isRoomWall(tiles[x + 2][y])) {
              tiles[x + 1][y] = MapUtils.WALL_ROOM;
            } else {
              tiles[x][y + 1] = MapUtils.WALL_ROOM;
            }
          } else {
            if (isRoomWall(tiles[x - 2][y])) {
              tiles[x - 1][y] = MapUtils.WALL_ROOM;
            } else {
              tiles[x][y - 1] = MapUtils.WALL_ROOM;
            }
          }
        }
      }
    }
  }

  private static void floodFill(int[][] fill, int x, int y, int target, int replacement) {
    if (fill[x][y] != target) {
    } else {
      fill[x][y] = replacement;
      if (x > 0) {
        floodFill(fill, x - 1, y, target, replacement);
      }
      if (x < fill.length - 1) {
        floodFill(fill, x + 1, y, target, replacement);
      }
      if (y > 0) {
        floodFill(fill, x, y - 1, target, replacement);
      }
      if (y < fill[x].length) {
        floodFill(fill, x, y + 1, target, replacement);
      }
    }
  }
}
