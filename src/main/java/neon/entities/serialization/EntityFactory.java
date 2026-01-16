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

import java.io.IOException;
import java.nio.ByteBuffer;
import neon.entities.*;
import neon.entities.mvstore.ByteBufferDataInput;
import neon.entities.mvstore.ByteBufferDataOutput;
import neon.resources.ResourceManager;
import org.h2.mvstore.WriteBuffer;

/**
 * Factory class that orchestrates entity serialization/deserialization for MVStore. This class
 * bridges existing DataInput/DataOutput-based serializers to MVStore's WriteBuffer/ByteBuffer
 * format.
 *
 * @author mdriesen
 */
public class EntityFactory {
  private static final int ITEM_TYPE = 1;
  private static final int CREATURE_TYPE = 2;

  private final ItemSerializer itemSerializer;
  private final CreatureSerializer creatureSerializer;

  /**
   * Constructor for full functionality (used by Engine).
   *
   * @param gameStores the game stores providing access to resources and entities
   * @param gameContext the game context for AI initialization (can be null for write-only
   *     operations)
   */
  public EntityFactory(ResourceManager resourceManager, UIDStore uidStore) {
    this.itemSerializer = new ItemSerializer(resourceManager);
    this.creatureSerializer = new CreatureSerializer(resourceManager, uidStore);
  }

  /**
   * Serializes an entity to MVStore's WriteBuffer format.
   *
   * @param out the WriteBuffer to write to
   * @param entity the entity to serialize
   */
  public void writeEntityToWriteBuffer(WriteBuffer out, Entity entity) {
    try {
      if (entity instanceof Item) {
        out.putInt(ITEM_TYPE);
        ByteBufferDataOutput adapter = new ByteBufferDataOutput(out);
        itemSerializer.serialize(adapter, (Item) entity);
      } else if (entity instanceof Creature) {
        out.putInt(CREATURE_TYPE);
        ByteBufferDataOutput adapter = new ByteBufferDataOutput(out);
        creatureSerializer.serialize(adapter, (Creature) entity);
      } else {
        throw new IllegalArgumentException("Unknown entity type: " + entity.getClass());
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize entity", e);
    }
  }

  /**
   * Deserializes an entity from MVStore's ByteBuffer format.
   *
   * @param in the ByteBuffer to read from
   * @return the deserialized entity
   */
  public Entity readEntityFromByteBuffer(ByteBuffer in) {
    try {
      int type = in.getInt();
      ByteBufferDataInput adapter = new ByteBufferDataInput(in);
      return switch (type) {
        case ITEM_TYPE -> itemSerializer.deserialize(adapter);
        case CREATURE_TYPE -> creatureSerializer.deserialize(adapter);
        default -> throw new IllegalStateException("Unknown entity type: " + type);
      };
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize entity", e);
    }
  }
}
