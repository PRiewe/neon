/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
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
import java.awt.Rectangle;
import neon.core.GameContext;
import neon.core.event.MagicEvent;
import neon.entities.Creature;
import neon.entities.components.ShapeComponent;
import neon.entities.property.Skill;
import neon.resources.RSpell;
import neon.util.Dice;

public class HuntBehaviour implements Behaviour {
  private final Creature creature;
  private final Creature prey;
  private final GameContext gameContext;
  private final PathFinder pathFinder;

  public HuntBehaviour(Creature hunter, Creature prey, GameContext gameContext) {
    creature = hunter;
    this.prey = prey;
    this.gameContext = gameContext;
    this.pathFinder = new PathFinder(gameContext);
  }

  public void act() {
    int dice = Dice.roll(1, 2, 0);
    Rectangle creaturePos = creature.getShapeComponent();
    Rectangle preyPos = prey.getShapeComponent();

    if (dice == 1) {
      int time = gameContext.getTimer().getTime();
      for (RSpell.Power power : creature.getMagicComponent().getPowers()) {
        if (power.effect.getSchool().equals(Skill.DESTRUCTION)
            && creature.getMagicComponent().canUse(power, time)
            && power.range >= Point.distance(creaturePos.x, creaturePos.y, preyPos.x, preyPos.y)) {
          creature.getMagicComponent().equipSpell(power);
          ShapeComponent bounds = prey.getShapeComponent();
          gameContext.post(new MagicEvent.CreatureOnPoint(this, creature, bounds.getLocation()));
          return; // abort hunt as soon as a spell is cast
        }
      }
      for (RSpell spell : creature.getMagicComponent().getSpells()) {
        if (spell.effect.getSchool().equals(Skill.DESTRUCTION)
            && spell.range >= Point.distance(creaturePos.x, creaturePos.y, preyPos.x, preyPos.y)) {
          creature.getMagicComponent().equipSpell(spell);
          ShapeComponent bounds = prey.getShapeComponent();
          gameContext.post(new MagicEvent.CreatureOnPoint(this, creature, bounds.getLocation()));
          return; // abort hunt as soon as a spell is cast
        }
      }
    }

    Point p;
    if (creature.getStatsComponent().getInt()
        < 5) { // if creature is clumsy, just try the shortest path
      int dx = 0;
      int dy = 0;
      if (creaturePos.x < preyPos.x) {
        dx = 1;
      } else if (creaturePos.x > preyPos.x) {
        dx = -1;
      }
      if (creaturePos.y < preyPos.y) {
        dy = 1;
      } else if (creaturePos.y > preyPos.y) {
        dy = -1;
      }
      p = new Point(creaturePos.x + dx, creaturePos.y + dy);
    } else { // if creature is smarter, try A*
      ShapeComponent cBounds = creature.getShapeComponent();
      ShapeComponent pBounds = prey.getShapeComponent();
      p = pathFinder.findPath(creature, cBounds.getLocation(), pBounds.getLocation())[0];
    }

    if (p.distance(preyPos.x, preyPos.y) < 1) {
      //			if(creature.inventory.hasEquiped(Slot.WEAPON) && creature.getWeapon().isRanged()) {
      //				if(!(creature.inventory.getWeaponType().equals(WeaponType.THROWN) || equip(Slot.AMMO)))
      // {
      //					InventoryHandler.unequip(creature.getWeapon().getUID(), creature);
      //				}
      //			} else if(!creature.inventory.hasEquiped(Slot.WEAPON)) {
      //				equip(Slot.WEAPON);
      //			}
      //			Engine.post(new CombatEvent(creature, prey));
      //		} else if(Atlas.getCurrentZone().getCreature(p) == null) {
      //			if(MotionHandler.move(creature, p) == MotionHandler.DOOR) {
      //				open(p);	// open door if necessary
      //			}
    } else { // if another creature is in the way
      //			wander();
    }
  }
}
