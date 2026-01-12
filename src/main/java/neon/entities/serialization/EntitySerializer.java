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

package neon.entities.serialization;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import neon.core.GameStores;
import neon.entities.Creature;
import neon.entities.Entity;
import neon.entities.Item;

public class EntitySerializer {

  private final ItemSerializer itemSerializer;
  private final CreatureSerializer creatureSerializer;

  EntitySerializer(GameStores gameStores) {
    itemSerializer = new ItemSerializer(gameStores);
    creatureSerializer = new CreatureSerializer(gameStores);
  }

  public Entity deserialize(DataInput input) throws IOException {
      return switch (input.readUTF()) {
          case "item" -> itemSerializer.deserialize(input);
          case "creature" -> creatureSerializer.deserialize(input);
          default -> null;
      };
  }

  public void serialize(DataOutput output, Entity entity) throws IOException {
    if (entity instanceof Item) {
      output.writeUTF("item");
      itemSerializer.serialize(output, (Item) entity);
    } else if (entity instanceof Creature) {
      output.writeUTF("creature");
      creatureSerializer.serialize(output, (Creature) entity);
    }
  }
}
