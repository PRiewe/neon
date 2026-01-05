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

package neon.maps.services;

import neon.entities.Entity;

/**
 * Service interface for entity storage and retrieval. Provides abstraction over the entity storage
 * system to reduce coupling.
 *
 * @author mdriesen
 */
public interface EntityStore {
  /**
   * Retrieves an entity by its unique identifier.
   *
   * @param uid the unique identifier of the entity
   * @return the entity with the given UID
   */
  Entity getEntity(long uid);

  /**
   * Adds an entity to the store.
   *
   * @param entity the entity to add
   */
  void addEntity(Entity entity);

  /**
   * Creates a new unique identifier for an entity.
   *
   * @return a new unique entity UID
   */
  long createNewEntityUID();

  /**
   * Creates a new unique identifier for a map.
   *
   * @return a new unique map UID
   */
  int createNewMapUID();

  /**
   * Gets the file path for a map with the given UID.
   *
   * @param uid the map UID
   * @return the file path for the map
   */
  String[] getMapPath(int uid);
}
