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
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

/**
 * MVStore DataType implementation for Long keys.
 *
 * @author mdriesen
 */
public class LongDataType extends BasicDataType<Long> {

  public static final LongDataType INSTANCE = new LongDataType();

  private static final Long[] EMPTY_LONG_ARR = new Long[0];

  private LongDataType() {}

  @Override
  public int getMemory(Long obj) {
    return 8;
  }

  @Override
  public void write(WriteBuffer buff, Long data) {
    buff.putVarLong(data);
  }

  @Override
  public Long read(ByteBuffer buff) {
    return DataUtils.readVarLong(buff);
  }

  @Override
  public Long[] createStorage(int size) {
    return size == 0 ? EMPTY_LONG_ARR : new Long[size];
  }

  @Override
  public int compare(Long one, Long two) {
    return Long.compare(one, two);
  }
}
