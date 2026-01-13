/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2024 - Maarten Driesen
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

import neon.core.GameStores;
import neon.entities.UIDStore;
import neon.resources.RZoneTheme;
import neon.resources.ResourceManager;
import neon.util.mapstorage.MapStore;
import neon.util.spatial.RTree;

/**
 * Factory for creating Zone instances with proper dependency injection. Eliminates the constructor
 * side effect of accessing Engine.getAtlas().getCache().
 *
 * @author mdriesen
 */
public class ZoneFactory {
  private final MapStore cache;
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;

  /**
   * Creates a new ZoneFactory with the given cache database.
   *
   * @param cache the MapDB cache database for spatial indices
   */
  public ZoneFactory(MapStore cache, UIDStore uidStore, ResourceManager resourceManager) {
    this.cache = cache;
    this.uidStore = uidStore;
    this.resourceManager = resourceManager;
  }

  public ZoneFactory(GameStores gameStore) {
    this(gameStore.getAtlas().getCache(), gameStore.getStore(), gameStore.getResources());
  }

  public Zone createZone(String name, int map, int index) {
    RTree<Region> regions = new RTree<>(100, 40, cache, map + ":" + index);
    return new Zone(name, map, index, uidStore, resourceManager, regions);
  }

  public Zone createZoneWithTheme(String name, int map, int index, RZoneTheme theme) {
    RTree<Region> regions = new RTree<>(100, 40, cache, map + ":" + index);
    return new Zone(name, map, theme, index, uidStore, resourceManager, regions);
  }
}
