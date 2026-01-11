/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2013 - Maarten Driesen
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

package neon.resources;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;

public class CGame extends Resource {
  private ArrayList<String> playableRaces = new ArrayList<>();
  private ArrayList<String> startingItems = new ArrayList<>();
  private ArrayList<String> startingSpells = new ArrayList<>();
  @Getter private Point startPosition = new Point(0, 0);
  @Setter @Getter private String[] startMap;
  @Setter @Getter private int startZone = 0; // default

  public CGame(String id, String... path) {
    super(id, path);
  }

  @Override
  public void load() {}

  @Override
  public void unload() {}

  public Collection<String> getStartingItems() {
    return startingItems;
  }

  public Collection<String> getStartingSpells() {
    return startingSpells;
  }

  public Collection<String> getPlayableRaces() {
    return playableRaces;
  }
}
