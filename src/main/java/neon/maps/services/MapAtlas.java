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

package neon.maps.services;

import neon.maps.Map;

/**
 * Interface for map atlas operations. Provides abstraction over map retrieval to reduce coupling
 * between generators and the concrete Atlas implementation.
 *
 * @author mdriesen
 */
public interface MapAtlas {
  /**
   * Gets a map by its UID.
   *
   * @param uid the map unique identifier
   * @return the map with the given UID
   */
  Map getMap(int uid);
}
