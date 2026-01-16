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

package neon.ai;

import neon.entities.Creature;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.components.HealthComponent;
import neon.resources.ResourceManager;

public class BasicAI extends AI {
  public BasicAI(
      Creature creature,
      byte aggression,
      byte confidence,
      ResourceManager resourceManager,
      UIDStore uidStore) {
    super(creature, aggression, confidence, resourceManager, uidStore);
  }

  public void act() {
    // TODO: not only pay attention to player, but also to other creatures in sight
    if (isHostile() && sees(uidStore.getPlayer())) {
      HealthComponent health = creature.getHealthComponent();
      if (100 * health.getHealth() / health.getBaseHealth() < confidence) {
        // 80% chance to just flee, 20% chance to heal; if no heal spell, flee anyway
        if (Math.random() > 0.2 || !(cure() || heal())) {
          flee(uidStore.getPlayer());
        }
      } else {
        hunt(uidStore.getPlayer());
      }
    } else {
      wander();
    }
  }
}
