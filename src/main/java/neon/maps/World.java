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

package neon.maps;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the surface of the game world. It can be seamlessly traversed.
 *
 * @author mdriesen
 */
public class World implements Map {
  @Getter @Setter private String name;
  private int uid;
  private Zone zone;

  /**
   * Initializes this {@code World} with the given parameters.
   *
   * @param name the name of this map
   * @param uid the uid of this map
   */
  public World(String name, int uid) {
    this.name = name;
    this.uid = uid;
  }

  public World() {}

  public Zone getZone(int i) {
    return zone;
  }

  public int getUID() {
    return uid;
  }

  public Collection<Zone> getZones() {
    return List.of(zone);
  }

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    name = in.readUTF();
    uid = in.readInt();
    zone = (Zone) in.readObject();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(name);
    out.writeInt(uid);
    out.writeObject(zone);
  }
}
