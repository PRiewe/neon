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
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.Collection;
import neon.core.GameStores;
import neon.entities.*;
import neon.entities.CreatureFactory;
import neon.entities.ItemFactory;
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
import neon.resources.ResourceManager;
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
  private final ItemFactory itemFactory;
  private final CreatureFactory creatureFactory;
  private final EntityStore entityStore;
  private final ResourceProvider resourceProvider;
  private final Dice dice;
  private final WildernessTerrainGenerator wildernessTerrainGenerator;

  /**
   * Creates a wilderness generator with dependency injection for engine use.
   *
   * @param zone the zone to generate
   * @param entityStore the entity store service
   * @param resourceProvider the resource provider service
   */
  public WildernessGenerator(Zone zone, GameStores gameStores, Player gameContext) {
    this(zone, gameStores, gameContext, new MapUtils(), new Dice());
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
      Zone zone, GameStores gameStores, Player gameContext, MapUtils mapUtils, Dice dice) {
    this.zone = zone;
    this.entityStore = gameStores.getStore();
    this.resourceProvider = gameStores.getResources();
    this.dice = dice;
    this.wildernessTerrainGenerator = new WildernessTerrainGenerator(mapUtils, dice);
    this.itemFactory = new ItemFactory(gameStores.getResources());
    this.creatureFactory = new CreatureFactory(gameStores, gameContext);
  }

  public WildernessGenerator(
      Zone zone, ResourceManager resourceManeger, UIDStore uidStore, Player gameContext) {
    this(zone, resourceManeger, uidStore, gameContext, new MapUtils(), new Dice());
  }

  public WildernessGenerator(
      Zone zone,
      ResourceManager resourceManeger,
      UIDStore uidStore,
      Player gameContext,
      MapUtils mapUtils,
      Dice dice) {
    this.zone = zone;
    this.entityStore = uidStore;
    this.resourceProvider = resourceManeger;
    this.dice = dice;
    this.wildernessTerrainGenerator = new WildernessTerrainGenerator(mapUtils, dice);
    this.itemFactory = new ItemFactory(resourceManeger);
    this.creatureFactory = new CreatureFactory(resourceManeger, uidStore, gameContext);
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
      String[][] terrain, GameStores gameStores, Player gameContext, MapUtils mapUtils, Dice dice) {
    this.terrain = terrain;
    this.entityStore = gameStores.getStore();
    this.resourceProvider = gameStores.getResources();
    this.dice = dice;
    this.wildernessTerrainGenerator = new WildernessTerrainGenerator(mapUtils, dice);
    this.itemFactory = new ItemFactory(gameStores.getResources());
    this.creatureFactory = new CreatureFactory(gameStores, gameContext);
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
      terrain =
          wildernessTerrainGenerator.generateTerrain(
              region.getWidth(), region.getHeight(), theme, region.getTextureType());

      // add vegetation if needed
      wildernessTerrainGenerator.addVegetation(
          region.getWidth(), region.getHeight(), theme, region.getTextureType(), terrain);

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
            creatureFactory.getCreature(id, rx + x, ry + y, entityStore.createNewEntityUID());
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
              Item item = itemFactory.getItem(id, region.getX() + i, region.getY() + j, uid);
              entityStore.addEntity(item);
              if (item instanceof Container) {
                for (String s : ((RItem.Container) item.resource).contents) {
                  Item content = itemFactory.getItem(s, entityStore.createNewEntityUID());
                  ((Container) item).addItem(content.getUID());
                  entityStore.addEntity(content);
                }
              }
              zone.addItem(item);
            } else if (entry.startsWith("c:")) {
              String id = entry.replace("c:", "");
              long uid = entityStore.createNewEntityUID();
              Creature creature =
                  creatureFactory.getCreature(id, region.getX() + i, region.getY() + j, uid);
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
}
