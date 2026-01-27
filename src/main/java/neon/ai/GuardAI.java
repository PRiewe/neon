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

import java.awt.Point;
import neon.core.UIEngineContext;
import neon.entities.Creature;
import neon.entities.components.HealthComponent;
import neon.entities.components.ShapeComponent;

public class GuardAI extends AI {
  private int range;
  private Point home;

  public GuardAI(
      Creature creature,
      byte aggression,
      byte confidence,
      int range,
      UIEngineContext uiEngineContext) {
    super(creature, aggression, confidence, uiEngineContext);
    this.range = range;
    ShapeComponent bounds = creature.getShapeComponent();
    home = new Point(bounds.x, bounds.y);
  }

  public void act() {
    // TODO: not only pay attention to player, but also to other creatures in sight
    ShapeComponent cBounds = creature.getShapeComponent();
    ShapeComponent pBounds = uiEngineContext.getPlayer().getShapeComponent();
    if (isHostile() && cBounds.getLocation().distance(pBounds.getLocation()) < range) {
      HealthComponent health = creature.getHealthComponent();
      if (100 * health.getHealth() / health.getBaseHealth() < confidence / 100) {
        // 80% chance to just flee, 20% chance to heal; if no heal spell, flee anyway
        if (Math.random() > 0.2 || !(cure() || heal())) {
          flee(uiEngineContext.getPlayer());
        }
      } else {
        hunt(range, home, uiEngineContext.getPlayer());
      }
    } else {
      wander(range, home);
    }
  }
}
