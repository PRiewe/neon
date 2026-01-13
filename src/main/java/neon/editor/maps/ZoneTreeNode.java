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

package neon.editor.maps;

import javax.swing.tree.*;
import lombok.Getter;
import lombok.Setter;
import neon.editor.DataStore;
import neon.editor.resources.RZone;

@SuppressWarnings("serial")
public class ZoneTreeNode extends DefaultMutableTreeNode {
  @Getter private RZone zone;
  @Setter @Getter private EditablePane pane;
  private int level;
  private final DataStore dataStore;

  /**
   * Initializes a new node representing a map level.
   *
   * @param level
   */
  public ZoneTreeNode(int level, RZone zone, DataStore dataStore) {
    this.level = level;
    this.zone = zone;
    this.dataStore = dataStore;
  }

  public String toString() {
    if (!zone.getPath()[0].equals(dataStore.getActive().get("id"))) {
      // niet-actieve data is cursief weergegeven
      return "<html><i>" + zone.name + "</i></html>";
    } else {
      return zone.name;
    }
  }

  public int getZoneLevel() {
    return level;
  }

  public short getUID() {
    return zone.map.getUID();
  }
}
