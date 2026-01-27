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

package neon.entities;

import java.awt.Rectangle;
import java.util.*;
import neon.ai.*;
import neon.core.GameContext;
import neon.core.handlers.InventoryHandler;
import neon.entities.components.FactionComponent;
import neon.entities.property.Gender;
import neon.resources.*;
import neon.util.Dice;

public class EntityFactory {
  private final AIFactory aiFactory;
  private final GameContext gameContext;
  private final ItemFactory itemFactory;

  public EntityFactory(GameContext gameContext) {
    this.gameContext = gameContext;
    this.aiFactory = new AIFactory(gameContext);
    this.itemFactory = new ItemFactory(gameContext);
  }

  public Item getItem(String id, long uid) {
    return itemFactory.getItem(id, -1, -1, uid);
  }

  public Item getItem(String id, int x, int y, long uid) {
    return itemFactory.getItem(id, x, y, uid);
  }

  /*
   * Returns a person with the given uid, position and properties.
   */
  private Creature getPerson(String id, int x, int y, long uid, RCreature species) {
    String name = id;
    RPerson person = (RPerson) gameContext.getResources().getResource(id);
    if (person.name != null) {
      name = person.name;
    }
    Creature creature = new Hominid(id, uid, name, species, Gender.OTHER);
    Rectangle bounds = creature.getShapeComponent();
    bounds.setLocation(x, y);
    for (String i : person.items) {
      long itemUID = gameContext.getStore().createNewEntityUID();
      Item item = getItem(i, itemUID);
      gameContext.getStore().addEntity(item);
      InventoryHandler.addItem(creature, itemUID);
    }
    for (String s : person.spells) {
      creature.getMagicComponent().addSpell(neon.magic.SpellFactory.getSpell(s));
    }
    FactionComponent factions = creature.getFactionComponent();
    for (String f : person.factions.keySet()) {
      factions.addFaction(f, person.factions.get(f));
    }
    for (String script : person.scripts) {
      creature.getScriptComponent().addScript(script);
    }
    return creature;
  }

  public Creature getCreature(String id, int x, int y, long uid) {
    Creature creature;
    Resource resource = gameContext.getResources().getResource(id);
    if (resource instanceof RPerson rp) {
      RCreature species = (RCreature) gameContext.getResources().getResource(rp.species);
      creature = getPerson(id, x, y, uid, species);
      creature.brain = aiFactory.getAI(creature, rp);
    } else if (resource instanceof LCreature lc) {
      ArrayList<String> creatures = new ArrayList<String>(lc.creatures.keySet());
      return getCreature(creatures.get(Dice.roll(1, creatures.size(), -1)), x, y, uid);
    } else {
      RCreature rc = (RCreature) gameContext.getResources().getResource(id);
      switch (rc.type) {
        case construct -> creature = new Construct(id, uid, rc);
        case humanoid -> creature = new Hominid(id, uid, rc);
        case daemon -> creature = new Daemon(id, uid, rc);
        case dragon -> creature = new Dragon(id, uid, rc);
        case goblin -> creature = new Hominid(id, uid, rc);
        default -> creature = new Creature(id, uid, rc);
      }

      // positie
      Rectangle bounds = creature.getShapeComponent();
      bounds.setLocation(x, y);

      // brain
      creature.brain = aiFactory.getAI(creature);
    }

    return creature;
  }
}
