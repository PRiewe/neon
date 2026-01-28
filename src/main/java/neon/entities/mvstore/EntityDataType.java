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

package neon.entities.mvstore;

import java.nio.ByteBuffer;
import neon.entities.Entity;
import neon.entities.serialization.EntitySerializerFactory;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

/**
 * MVStore DataType implementation for Entity serialization. This class delegates
 * serialization/deserialization to EntityFactory.
 *
 * @author mdriesen
 */
public class EntityDataType extends BasicDataType<Entity> {
  private final EntitySerializerFactory entitySerializerFactory;

  public EntityDataType(EntitySerializerFactory entitySerializerFactory) {
    this.entitySerializerFactory = entitySerializerFactory;
  }

  @Override
  public int getMemory(Entity obj) {
    // Return 0 for now - can be optimized later if needed
    return 0;
  }

  @Override
  public void write(WriteBuffer buff, Entity obj) {
    entitySerializerFactory.writeEntityToWriteBuffer(buff, obj);
  }

  @Override
  public Entity read(ByteBuffer buff) {
    return entitySerializerFactory.readEntityFromByteBuffer(buff);
  }

  @Override
  public Entity[] createStorage(int size) {
    return new Entity[size];
  }
}
