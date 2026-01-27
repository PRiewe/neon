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
import java.awt.Polygon;
import java.awt.Rectangle;
import neon.maps.MapUtils;

/**
 * Generator for terrain features like lakes and rivers.
 *
 * @author mdriesen
 */
public class FeatureGenerator {
  private final MapUtils mapUtils;

  /** Creates a new FeatureGenerator with default (non-deterministic) random behavior. */
  public FeatureGenerator() {
    this(new MapUtils());
  }

  /**
   * Creates a new FeatureGenerator with a specific MapUtils instance for deterministic testing.
   *
   * @param mapUtils the MapUtils instance to use for random operations
   */
  public FeatureGenerator(MapUtils mapUtils) {
    this.mapUtils = mapUtils;
  }

  /**
   * Generates a lake feature on the terrain.
   *
   * @param terrain the terrain array to modify
   * @param type the terrain type to use for the lake
   * @param bounds the bounding rectangle for the lake
   */
  protected void generateLake(String[][] terrain, String type, Rectangle bounds) {
    int width = terrain.length;
    int height = terrain[0].length;
    Polygon lake = mapUtils.randomPolygon(bounds, 16);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (lake.contains(x, y)) {
          terrain[x][y] = type;
        }
      }
    }
  }

  /**
   * Generates a river feature on the terrain.
   *
   * @param terrain the terrain array to modify
   * @param tiles the tile array
   * @param type the terrain type to use for the river
   * @param size the width of the river
   */
  protected void generateRiver(String[][] terrain, int[][] tiles, String type, int size) {
    int width = terrain.length;
    int height = terrain[0].length;
    boolean direction = mapUtils.randomSource().nextDouble() > 0.5;
    Point[] points = mapUtils.randomRibbon(new Rectangle(width + 1, height + 1), direction);
    Polygon river = generateRiverPolygon(points, size, direction);
    for (int x = 0; x < tiles.length; x++) {
      for (int y = 0; y < tiles[x].length; y++) {
        if (river.contains(x, y)) {
          terrain[x][y] = type;
        }
      }
    }
  }

  private static Polygon generateRiverPolygon(Point[] points, int width, boolean isHorizontal) {
    int[] xPoints = new int[2 * points.length];
    int[] yPoints = new int[2 * points.length];
    for (int i = 0; i < points.length; i++) {
      if (isHorizontal) {
        xPoints[i] = points[i].x;
        xPoints[i + points.length] = points[points.length - 1 - i].x;
        yPoints[i] = points[i].y - width / 2;
        yPoints[i + points.length] = points[points.length - 1 - i].y + width / 2;
      } else {
        yPoints[i] = points[i].y;
        yPoints[i + points.length] = points[points.length - 1 - i].y;
        xPoints[i] = points[i].x - width / 2;
        xPoints[i + points.length] = points[points.length - 1 - i].x + width / 2;
      }
    }
    return new Polygon(xPoints, yPoints, 2 * points.length);
  }
}
