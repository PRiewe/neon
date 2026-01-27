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
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

/**
 * MVStore DataType implementation for Short keys.
 *
 * @author mdriesen
 */
public class ShortDataType extends BasicDataType<Short> {

  public static final ShortDataType INSTANCE = new ShortDataType();

  private static final Short[] EMPTY_SHORT_ARR = new Short[0];

  private ShortDataType() {}

  @Override
  public int getMemory(Short obj) {
    return 2;
  }

  @Override
  public void write(WriteBuffer buff, Short data) {
    buff.putShort(data);
  }

  @Override
  public Short read(ByteBuffer buff) {
    return buff.getShort();
  }

  @Override
  public Short[] createStorage(int size) {
    return size == 0 ? EMPTY_SHORT_ARR : new Short[size];
  }

  @Override
  public int compare(Short one, Short two) {
    return Short.compare(one, two);
  }
}
