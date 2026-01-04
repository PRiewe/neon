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

import neon.resources.RZoneTheme;
import org.mapdb.DB;

/**
 * Factory for creating Zone instances with proper dependency injection. Eliminates the constructor
 * side effect of accessing Engine.getAtlas().getCache().
 *
 * @author mdriesen
 */
public class ZoneFactory {
  private final DB cache;

  /**
   * Creates a new ZoneFactory with the given cache database.
   *
   * @param cache the MapDB cache database for spatial indices
   */
  public ZoneFactory(DB cache) {
    this.cache = cache;
  }

  /**
   * Creates a new zone with the given parameters.
   *
   * @param name the zone name
   * @param mapUID the UID of the map containing this zone
   * @param index the zone index within its map
   * @return a new Zone instance
   */
  public Zone createZone(String name, int mapUID, int index) {
    return Zone.create(name, mapUID, index, cache);
  }

  /**
   * Creates a new zone with a theme for random generation.
   *
   * @param name the zone name
   * @param mapUID the UID of the map containing this zone
   * @param theme the zone theme for random generation
   * @param index the zone index within its map
   * @return a new Zone instance with a theme
   */
  public Zone createZone(String name, int mapUID, RZoneTheme theme, int index) {
    return Zone.create(name, mapUID, theme, index, cache);
  }
}
