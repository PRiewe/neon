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

import java.util.Vector;
import neon.resources.Resource;

/**
 * Service interface for resource retrieval. Provides abstraction over the resource management
 * system to reduce coupling.
 *
 * @author mdriesen
 */
public interface ResourceProvider {
  /**
   * Gets a resource by its ID.
   *
   * @param id the resource identifier
   * @return the resource with the given ID
   */
  Resource getResource(String id);

  /**
   * Gets a resource by its ID and type.
   *
   * @param id the resource identifier
   * @param type the resource type
   * @return the resource with the given ID and type
   */
  Resource getResource(String id, String type);

  <T extends Resource> Vector<T> getResources(Class<T> rRecipeClass);
}
