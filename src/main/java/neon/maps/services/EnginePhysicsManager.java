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
import neon.core.Engine;
import neon.entities.components.PhysicsComponent;
import neon.maps.Region;

/**
 * Adapter implementation of PhysicsManager that delegates to the Engine singleton. This class
 * provides a bridge during the transition to dependency injection.
 *
 * @author mdriesen
 */
public class EnginePhysicsManager implements PhysicsManager {
  @Override
  public void clear() {
    Engine.getPhysicsEngine().clear();
  }

  @Override
  public void register(Region region, Rectangle bounds, boolean fixed) {
    Engine.getPhysicsEngine().register(region, bounds, fixed);
  }

  @Override
  public void register(PhysicsComponent component) {
    Engine.getPhysicsEngine().register(component.getTheBody());
  }
}
