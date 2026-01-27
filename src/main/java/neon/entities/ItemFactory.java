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

import java.util.*;
import neon.ai.*;
import neon.core.UIStorage;
import neon.entities.components.Enchantment;
import neon.entities.components.RenderComponent;
import neon.entities.components.ShapeComponent;
import neon.magic.SpellFactory;
import neon.resources.*;
import neon.ui.graphics.shapes.JVShape;
import neon.ui.graphics.svg.SVGLoader;
import neon.util.Dice;

public class ItemFactory {
  private final UIStorage dataStore;

  public ItemFactory(UIStorage dataStore) {
    this.dataStore = dataStore;
  }

  public Item getItem(String id, long uid) {
    Item item = getItem(id, -1, -1, uid);
    return item;
  }

  public Item getItem(String id, int x, int y, long uid) {
    // item aanmaken
    RItem resource;
    if (dataStore.getResources().getResource(id) instanceof LItem) {
      LItem li = (LItem) dataStore.getResources().getResource(id);
      ArrayList<String> items = new ArrayList<String>(li.items.keySet());
      resource =
          (RItem) dataStore.getResources().getResource(items.get(Dice.roll(1, items.size(), -1)));
    } else {
      resource = (RItem) dataStore.getResources().getResource(id);
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
          new Enchantment(SpellFactory.getSpell(resource.spell), mana, item.getUID()));
    }

    return item;
  }

  private Item getItem(RItem resource, long uid) {
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
}
