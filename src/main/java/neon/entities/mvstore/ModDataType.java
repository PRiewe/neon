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

import java.io.Serializable;
import java.nio.ByteBuffer;
import neon.maps.mvstore.MVUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

/**
 * MVStore DataType implementation for Mod records. A Mod represents a loaded game modification with
 * a UID and name.
 *
 * @author mdriesen
 */
public class ModDataType extends BasicDataType<ModDataType.Mod> {

  @Override
  public int getMemory(Mod obj) {
    return 2 + (obj.name() == null ? 0 : obj.name().length() * 2);
  }

  @Override
  public void write(WriteBuffer buff, Mod obj) {
    buff.putShort(obj.uid());
    MVUtils.writeString(buff, obj.name());
  }

  @Override
  public Mod read(ByteBuffer buff) {
    short uid = buff.getShort();
    String name = MVUtils.readString(buff);
    return new Mod(uid, name);
  }

  @Override
  public Mod[] createStorage(int size) {
    return new Mod[size];
  }

  /**
   * Record representing a game modification (mod). Package-private to allow UIDStore to use it.
   *
   * @param uid the unique identifier for this mod
   * @param name the name of the mod
   */
  public record Mod(short uid, String name) implements Serializable {}
}
