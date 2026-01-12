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
import neon.core.handlers.InventoryHandler;
import neon.entities.components.Enchantment;
import neon.entities.components.FactionComponent;
import neon.entities.components.RenderComponent;
import neon.entities.components.ShapeComponent;
import neon.entities.property.Gender;
import neon.magic.SpellFactory;
import neon.resources.*;
import neon.ui.graphics.shapes.JVShape;
import neon.ui.graphics.svg.SVGLoader;
import neon.util.Dice;

public class EntityFactory {
  private final AIFactory aiFactory = new AIFactory();

  private final InventoryHandler inventoryHandler;
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;
  private final SpellFactory spellFactory;

  public EntityFactory(UIDStore uidStore, ResourceManager resourceManager) {
    this.uidStore = uidStore;
    this.resourceManager = resourceManager;
    inventoryHandler = new InventoryHandler(uidStore);
    spellFactory = new SpellFactory(resourceManager);
  }

  public Item getItem(String id, long uid) {
    Item item = getItem(id, -1, -1, uid);
    return item;
  }

  public Item getItem(String id, int x, int y, long uid) {
    // item aanmaken
    RItem resource;
    if (resourceManager.getResource(id) instanceof LItem) {
      LItem li = (LItem) resourceManager.getResource(id);
      ArrayList<String> items = new ArrayList<String>(li.items.keySet());
      resource = (RItem) resourceManager.getResource(items.get(Dice.roll(1, items.size(), -1)));
    } else {
      resource = (RItem) resourceManager.getResource(id);
    }
    Item item = getItem(resource, uid);

    // positie
    ShapeComponent itemBounds = item.getShapeComponent();
    itemBounds.setLocation(x, y);
    RenderComponent renderer = item.getRenderComponent();
    renderer.setZ(resource.top ? Byte.MAX_VALUE : Byte.MAX_VALUE - 2);

    if (resource.svg != null) { // svg custom look gedefinieerd
      JVShape shape = SVGLoader.loadShape(resource.svg);
      shape.setX(x);
      shape.setY(y);
      shape.setZ(renderer.getZ());
      item.setRenderComponent(shape);
      itemBounds.setWidth(shape.getBounds().width);
      itemBounds.setHeight(shape.getBounds().height);
    }

    if (resource.spell != null) {
      int mana = 0;
      if (resource instanceof RWeapon) {
        mana = ((RWeapon) resource).mana;
      }
      item.setMagicComponent(
          new Enchantment(spellFactory.getSpell(resource.spell), mana, item.getUID()));
    }

    return item;
  }

  private static Item getItem(RItem resource, long uid) {
    // item aanmaken
    return switch (resource.type) {
      case container -> new Container(uid, (RItem.Container) resource);
      case food -> new Item.Food(uid, resource);
      case aid -> new Item.Aid(uid, resource);
      case book -> new Item.Book(uid, (RItem.Text) resource);
      case clothing -> new Clothing(uid, (RClothing) resource);
      case armor -> new Armor(uid, (RClothing) resource);
      case coin -> new Item.Coin(uid, resource);
      case door -> new Door(uid, resource);
      case light -> new Item.Light(uid, resource);
      case potion -> new Item.Potion(uid, resource);
      case scroll -> new Item.Scroll(uid, (RItem.Text) resource);
      case weapon -> new Weapon(uid, (RWeapon) resource);
      default -> new Item(uid, resource);
    };
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
      Item item = getItem(i, itemUID);
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
