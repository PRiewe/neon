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

package neon.core.handlers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.core.event.MagicEvent;
import neon.core.event.MagicTask;
import neon.entities.Creature;
import neon.entities.Item;
import neon.entities.components.Characteristics;
import neon.entities.components.Enchantment;
import neon.entities.property.Ability;
import neon.entities.property.Condition;
import neon.entities.property.Skill;
import neon.magic.*;
import neon.resources.RSpell;
import neon.util.Dice;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/** This class handles all magic casting. */
@Listener(references = References.Strong) // strong, om gc te vermijden
@Slf4j
public class MagicHandler {
  public static final int RANGE = 0; // verkeerde range
  public static final int NULL = 1; // geen target geselecteerd
  public static final int MANA = 2; // te weinig mana of charge (voor items)
  public static final int TARGET = 3; // verkeerd type target (item of creature)
  public static final int SKILL = 4; // skill check niet gelukt
  public static final int NONE = 5; // geen spell equiped
  public static final int OK = 6; // casten ok
  public static final int LEVEL = 7; // spell te moeilijk
  public static final int SILENCED = 8; // caster silenced
  public static final int INTERVAL = 9; // power interval niet gedaan

  private final MagicUtils magicUtils;
  private final CombatUtils combatUtils;
  private final InventoryHandler inventoryHandler;
  private final SkillHandler skillHandler;
  private final GameContext gameContext;

  public MagicHandler(GameContext gameContext) {
    this.gameContext = gameContext;
    this.combatUtils = new CombatUtils(gameContext);
    this.magicUtils = new MagicUtils(gameContext);
    this.inventoryHandler = new InventoryHandler(gameContext);
    this.skillHandler = new SkillHandler(gameContext);
  }

  /**
   * Casts a spell on a target creature. Used by traps.
   *
   * @param me
   */
  @Handler
  public void cast(MagicEvent.OnCreature me) {
    log.trace("cast OnCreature {}", me);
    castSpell(me.getTarget(), null, me.getSpell());
  }

  /**
   * Casts a spell on a target point. Used by traps.
   *
   * @param me
   */
  @Handler
  public void cast(MagicEvent.OnPoint me) {
    log.trace("cast OnPoint {}", me);
    RSpell spell = me.getSpell();
    Point target = me.getTarget();

    int radius = spell.radius;
    Rectangle box =
        new Rectangle(target.x - radius, target.y - radius, radius * 2 + 1, radius * 2 + 1);

    if (spell.effect == Effect.SCRIPTED) {
      gameContext.execute(spell.script);
    } else if (spell.effect.getHandler().onItem()) {
      Collection<Long> items = gameContext.getAtlas().getCurrentZone().getItems(box);
      for (long uid : items) {
        castSpell((Item) gameContext.getStore().getEntity(uid), spell);
      }
    } else {
      Collection<Creature> creatures = gameContext.getAtlas().getCurrentZone().getCreatures(box);
      for (Creature creature : creatures) {
        castSpell(creature, null, spell);
      }
      if (box.contains(gameContext.getPlayer().getShapeComponent())) {
        castSpell(gameContext.getPlayer(), null, spell);
      }
    }
  }

  /**
   * This method lets a creature cast a spell on a target.
   *
   * @return the result of the casting
   */
  @Handler
  public void cast(MagicEvent.CreatureOnPoint me) {
    log.trace("cast CreatureOnPoint {}", me);
    Creature caster = me.getCaster();
    Point target = me.getTarget();
    Rectangle bounds = caster.getShapeComponent();
    RSpell formula = caster.getMagicComponent().getSpell();

    if (formula == null) {
      // geen spell/enchantment beschikbaar
      gameContext.post(new MagicEvent.Result(this, caster, NONE));
    } else if (caster.hasCondition(Condition.SILENCED)) {
      // gesilenced
      gameContext.post(new MagicEvent.Result(this, caster, SILENCED));
    } else if (target.distance(bounds.getLocation()) > formula.range) {
      // out of range
      gameContext.post(new MagicEvent.Result(this, caster, RANGE));
    } else {
      if (formula instanceof RSpell.Power) {
        int time = gameContext.getTimer().getTime();
        if (caster.getMagicComponent().canUse((RSpell.Power) formula, time)) {
          caster.getMagicComponent().usePower((RSpell.Power) formula, time);
        } else { // te kort geleden power gecast
          gameContext.post(new MagicEvent.Result(this, caster, INTERVAL));
        }
      } else {
        int penalty = checkPenalty(caster);
        int check = caster.getSkill(formula.effect.getSchool());
        if (check < MagicUtils.getLevel(formula)) {
          // spell level te hoog
          gameContext.post(new MagicEvent.Result(this, caster, LEVEL));
        } else if (!formula.effect.equals(Effect.SCRIPTED)
            && magicUtils.check(caster, formula) < 20 + penalty) {
          // skill check gefaald
          gameContext.post(new MagicEvent.Result(this, caster, SKILL));
        } else if (caster.getMagicComponent().getMana() < MagicUtils.getMana(formula)) {
          // genoeg mana om te casten?
          gameContext.post(new MagicEvent.Result(this, caster, MANA));
        } else {
          caster.getMagicComponent().addMana(-MagicUtils.getMana(formula));
        }
      }

      // gebied dat door de spel geraakt wordt
      int area = formula.radius;
      Rectangle box = new Rectangle(target.x - area, target.y - area, area * 2 + 1, area * 2 + 1);

      // alle items/creatures binnen bereik
      if (formula.effect == Effect.SCRIPTED) {
        gameContext.execute(formula.script);
      } else if (formula.effect.getHandler().onItem()) {
        Collection<Long> items = gameContext.getAtlas().getCurrentZone().getItems(box);
        for (long uid : items) {
          castSpell((Item) gameContext.getStore().getEntity(uid), formula);
        }
      } else {
        Collection<Creature> creatures = gameContext.getAtlas().getCurrentZone().getCreatures(box);
        if (box.contains(gameContext.getPlayer().getShapeComponent())) {
          creatures.add(gameContext.getPlayer());
        }
        for (Creature creature : creatures) {
          castSpell(creature, caster, formula);
        }
      }

      // en resultaat posten
      gameContext.post(new MagicEvent.Result(this, caster, OK));
    }
  }

  /**
   * This methods lets a creature using a magic item cast a spell on a point.
   *
   * @return the result of the cast
   */
  @Handler
  public void cast(MagicEvent.ItemOnPoint me) {
    log.trace("cast ItemOnPoint {}", me);
    Creature caster = me.getCaster();
    Item item = me.getItem();
    Point target = me.getTarget();
    Rectangle bounds = caster.getShapeComponent();
    Enchantment enchantment = item.getMagicComponent();
    RSpell formula = null;

    if (item instanceof Item.Scroll) {
      formula = enchantment.getSpell();
    }

    if (formula == null) {
      gameContext.post(new MagicEvent.Result(this, caster, NONE));
    } else if (!(item instanceof Item.Scroll)
        && MagicUtils.getMana(formula) > enchantment.getMana()) {
      gameContext.post(new MagicEvent.Result(this, caster, MANA));
    } else if (target == null) {
      gameContext.post(new MagicEvent.Result(this, caster, NULL));
    } else if (formula.range < target.distance(bounds.getLocation())) {
      gameContext.post(new MagicEvent.Result(this, caster, RANGE));
    } else {
      if (item instanceof Item.Scroll) {
        inventoryHandler.removeItem(caster, item.getUID());
      } else {
        enchantment.addMana(-MagicUtils.getMana(formula));
      }

      int area = formula.radius;
      Rectangle box = new Rectangle(target.x - area, target.y - area, area * 2 + 1, area * 2 + 1);

      if (formula.effect == Effect.SCRIPTED) {
        gameContext.execute(formula.script);
      } else if (formula.effect.getHandler().onItem()) {
        Collection<Long> items = gameContext.getAtlas().getCurrentZone().getItems(box);
        for (long uid : items) {
          castSpell((Item) gameContext.getStore().getEntity(uid), formula);
        }
      } else {
        Collection<Creature> creatures = gameContext.getAtlas().getCurrentZone().getCreatures(box);
        if (box.contains(gameContext.getPlayer().getShapeComponent())) {
          creatures.add(gameContext.getPlayer());
        }
        for (Creature creature : creatures) {
          castSpell(creature, caster, formula);
        }
      }

      // en resultaat posten
      gameContext.post(new MagicEvent.Result(this, caster, OK));
    }
  }

  /**
   * This methods lets a creature using a magic item cast a spell on itself.
   *
   * @return the result of the cast
   */
  @Handler
  public void cast(MagicEvent.ItemOnSelf me) {
    log.trace("cast ItemOnSelf {}", me);
    Item item = me.getItem();
    Creature caster = me.getCaster();

    Enchantment enchantment = item.getMagicComponent();
    RSpell formula = null;

    if (item instanceof Item.Scroll) {
      formula = enchantment.getSpell();
    }

    if (formula == null) {
      gameContext.post(new MagicEvent.Result(this, caster, NONE));
    } else if (!(item instanceof Item.Scroll)
        && MagicUtils.getMana(formula) > enchantment.getMana()) {
      gameContext.post(new MagicEvent.Result(this, caster, MANA));
    } else if (formula.range > 0) {
      gameContext.post(new MagicEvent.Result(this, caster, RANGE));
    } else {
      enchantment.addMana(-MagicUtils.getMana(formula));
      if (item instanceof Item.Scroll) {
        inventoryHandler.removeItem(caster, item.getUID());
      }
      gameContext.post(new MagicEvent.Result(this, caster, castSpell(caster, caster, formula)));
    }
  }

  /**
   * Lets a creature cast a spell on itself.
   *
   * @param me
   * @return the result of the cast
   */
  @Handler
  public void cast(MagicEvent.OnSelf me) {
    log.trace("cast OnSelf {}", me);
    Creature caster = me.getCaster();
    RSpell spell = me.getSpell();

    if (caster.hasCondition(Condition.SILENCED)) {
      gameContext.post(new MagicEvent.Result(this, caster, SILENCED));
    } else if (spell == null) {
      gameContext.post(new MagicEvent.Result(this, caster, NONE));
    } else if (spell.range > 0) {
      gameContext.post(new MagicEvent.Result(this, caster, RANGE));
    } else {
      if (spell instanceof RSpell.Power) {
        int time = gameContext.getTimer().getTime();
        if (caster.getMagicComponent().canUse((RSpell.Power) spell, time)) {
          caster.getMagicComponent().usePower((RSpell.Power) spell, time);
          castSpell(caster, caster, spell);
          gameContext.post(new MagicEvent.Result(this, caster, OK));
        } else { // te kort geleden power gecast
          gameContext.post(new MagicEvent.Result(this, caster, INTERVAL));
        }
      } else {
        int penalty = checkPenalty(caster);
        if (caster.getSkill(spell.effect.getSchool()) < MagicUtils.getLevel(spell)) {
          gameContext.post(new MagicEvent.Result(this, caster, LEVEL));
        } else if (!spell.effect.equals(Effect.SCRIPTED)
            && magicUtils.check(caster, spell) < 20 + penalty) {
          gameContext.post(new MagicEvent.Result(this, caster, SKILL));
        } else if (caster.getMagicComponent().getMana() < MagicUtils.getMana(spell)) {
          gameContext.post(new MagicEvent.Result(this, caster, MANA));
        } else {
          caster.getMagicComponent().addMana(-MagicUtils.getMana(spell));
          castSpell(caster, caster, spell);
          gameContext.post(new MagicEvent.Result(this, caster, OK));
        }
      }
    }
  }

  private int castSpell(Item target, RSpell formula) {
    if (formula.effect.getHandler().onItem()) {
      Spell spell = new Spell(formula, 0, target, null);
      spell.getHandler().addEffect(spell);
      return OK;
    } else {
      return TARGET;
    }
  }

  private int castSpell(Creature target, Creature caster, RSpell formula) {
    Characteristics chars = target.getCharacteristicsComponent();
    int penalty = 0;

    if (chars.hasAbility(Ability.SPELL_ABSORPTION)) {
      penalty += chars.getAbility(Ability.SPELL_ABSORPTION);
      target.getMagicComponent().addMana(MagicUtils.getMana(formula) * penalty / 100);
    }
    if (chars.hasAbility(Ability.SPELL_RESISTANCE)) {
      penalty += chars.getAbility(Ability.SPELL_RESISTANCE);
    }
    if (chars.hasAbility(Ability.FIRE_RESISTANCE) && formula.effect == Effect.FIRE_DAMAGE) {
      penalty += chars.getAbility(Ability.FIRE_RESISTANCE);
    }
    if (chars.hasAbility(Ability.COLD_RESISTANCE) && formula.effect == Effect.FROST_DAMAGE) {
      penalty += chars.getAbility(Ability.COLD_RESISTANCE);
    }
    if (chars.hasAbility(Ability.SHOCK_RESISTANCE) && formula.effect == Effect.SHOCK_DAMAGE) {
      penalty += chars.getAbility(Ability.SHOCK_RESISTANCE);
    }
    float mod = 1 - penalty / 100;

    Spell spell = new Spell(formula, mod, target, caster);
    spell.getHandler().addEffect(spell);

    if (formula.duration > 0) {
      target.addActiveSpell(spell);
      int time = gameContext.getTimer().getTime();
      MagicTask task = new MagicTask(spell, time + formula.duration, gameContext);
      gameContext.getTaskSubmissionQueue().add(task, time, 1, time + formula.duration);
    }

    return OK;
  }

  /*
   * Calculates all penalties related to spellcasting
   */
  private int checkPenalty(Creature caster) {
    int penalty = 0;

    // wearing armor
    if (combatUtils.getDV(caster) > caster.species.dv) {
      penalty += 10;
    }

    return penalty;
  }

  /**
   * Lets a creature eat food.
   *
   * @param eater
   * @param food
   * @return
   */
  public void eat(Creature eater, Item.Food food) {
    Enchantment enchantment = food.getMagicComponent();
    int check = Math.max(1, skillHandler.check(eater, Skill.ALCHEMY) / 10);
    RSpell spell =
        new RSpell(
            "",
            0,
            Dice.roll(1, check, 0),
            enchantment.getSpell().effect.name(),
            1,
            Dice.roll(1, check, 0),
            "spell");
    castSpell(eater, eater, spell);
  }

  /**
   * Lets a creature drink a potion.
   *
   * @param drinker
   * @param potion
   * @return
   */
  public void drink(Creature drinker, Item.Potion potion) {
    Enchantment enchantment = potion.getMagicComponent();
    RSpell spell = enchantment.getSpell();
    castSpell(drinker, drinker, spell);
  }
}
