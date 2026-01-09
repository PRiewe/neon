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
import java.util.ArrayList;
import neon.entities.Door;
import neon.entities.EntityFactory;
import neon.maps.MapUtils;
import neon.maps.Region;
import neon.maps.Zone;
import neon.maps.services.EntityStore;
import neon.maps.services.ResourceProvider;
import neon.resources.RRegionTheme;
import neon.resources.RTerrain;

/**
 * This class generates random towns.
 *
 * @author mdriesen
 */
public class TownGenerator {
  private final Zone zone;
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;
  private final MapUtils mapUtils;

  /**
   * Creates a town generator with dependency injection. Uses default (non-deterministic) random
   * number generation.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   */
  public TownGenerator(Zone zone, EntityStore entityStore, ResourceProvider resourceProvider) {
    this(zone, entityStore, resourceProvider, new MapUtils());
  }

  /**
   * Creates a town generator with dependency injection and custom random sources for deterministic
   * testing.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   * @param mapUtils the map utilities with configured random source
   * @param dice the dice roller with configured random source
   */
  public TownGenerator(
      Zone zone, EntityStore entityStore, ResourceProvider resourceProvider, MapUtils mapUtils) {
    this.zone = zone;
    this.entityStore = entityStore;
    this.resourceProvider = resourceProvider;
    this.mapUtils = mapUtils;
  }

  /**
   * Generates a town cell with the given properties.
   *
   * @param width the width
   * @param height the height
   * @param x the x coordinate
   * @param y the y coordinate
   */
  public void generate(int x, int y, int width, int height, RRegionTheme theme, int layer) {
    ArrayList<Rectangle> temp1;

    // region verdelen in willekeurig rechthoeken
    if (theme.id.equals("town_big")) {
      temp1 = BlocksGenerator.generateBSPRectangles(width / 2, height / 2, 4, 8);
    } else if (theme.id.equals("town_small")) {
      temp1 = BlocksGenerator.generatePackedRectangles(width / 2, height / 2, 4, 8, 2, 10);
    } else {
      temp1 = BlocksGenerator.generateSparseRectangles(width / 2, height / 2, 4, 8, 2, 20);
    }

    for (Rectangle r : temp1) {
      if (r != null) {
        RTerrain wall = (RTerrain) resourceProvider.getResource(theme.wall, "terrain");
        Region house =
            new Region(
                wall.id,
                x + width / 4 + r.x,
                y + height / 4 + r.y,
                r.width - 1,
                r.height - 1,
                null,
                (byte) (layer + 1),
                wall);
        makeDoor(house, theme);
        zone.addRegion(house);
      }
    }
  }

  private void makeDoor(Region r, RRegionTheme theme) {
    // sneak in a door somewhere
    int x = 0, y = 0;

    y =
        switch (mapUtils.random(0, 3)) {
          case 0 -> {
            x = r.getX() + 1;
            yield r.getY();
          }
          case 1 -> {
            x = r.getX() + 1;
            yield r.getHeight() + r.getY() - 1;
          }
          case 2 -> {
            x = r.getX();
            yield r.getY() + 1;
          }
          case 3 -> {
            x = r.getWidth() + r.getX() - 1;
            yield r.getY() + 1;
          }
          default -> y;
        };

    long uid = entityStore.createNewEntityUID();
    Door door = (Door) EntityFactory.getItem(theme.door, x, y, uid);
    entityStore.addEntity(door);
    door.lock.close();
    zone.addItem(door);
    RTerrain rt = (RTerrain) resourceProvider.getResource(theme.floor, "terrain");
    zone.addRegion(new Region(theme.floor, x, y, 1, 1, null, r.getZ() + 1, rt));
  }
}
