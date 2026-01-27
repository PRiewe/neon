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

package neon.core.handlers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.entities.Creature;
import neon.entities.Door;
import neon.entities.Entity;
import neon.entities.components.Lock;
import neon.entities.property.Condition;
import neon.entities.property.Habitat;
import neon.entities.property.Skill;
import neon.maps.*;

/**
 * This class takes care of all motion-related actions. Walking, climbing, swimming and teleporting
 * can be handled.
 *
 * @author mdriesen
 */
@Slf4j
public class MotionHandler {
  public static final byte OK = 0;
  public static final byte BLOCKED = 1;
  public static final byte SWIM = 2;
  public static final byte CLIMB = 3;
  public static final byte DOOR = 4;
  public static final byte NULL = 5;
  public static final byte HABITAT = 6;
  public final GameContext gameContext;
  public final MapLoader mapLoader;

  public MotionHandler(GameContext gameContext) {
    this.gameContext = gameContext;
    this.mapLoader = new MapLoader(gameContext);
  }

  /**
   * Lets a creature move (walking, climbing or swimming). The possible results are:
   *
   * <ul>
   *   <li>OK - creature could move
   *   <li>NULL - the point this creature wanted to move to doesn't exist
   *   <li>SWIM - the creature wanted to swim, but failed a swim check
   *   <li>CLIMB - the creature wanted to climb, but failed a climb check
   *   <li>BLOCKED - the point this creature wanted to move to was blocked
   *   <li>DOOR - the point this creature wanted to move to is blocked by a closed door
   *   <li>HABITAT - creature tried to move to the wrong habitat type
   * </ul>
   *
   * @param actor the creature that wants to move
   * @param p the point the creature wants to move to
   * @return the result of the movement
   */
  public byte move(Creature actor, Point p) {
    Region region = gameContext.getAtlas().getCurrentZone().getRegion(p);
    if (p == null || region == null) {
      return NULL;
    }

    // check if there is no closed door present
    Collection<Long> items = gameContext.getAtlas().getCurrentZone().getItems(p);
    for (long uid : items) {
      Entity i = gameContext.getStore().getEntity(uid);
      if (i instanceof Door) {
        if (((Door) i).lock.getState() != Lock.OPEN) {
          return DOOR;
        }
      }
    }

    // determine ground type:
    Region.Modifier mov = region.getMovMod();

    // check if actor is levitating/flying
    if (actor.hasCondition(Condition.LEVITATE) || actor.species.habitat == Habitat.AIR) {
      if (mov != Region.Modifier.BLOCK) {
        mov = Region.Modifier.NONE;
      }
    }

    return switch (mov) {
      case NONE -> walk(actor, p);
      case SWIM -> swim(actor, p);
      case CLIMB -> climb(actor, p);
      default -> BLOCKED;
    };
  }

  /**
   * Lets a creature move.
   *
   * @param creature
   * @param x
   * @param y
   * @return the result of the movement
   */
  public byte move(Creature creature, int x, int y) {
    return move(creature, new Point(x, y));
  }

  private byte swim(Creature swimmer, Point p) {
    if (swimmer.species.habitat == Habitat.WATER) {
      return OK;
    } else if (SkillHandler.check(swimmer, Skill.SWIMMING) > 20) {
      Rectangle bounds = swimmer.getShapeComponent();
      bounds.setLocation(p.x, p.y);
      return OK;
    } else {
      return SWIM;
    }
  }

  /*
   * Method to climb. The skill check
   * must be greater than 25 (more terrain varieties later).
   *
   * @param tile
   */
  private static byte climb(Creature climber, Point p) {
    if (climber.species.habitat == Habitat.WATER) {
      return HABITAT;
    }
    if (SkillHandler.check(climber, Skill.CLIMBING) > 25) {
      Rectangle bounds = climber.getShapeComponent();
      bounds.setLocation(p.x, p.y);
      return OK;
    } else {
      return CLIMB;
    }
  }

  static byte walk(Creature walker, Point p) {
    Rectangle bounds = walker.getShapeComponent();
    if (walker.species.habitat == Habitat.WATER) {
      return HABITAT;
    } else {
      bounds.setLocation(p.x, p.y);
      return OK;
    }
  }
}
