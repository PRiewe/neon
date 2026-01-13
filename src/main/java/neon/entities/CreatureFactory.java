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
import neon.core.GameStores;
import neon.core.handlers.InventoryHandler;
import neon.entities.components.FactionComponent;
import neon.entities.property.Gender;
import neon.magic.SpellFactory;
import neon.resources.*;
import neon.util.Dice;

public class CreatureFactory {
  private final AIFactory aiFactory;
  private final InventoryHandler inventoryHandler;
  private final SpellFactory spellFactory;
  // private final GameStores gameStores;
  private final ItemFactory itemFactory;
  private final ResourceManager resourceManager;
  private final UIDStore uidStore;

  public CreatureFactory(GameStores gameStores, Player player) {
    inventoryHandler = new InventoryHandler(gameStores.getStore());
    spellFactory = new SpellFactory(gameStores.getResources());
    this.resourceManager = gameStores.getResources();
    this.uidStore = gameStores.getStore();
    this.aiFactory = new AIFactory(gameStores.getResources(), gameStores.getStore(), player);
    this.itemFactory = new ItemFactory(gameStores.getResources());
  }

  public CreatureFactory(ResourceManager resourceManager, UIDStore uidStore, Player player) {
    inventoryHandler = new InventoryHandler(uidStore);
    spellFactory = new SpellFactory(resourceManager);
    this.resourceManager = resourceManager;
    this.uidStore = uidStore;
    this.aiFactory = new AIFactory(resourceManager, uidStore, player);
    this.itemFactory = new ItemFactory(resourceManager);
  }

  /*
   * Returns a person with the given uid, position and properties.
   */
  private Creature getPerson(String id, int x, int y, long uid, RCreature species) {
    String name = id;
    RPerson person = (RPerson) resourceManager.getResource(id);
    if (person.name != null) {
      name = person.name;
    }
    Creature creature = new Hominid(id, uid, name, species, Gender.OTHER);
    Rectangle bounds = creature.getShapeComponent();
    bounds.setLocation(x, y);
    for (String i : person.items) {
      long itemUID = uidStore.createNewEntityUID();
      Item item = itemFactory.getItem(i, itemUID);
      uidStore.addEntity(item);
      inventoryHandler.addItem(creature, itemUID);
    }
    for (String s : person.spells) {
      creature.getMagicComponent().addSpell(spellFactory.getSpell(s));
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
    Resource resource = resourceManager.getResource(id);
    if (resource instanceof RPerson) {
      RPerson rp = (RPerson) resource;
      RCreature species = (RCreature) resourceManager.getResource(rp.species);
      creature = getPerson(id, x, y, uid, species);
      creature.brain = aiFactory.getAI(creature, rp);
    } else if (resource instanceof LCreature) {
      LCreature lc = (LCreature) resource;
      ArrayList<String> creatures = new ArrayList<String>(lc.creatures.keySet());
      return getCreature(creatures.get(Dice.roll(1, creatures.size(), -1)), x, y, uid);
    } else {
      RCreature rc = (RCreature) resourceManager.getResource(id);
      creature =
          switch (rc.type) {
            case construct -> new Construct(id, uid, rc);
            case humanoid, goblin -> new Hominid(id, uid, rc);
            case daemon -> new Daemon(id, uid, rc);
            case dragon -> new Dragon(id, uid, rc);
            default -> new Creature(id, uid, rc);
          };

      // positie
      Rectangle bounds = creature.getShapeComponent();
      bounds.setLocation(x, y);

      // brain
      creature.brain = aiFactory.getAI(creature);
    }

    return creature;
  }
}
