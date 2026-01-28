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

import java.awt.Rectangle;
import javax.swing.SwingConstants;
import neon.core.GameContext;
import neon.core.event.MessageEvent;
import neon.entities.Creature;
import neon.entities.Door;
import neon.entities.Entity;
import neon.maps.*;

/**
 * This class takes care of all motion-related actions. Walking, climbing, swimming and teleporting
 * can be handled.
 *
 * @author mdriesen
 */
public class TeleportHandler {
  public static final byte OK = 0;
  public static final byte BLOCKED = 1;
  public static final byte SWIM = 2;
  public static final byte CLIMB = 3;
  public static final byte DOOR = 4;
  public static final byte NULL = 5;
  public static final byte HABITAT = 6;
  public final GameContext gameContext;
  public final MapLoader mapLoader;
  public final MotionHandler motionHandler;

  public TeleportHandler(GameContext gameContext) {
    this.gameContext = gameContext;
    this.motionHandler = new MotionHandler(gameContext);
    this.mapLoader = new MapLoader(gameContext);
  }

  /**
   * Teleports a creature. Two results are possible:
   *
   * <ul>
   *   <li>OK - creature was teleported
   *   <li>DOOR - this portal is just a door and does not support teleporting
   * </ul>
   *
   * @param creature the creature to teleport.
   * @param door the portal that the creature used
   * @return the result
   */
  public byte teleport(Creature creature, Door door) {
    if (door.portal.isPortal()) {
      Zone previous = gameContext.getAtlas().getCurrentZone(); // briefly buffer current zone
      if (door.portal.getDestMap() != 0) {
        // load map and have door refer back
        Map map = gameContext.getAtlas().getMap(door.portal.getDestMap());
        Zone zone = map.getZone(door.portal.getDestZone());
        for (long uid : zone.getItems(door.portal.getDestPos())) {
          Entity i = gameContext.getStore().getEntity(uid);
          if (i instanceof Door) {
            ((Door) i).portal.setDestMap(gameContext.getAtlas().getCurrentMap());
          }
        }
        gameContext.getAtlas().setCurrentMap(map);
        gameContext.getScriptEngine().getBindings().putMember("map", map);
        door.portal.setDestMap(gameContext.getAtlas().getCurrentMap());
      } else if (door.portal.getDestTheme() != null) {
        Dungeon dungeon = mapLoader.loadDungeon(door.portal.getDestTheme());
        gameContext.getAtlas().setCurrentMap(dungeon);
        door.portal.setDestMap(gameContext.getAtlas().getCurrentMap());
      }

      gameContext.getAtlas().enterZone(door, previous);

      motionHandler.walk(creature, door.portal.getDestPos());
      // check if there is a door at the destination, if so, unlock and open this door
      Rectangle bounds = creature.getShapeComponent();
      for (long uid : gameContext.getAtlas().getCurrentZone().getItems(bounds)) {
        Entity i = gameContext.getStore().getEntity(uid);
        if (i instanceof Door) {
          ((Door) i).lock.open();
        }
      }

      // if there is a sign on the door, show it now
      if (door.hasSign()) {
        gameContext.post(new MessageEvent(door, door.toString(), 3, SwingConstants.BOTTOM));
      }
      return OK;
    }
    return DOOR;
  }
}
