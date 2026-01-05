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

import java.awt.Rectangle;
import neon.entities.components.PhysicsComponent;
import neon.maps.Region;

/**
 * Service interface for physics system management. Provides abstraction over the physics system to
 * reduce coupling.
 *
 * @author mdriesen
 */
public interface PhysicsManager {
  /** Clears all registered physics objects. */
  void clear();

  /**
   * Registers a region with the physics system.
   *
   * @param region the region to register
   * @param bounds the bounding rectangle of the region
   * @param fixed whether the region is fixed in place
   */
  void register(Region region, Rectangle bounds, boolean fixed);

  /**
   * Registers a physics component with the physics system.
   *
   * @param component the physics component to register
   */
  void register(PhysicsComponent component);
}
