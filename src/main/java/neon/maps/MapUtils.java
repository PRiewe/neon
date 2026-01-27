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

package neon.maps;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;
import neon.util.DefaultRandomSource;
import neon.util.RandomSource;

/**
 * Utility class for map generation operations including random shape generation. Can be
 * instantiated with a specific {@link RandomSource} for deterministic testing.
 */
public record MapUtils(RandomSource randomSource) {
  public static final int WALL = 0;
  public static final int FLOOR = 1;
  public static final int DOOR = 2;
  public static final int DOOR_CLOSED = 3;
  public static final int DOOR_LOCKED = 4;
  public static final int CORRIDOR = 5;
  public static final int WALL_ROOM = 6;
  public static final int ENTRY = 7;
  public static final int CORNER = 8;
  public static final int TEMP = 9;

  /** Creates a new MapUtils with a default (non-deterministic) random source. */
  public MapUtils() {
    this(new DefaultRandomSource());
  }

  /**
   * Creates a new MapUtils with a specific random source. Use this constructor for deterministic
   * testing.
   *
   * @param randomSource the random source to use
   */
  public MapUtils {}

  /**
   * Factory method to create a MapUtils with a seeded random source for reproducible results.
   *
   * @param seed the seed for the random number generator
   * @return a new MapUtils instance with seeded random behavior
   */
  public static MapUtils withSeed(long seed) {
    return new MapUtils(new DefaultRandomSource(seed));
  }

  /**
   * Returns a rectangle with the given min/max width and height.
   *
   * @param minW minimum width
   * @param maxW maximum width
   * @param minH minimum height
   * @param maxH maximum height
   * @return a random Rectangle with its origin at (0,0)
   */
  public Rectangle randomRectangle(int minW, int maxW, int minH, int maxH) {
    int w = random(minW, maxW);
    int h = random(minH, maxH);
    return new Rectangle(w, h);
  }

  /**
   * Returns a square with the given min/max side length.
   *
   * @param minW minimum side length
   * @param maxW maximum side length
   * @return a square with its origin at (0,0)
   */
  public Rectangle randomSquare(int minW, int maxW) {
    int w = random(minW, maxW);
    return new Rectangle(w, w);
  }

  /**
   * Returns a rectangle with the given min/max dimensions and a maximum width/height or
   * height/width ratio.
   *
   * @param minW minimum width
   * @param maxW maximum width
   * @param minH minimum height
   * @param maxH maximum height
   * @param ratio maximum aspect ratio
   * @return a random Rectangle with its origin at (0,0)
   */
  public Rectangle randomRectangle(int minW, int maxW, int minH, int maxH, double ratio) {
    int w = random(minW, maxW);
    int hMin = Math.max(minH, (int) (w / ratio));
    int hMax = Math.min(maxH, (int) (w * ratio));
    int h = random(hMin, hMax);
    return new Rectangle(w, h);
  }

  /**
   * Generates a random rectangle within the given rectangle.
   *
   * @param minW minimum width
   * @param bounds the bounding rectangle
   * @return a Rectangle within the bounds
   */
  public Rectangle randomRectangle(int minW, Rectangle bounds) {
    int w = random(minW, bounds.width - 1);
    int h = random(minW, bounds.height - 1);
    Rectangle rec = new Rectangle(w, h);

    while (!bounds.contains(rec)) {
      rec.x = random(bounds.x, bounds.x + bounds.width - w);
      rec.y = random(bounds.y, bounds.y + bounds.height - h);
    }
    return rec;
  }

  /**
   * Returns a random int between min and max (min and max included).
   *
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive)
   * @return a random int between min and max
   */
  public int random(int min, int max) {
    return randomSource.nextInt(min, max);
  }

  /**
   * Returns the random source used by this MapUtils instance.
   *
   * @return the random source
   */
  @Override
  public RandomSource randomSource() {
    return randomSource;
  }

  /**
   * Returns a random polygon with approximately the given number of vertices. The polygon is not
   * guaranteed to be convex, but will not intersect itself.
   *
   * @param r a rectangle bounding the polygon
   * @param corners the approximate number of vertices
   * @return a polygon that is bounded by the given rectangle
   */
  public Polygon randomPolygon(Rectangle r, int corners) {
    Rectangle up = new Rectangle(r.x + r.width / 4, r.y, r.width / 2, r.height / 4);
    Rectangle right =
        new Rectangle(r.x + 3 * r.width / 4, r.y + r.height / 4, r.width / 4, r.height / 2);
    Rectangle down =
        new Rectangle(r.x + r.width / 4, r.y + 3 * r.height / 4, r.width / 2, r.height / 4);
    Rectangle left = new Rectangle(r.x, r.y + r.height / 4, r.width / 4, r.height / 2);

    int numPoints = corners / 4;

    int[] xPoints = new int[4 * numPoints];
    int[] yPoints = new int[4 * numPoints];
    int[] buffer = new int[numPoints];

    // top
    for (int i = 0; i < numPoints; i++) {
      Point p = randomPoint(up);
      buffer[i] = p.x;
      yPoints[i] = p.y;
    }
    Arrays.sort(buffer);
    System.arraycopy(buffer, 0, xPoints, 0, numPoints);

    // right
    for (int i = 0; i < numPoints; i++) {
      Point p = randomPoint(right);
      xPoints[numPoints + i] = p.x;
      buffer[i] = p.y;
    }
    Arrays.sort(buffer);
    System.arraycopy(buffer, 0, yPoints, numPoints, numPoints);

    // bottom
    for (int i = 0; i < numPoints; i++) {
      Point p = randomPoint(down);
      buffer[i] = p.x;
      yPoints[2 * numPoints + i] = p.y;
    }
    Arrays.sort(buffer);
    System.arraycopy(reverse(buffer), 0, xPoints, 2 * numPoints, numPoints);

    // left
    for (int i = 0; i < numPoints; i++) {
      Point p = randomPoint(left);
      xPoints[3 * numPoints + i] = p.x;
      buffer[i] = p.y;
    }
    Arrays.sort(buffer);
    System.arraycopy(reverse(buffer), 0, yPoints, 3 * numPoints, numPoints);

    return new Polygon(xPoints, yPoints, 4 * numPoints);
  }

  private static int[] reverse(int[] array) {
    int length = array.length;
    int[] reverse = new int[length];
    for (int i = 0; i < length; i++) {
      reverse[length - i - 1] = array[i];
    }

    return reverse;
  }

  /**
   * Returns a random point in the given rectangle.
   *
   * @param r the bounding rectangle
   * @return a random point within the rectangle
   */
  public Point randomPoint(Rectangle r) {
    return new Point(random(r.x, r.x + r.width), random(r.y, r.y + r.height));
  }

  /**
   * Returns a ribbon of width one, running from one side of a rectangle to the opposite one.
   *
   * @param r the bounding rectangle
   * @param horizontal true for horizontal ribbon, false for vertical
   * @return an array of points contained in the ribbon
   */
  public Point[] randomRibbon(Rectangle r, boolean horizontal) {
    // direction: true is horizontal, false is vertical
    Point[] ribbon;

    if (horizontal) {
      ribbon = new Point[r.width];
      // start position
      int y = random(r.y, r.y + r.height);
      ribbon[0] = new Point(r.x, y);
      for (int i = 1; i < r.width; i++) { // next points
        y = random(Math.max(r.y, y - 1), Math.min(r.y + r.height, y + 1));
        ribbon[i] = new Point(r.x + i, y);
      }
    } else {
      ribbon = new Point[r.height];
      // start position
      int x = random(r.x, r.x + r.width);
      ribbon[0] = new Point(x, r.y);
      for (int i = 1; i < r.height; i++) {
        x = random(Math.max(r.x, x - 1), Math.min(r.x + r.width, x + 1));
        ribbon[i] = new Point(x, r.y + i);
      }
    }

    return ribbon;
  }

  /**
   * Counts the number of times the given boolean occurs in the given array.
   *
   * @param array the array to search
   * @param ref the boolean value to count
   * @return the number of occurrences
   */
  public static int amount(boolean[] array, boolean ref) {
    int count = 0;
    for (boolean o : array) {
      if (ref == o) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the average of two integers.
   *
   * @param x1 first integer
   * @param x2 second integer
   * @return the average (integer division)
   */
  public static int average(int x1, int x2) {
    return (x1 + x2) / 2;
  }
}
