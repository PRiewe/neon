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

package neon.ui.states;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.EventObject;
import neon.core.UIEngineContext;
import neon.core.event.CombatEvent;
import neon.core.event.MagicEvent;
import neon.core.event.TurnEvent;
import neon.core.handlers.*;
import neon.entities.*;
import neon.entities.property.Condition;
import neon.entities.property.Slot;
import neon.resources.CClient;
import neon.resources.RItem;
import neon.resources.RSpell;
import neon.ui.GamePanel;
import neon.util.fsm.State;
import neon.util.fsm.TransitionEvent;
import net.engio.mbassy.bus.MBassador;

public class MoveState extends State implements KeyListener {
  private Player player;
  private GamePanel panel;
  private CClient keys;
  private MBassador<EventObject> bus;
  private final UIEngineContext context;
  private final MotionHandler motionHandler;
  private final TeleportHandler teleportHandler;

  public MoveState(State parent, MBassador<EventObject> bus, UIEngineContext context) {
    super(parent, "move module");
    this.bus = bus;
    this.context = context;
    this.motionHandler = new MotionHandler(context);
    this.teleportHandler = new TeleportHandler(context);
    keys = (CClient) context.getResources().getResource("client", "config");
  }

  @Override
  public void enter(TransitionEvent e) {
    player = context.getPlayer();
    panel = (GamePanel) getVariable("panel");
    panel.addKeyListener(this);
  }

  @Override
  public void exit(TransitionEvent e) {
    panel.removeKeyListener(this);
  }

  private void move(int x, int y) {
    // TODO: this should partially move to MotionHandler?
    Rectangle bounds = player.getShapeComponent();
    Point p = new Point(bounds.x + x, bounds.y + y);

    // check if creature is in the way
    Creature other = context.getAtlas().getCurrentZone().getCreature(p);
    if (other != null && !other.hasCondition(Condition.DEAD)) {
      if (other.brain.isHostile()) {
        bus.publishAsync(new CombatEvent(player, other));
        bus.publishAsync(new TurnEvent(context.getTimer().addTick())); // next turn
      } else {
        bus.publishAsync(new TransitionEvent("bump", "creature", other));
      }
    } else { // no one in the way, so move
      if (motionHandler.move(player, p) == MotionHandler.DOOR) {
        for (long uid : context.getAtlas().getCurrentZone().getItems(p)) {
          if (context.getStore().getEntity(uid) instanceof Door) {
            bus.publishAsync(
                new TransitionEvent("door", "door", context.getStore().getEntity(uid)));
          }
        }
      }
      bus.publishAsync(new TurnEvent(context.getTimer().addTick())); // next turn
    }
  }

  /*
   * things to do when space is used
   */
  private void act() {
    // clone the list here, otherwise concurrent modification exceptions when picking up items
    Rectangle bounds = player.getShapeComponent();
    ArrayList<Long> items =
        new ArrayList<Long>(context.getAtlas().getCurrentZone().getItems(bounds));
    Creature c = context.getAtlas().getCurrentZone().getCreature(bounds.getLocation());
    if (c != null) {
      items.add(c.getUID());
    }

    if (items.size() == 1) {
      Entity entity = context.getStore().getEntity(items.get(0));
      if (entity instanceof Container) {
        Container container = (Container) entity;
        if (container.lock.isLocked()) {
          if (container.lock.hasKey() && hasItem(player, container.lock.getKey())) {
            bus.publishAsync(new TransitionEvent("container", "holder", entity));
          } else {
            bus.publishAsync(new TransitionEvent("lock", "lock", container.lock));
          }
        } else {
          bus.publishAsync(new TransitionEvent("container", "holder", entity));
        }
      } else if (entity instanceof Door) {
        if (teleportHandler.teleport(player, (Door) entity) == MotionHandler.OK) {
          bus.publishAsync(new TurnEvent(context.getTimer().addTick()));
        }
      } else if (entity instanceof Creature) {
        bus.publishAsync(new TransitionEvent("container", "holder", entity));
      } else {
        context.getAtlas().getCurrentZone().removeItem((Item) entity);
        InventoryHandler.addItem(player, entity.getUID());
      }
    } else if (items.size() > 1) {
      bus.publishAsync(
          new TransitionEvent("container", "holder", context.getAtlas().getCurrentZone()));
    }
  }

  public void keyReleased(KeyEvent key) {}

  public void keyTyped(KeyEvent key) {}

  public void keyPressed(KeyEvent key) {
    int code = key.getKeyCode();
    if (code == keys.up) {
      move(0, -1);
    } else if (code == keys.upright) {
      move(1, -1);
    } else if (code == keys.right) {
      move(1, 0);
    } else if (code == keys.downright) {
      move(1, 1);
    } else if (code == keys.down) {
      move(0, 1);
    } else if (code == keys.downleft) {
      move(-1, 1);
    } else if (code == keys.left) {
      move(-1, 0);
    } else if (code == keys.upleft) {
      move(-1, -1);
    } else if (code == keys.wait) {
      move(0, 0);
    } else if (code == keys.act) {
      act();
    } else if (code == keys.look) {
      bus.publishAsync(new TransitionEvent("aim"));
    } else if (code == keys.shoot) {
      bus.publishAsync(new TransitionEvent("aim"));
    } else if (code == keys.talk) {
      bus.publishAsync(new TransitionEvent("aim"));
    } else if (code == keys.unmount) {
      if (player.isMounted()) {
        Creature mount = player.getMount();
        player.unmount();
        context.getAtlas().getCurrentZone().addCreature(mount);
        Rectangle pBounds = player.getShapeComponent();
        Rectangle mBounds = mount.getShapeComponent();
        mBounds.setLocation(pBounds.x, pBounds.y);
      }
    } else if (code == keys.magic) {
      if (player.getMagicComponent().getSpell() != null) {
        RSpell spell = player.getMagicComponent().getSpell();
        if (spell.range > 0) {
          bus.publishAsync(new TransitionEvent("aim"));
        } else {
          bus.publishAsync(new MagicEvent.OnSelf(this, player, spell));
        }
      } else if (player.getInventoryComponent().hasEquiped(Slot.MAGIC)) {
        Item item =
            (Item) context.getStore().getEntity(player.getInventoryComponent().get(Slot.MAGIC));
        if (item.getMagicComponent().getSpell().range > 0) {
          bus.publishAsync(new TransitionEvent("aim"));
        } else {
          bus.publishAsync(new MagicEvent.ItemOnSelf(this, player, item));
        }
      }
    }
  }

  private boolean hasItem(Creature creature, RItem item) {
    for (long uid : creature.getInventoryComponent()) {
      if (context.getStore().getEntity(uid).getID().equals(item.id)) {
        return true;
      }
    }
    return false;
  }
}
