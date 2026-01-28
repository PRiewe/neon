/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - mdriesen
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
import lombok.extern.slf4j.Slf4j;
import neon.core.Engine;
import neon.core.GameContext;
import neon.core.event.CombatEvent;
import neon.core.event.MagicEvent;
import neon.entities.Creature;
import neon.entities.Item;
import neon.entities.Weapon;
import neon.entities.components.HealthComponent;
import neon.entities.property.Slot;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * This class handles combat between two creatures. There are four possible outcomes of a fight:
 *
 * <ul>
 *   <li>ATTACK - a succesful attack
 *   <li>DIE - succesful attack, the target died
 *   <li>DODGE- the target dodged the attack
 *   <li>BLOCK - the target blocked the attack
 * </ul>
 *
 * @author mdriesen
 */
@Listener(references = References.Strong) // strong, to avoid gc
@Slf4j
public class CombatHandler {
  private final CombatUtils combatUtils;
  private final GameContext context;
  private final InventoryHandler inventoryHandler;

  public CombatHandler(GameContext context) {
    this.context = context;
    combatUtils = new CombatUtils(context.getStore());
    inventoryHandler = new InventoryHandler(context);
  }

  @Handler
  public void handleCombat(CombatEvent ce) {
    log.trace("handleCombat {}", ce);
    if (!ce.isFinished()) {
      int result =
          switch (ce.getType()) {
            case CombatEvent.SHOOT -> shoot(ce.getAttacker(), ce.getDefender());
            case CombatEvent.FLING -> fling(ce.getAttacker(), ce.getDefender());
            default -> fight(ce.getAttacker(), ce.getDefender());
          };
      Engine.post(new CombatEvent(ce.getAttacker(), ce.getDefender(), result));
    }
  }

  /*
   * This method lets two creatures fight.
   *
   * @param attacker the attacking creature
   * @param defender the defending creature
   * @return the outcome of the fight
   */
  private int fight(Creature attacker, Creature defender) {
    long uid = attacker.getInventoryComponent().get(Slot.WEAPON);
    Weapon weapon = (Weapon) context.getStore().getEntity(uid);
    return fight(attacker, defender, weapon);
  }

  /*
   * Lets a creature shoot at a creature. The ammo used to shoot with is
   * removed from the attacker's inventory.
   *
   * @param shooter	the attacking creature
   * @param target	the target creature
   * @return			the outcome of the fight
   */
  private int shoot(Creature shooter, Creature target) {
    // damage is average of arrow and bow (Creature.getAV)
    Weapon ammo =
        (Weapon) context.getStore().getEntity(shooter.getInventoryComponent().get(Slot.AMMO));
    inventoryHandler.removeItem(shooter, ammo.getUID());
    for (long uid : shooter.getInventoryComponent()) {
      Item item = (Item) context.getStore().getEntity(uid);
      if (item.getID().equals(ammo.getID())) {
        inventoryHandler.equip(item, shooter);
        break;
      }
    }

    long uid = shooter.getInventoryComponent().get(Slot.WEAPON);
    Weapon weapon = (Weapon) context.getStore().getEntity(uid);
    return fight(shooter, target, weapon);
  }

  /*
   * Lets a humanoid throw something at a creature. The weapon thrown is removed
   * from the attacker's inventory.
   *
   * @param thrower	the attacking creature
   * @param target	the target creature
   * @return			the outcome of the fight
   */
  private int fling(Creature thrower, Creature target) {
    Weapon weapon =
        (Weapon) context.getStore().getEntity(thrower.getInventoryComponent().get(Slot.AMMO));
    inventoryHandler.removeItem(thrower, weapon.getUID());
    for (long uid : thrower.getInventoryComponent()) {
      Item item = (Item) context.getStore().getEntity(uid);
      if (item.getID().equals(weapon.getID())) {
        inventoryHandler.equip(item, thrower);
        break;
      }
    }
    return fight(thrower, target, weapon);
  }

  private int fight(Creature attacker, Creature defender, Weapon weapon) {
    // attacker determines an attack value (depends on dex)
    int attack = combatUtils.attack(attacker);

    int result;

    // defender checks if they can dodge or block
    if (combatUtils.dodge(defender) < attack) {
      if (combatUtils.block(defender) < attack) {
        if (weapon != null) {
          weapon.setState(weapon.getState() - 1);
        }

        // Attack Value, dependent on weapon, skill and str
        int AV = combatUtils.getAV(attacker);
        // defense value, dependent on armor, skill
        int DV = combatUtils.getDV(defender);

        // always minimum 1 damage
        HealthComponent health = defender.getHealthComponent();
        health.heal(Math.min(-1, -(int) ((AV - DV) / (DV + 1))));

        // cast enchanted weapon spell
        if (weapon != null && weapon.getMagicComponent().getSpell() != null) {
          Rectangle bounds = defender.getShapeComponent();
          Engine.post(new MagicEvent.ItemOnPoint(this, attacker, weapon, bounds.getLocation()));
        }

        // determine messages
        if (health.getHealth() < 0) {
          result = CombatEvent.DIE;
        } else {
          result = CombatEvent.ATTACK;
        }
      } else { // blocked
        result = CombatEvent.BLOCK;
      }
    } else { // dodged
      result = CombatEvent.DODGE;
    }

    return result;
  }
}
