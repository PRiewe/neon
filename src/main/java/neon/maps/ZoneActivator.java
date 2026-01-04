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

import neon.entities.Player;
import neon.maps.services.PhysicsManager;

/**
 * Manages the activation of zones by registering regions and entities with the physics system.
 * Extracted from Atlas to follow single responsibility principle.
 *
 * @author mdriesen
 */
public class ZoneActivator {
  private final PhysicsManager physicsManager;
  private final Player player;

  /**
   * Creates a new ZoneActivator with the given dependencies.
   *
   * @param physicsManager the physics system manager
   * @param player the player entity
   */
  public ZoneActivator(PhysicsManager physicsManager, Player player) {
    this.physicsManager = physicsManager;
    this.player = player;
  }

  /**
   * Activates a zone by clearing the physics system and registering all active regions and the
   * player.
   *
   * @param zone the zone to activate
   */
  public void activateZone(Zone zone) {
    physicsManager.clear();

    // Register all active regions with the physics system
    for (Region region : zone.getRegions()) {
      if (region.isActive()) {
        physicsManager.register(region, region.getBounds(), true);
      }
    }

    // Re-register the player
    physicsManager.register(player.getPhysicsComponent());
  }
}
