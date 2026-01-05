/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2010 - Maarten Driesen
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

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.ListIterator;
import neon.maps.MapUtils;

/**
 * This class contains methods to generate collections of rectangles that are packed in different
 * ways.
 *
 * @author mdriesen
 */
public class BlocksGenerator {
  /** Shared default instance for static convenience methods. */
  private static final BlocksGenerator DEFAULT_INSTANCE = new BlocksGenerator();

  private final MapUtils mapUtils;

  /** Creates a new BlocksGenerator with default (non-deterministic) random behavior. */
  public BlocksGenerator() {
    this(new MapUtils());
  }

  /**
   * Creates a new BlocksGenerator with a specific MapUtils instance for deterministic testing.
   *
   * @param mapUtils the MapUtils instance to use for random operations
   */
  public BlocksGenerator(MapUtils mapUtils) {
    this.mapUtils = mapUtils;
  }

  /**
   * Static convenience method for generating BSP rectangles using the default instance.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @return an ArrayList of Rectangles
   */
  public static ArrayList<Rectangle> generateBSPRectangles(int w, int h, int minW, int maxW) {
    return DEFAULT_INSTANCE.createBSPRectangles(w, h, minW, maxW);
  }

  /**
   * Static convenience method for generating packed rectangles using the default instance.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @param ratio maximum aspect ratio
   * @param numRecs number of rectangles to generate
   * @return an ArrayList of Rectangles
   */
  public static ArrayList<Rectangle> generatePackedRectangles(
      int w, int h, int minW, int maxW, double ratio, int numRecs) {
    return DEFAULT_INSTANCE.createPackedRectangles(w, h, minW, maxW, ratio, numRecs);
  }

  /**
   * Static convenience method for generating sparse rectangles using the default instance.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @param ratio maximum aspect ratio
   * @param numRecs number of rectangles to generate
   * @return an ArrayList of Rectangles
   */
  public static ArrayList<Rectangle> generateSparseRectangles(
      int w, int h, int minW, int maxW, double ratio, int numRecs) {
    return DEFAULT_INSTANCE.createSparseRectangles(w, h, minW, maxW, ratio, numRecs);
  }

  /**
   * Generates a given number of closely packed rectangles with given maximum and minimum size and
   * aspect ratio within a rectangle with a given width and height.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @param ratio maximum aspect ratio
   * @param numRecs number of rectangles to generate
   * @return an ArrayList of Rectangles
   */
  public ArrayList<Rectangle> createPackedRectangles(
      int w, int h, int minW, int maxW, double ratio, int numRecs) {
    return new RectangleGenerator(w, h, minW, maxW, ratio, mapUtils).generate(numRecs);
  }

  /**
   * Generates a given number of loosely packed rectangles.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @param ratio maximum aspect ratio
   * @param numRecs number of rectangles to generate
   * @return an ArrayList of Rectangles
   */
  public ArrayList<Rectangle> createSparseRectangles(
      int w, int h, int minW, int maxW, double ratio, int numRecs) {
    return new SparseGenerator(w, h, minW, maxW, ratio, mapUtils).generate(numRecs);
  }

  /**
   * Divides a rectangle with the given width and height in smaller rectangles with given minimum
   * and maximum width, using a BSP algorithm.
   *
   * @param w width of the bounding area
   * @param h height of the bounding area
   * @param minW minimum rectangle width
   * @param maxW maximum rectangle width
   * @return an ArrayList of Rectangles
   */
  public ArrayList<Rectangle> createBSPRectangles(int w, int h, int minW, int maxW) {
    return new BSPGenerator(w, h, minW, maxW, mapUtils).generate();
  }

  private static class SparseGenerator {
    private final int w;
    private final int h;
    private final int minW;
    private final int maxW;
    private final double ratio;
    private final ArrayList<Rectangle> rooms;
    private final MapUtils mapUtils;
    private Area area;

    private SparseGenerator(int w, int h, int minW, int maxW, double ratio, MapUtils mapUtils) {
      this.w = w;
      this.h = h;
      this.minW = minW;
      this.maxW = maxW;
      this.ratio = ratio;
      this.mapUtils = mapUtils;
      rooms = new ArrayList<Rectangle>();
    }

    private ArrayList<Rectangle> generate(int numRecs) {
      for (int i = 0; i < numRecs; i++) {
        Rectangle room = randomRoom();
        if (room != null) {
          rooms.add(room);
        }
      }

      return rooms;
    }

    private Rectangle randomRoom() {
      Rectangle r = mapUtils.randomRectangle(minW, maxW, minW, maxW, ratio);

      r.x = mapUtils.random(0, w - r.width);
      r.y = mapUtils.random(0, h - r.height);
      if (rooms.isEmpty()) { // just throw room in the middle
        area = new Area(r);
      } else { // move room around randomly
        int i = 0;
        while (area.intersects(r) && i < 100) { // try 100 times
          r.x = mapUtils.random(0, w - r.width);
          r.y = mapUtils.random(0, h - r.height);
          i++;
        }
        if (i > 99) {
          return null;
        } else {
          area.add(new Area(r));
        }
      }

      return r;
    }
  }

  private static class BSPGenerator {
    private final int w;
    private final int h;
    private final int minW;
    private final int maxW;
    private final MapUtils mapUtils;

    private BSPGenerator(int w, int h, int minW, int maxW, MapUtils mapUtils) {
      this.w = w;
      this.h = h;
      this.minW = minW;
      this.maxW = maxW;
      this.mapUtils = mapUtils;
    }

    private ArrayList<Rectangle> generate() {
      ArrayList<Rectangle> buffer = new ArrayList<Rectangle>();
      ArrayList<Rectangle> result = new ArrayList<Rectangle>();

      buffer.add(new Rectangle(w, h));

      while (!buffer.isEmpty()) {
        ListIterator<Rectangle> i = buffer.listIterator();
        while (i.hasNext()) {
          Rectangle r = i.next();
          i.remove();
          Rectangle r1;
          Rectangle r2;
          if (r.width < r.height) {
            int dy = mapUtils.random(r.y + minW, r.y + r.height - minW);
            r1 = new Rectangle(r.x, r.y, r.width, dy - r.y);
            r2 = new Rectangle(r.x, dy, r.width, r.y + r.height - dy);
          } else {
            int dx = mapUtils.random(r.x + minW, r.x + r.width - minW);
            r1 = new Rectangle(r.x, r.y, dx - r.x, r.height);
            r2 = new Rectangle(dx, r.y, r.x + r.width - dx, r.height);
          }
          if (r1.width < maxW && r1.height < maxW) {
            result.add(r1);
          } else {
            i.add(r1);
          }
          if (r2.width < maxW && r2.height < maxW) {
            result.add(r2);
          } else {
            i.add(r2);
          }
        }
      }

      return result;
    }
  }

  private static class RectangleGenerator {
    private final int w;
    private final int h;
    private final int minW;
    private final int maxW;
    private final double ratio;
    private final ArrayList<Rectangle> rooms;
    private final MapUtils mapUtils;
    private Area area;

    private RectangleGenerator(int w, int h, int minW, int maxW, double ratio, MapUtils mapUtils) {
      this.w = w;
      this.h = h;
      this.minW = minW;
      this.maxW = maxW;
      this.ratio = ratio;
      this.mapUtils = mapUtils;
      rooms = new ArrayList<>();
    }

    private ArrayList<Rectangle> generate(int numRecs) {
      for (int i = 0; i < numRecs; i++) {
        Rectangle room = randomRoom();
        if (room != null) {
          rooms.add(room);
        }
      }

      return rooms;
    }

    private Rectangle randomRoom() {
      Rectangle r = mapUtils.randomRectangle(minW, maxW, minW, maxW, ratio);

      r.x = w / 2 - r.width / 2;
      r.y = h / 2 - r.height / 2;
      if (rooms.isEmpty()) { // just throw room in the middle
        area = new Area(r);
      } else { // move room around in a spiral
        int i = 0;
        while (area.intersects(r) && i < 9 * w * h) {
          r.x = w / 2 - r.width / 2 + (int) (i / 3 * Math.cos(i / 3));
          r.y = h / 2 - r.height / 2 + (int) (i / 3 * Math.sin(i / 3));
          i++;
        }
        if (r.x < 0 || r.y < 0 || r.x + r.width > w || r.y + r.height > h) {
          return null; // if it doesn't work, give up
        }
        area.add(new Area(r));
      }

      return r;
    }
  }
}
