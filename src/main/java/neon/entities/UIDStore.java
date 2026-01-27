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

package neon.entities;

import java.io.*;
import neon.maps.services.EntityStore;
import org.h2.mvstore.MVStore;

/**
 * This class stores the UIDs of every object, map and mod currently in the game. It can give out
 * new UIDs to objects created during gameplay. Positive UIDs are used in resources loaded from a
 * mod. Negative UIDs are reserved for random generation.
 *
 * @author mdriesen
 */
public class UIDStore extends AbstractUIDStore implements Closeable, EntityStore {

  // uid database
  private final MVStore uidDb;

  /**
   * Tells this UIDStore to use the given jdbm3 cache.
   *
   * @param file
   */
  public UIDStore(String file) {
    uidDb = MVStore.open(file);
    objects = uidDb.openMap("object");
    mods = uidDb.openMap("mods");
  }

  /**
   * Adds an object to the list.
   *
   * @param entity the object to be added
   */
  @Override
  public void addEntity(Entity entity) {
    super.addEntity(entity);
    if (objects.size() % 1000 == 0) { // do a commit every 1000 entities
      uidDb.commit();
    }
  }

  /**
   * @return the jdbm3 cache used by this UIDStore
   */
  public MVStore getCache() {
    return uidDb;
  }

  @Override
  public void close() throws IOException {
    uidDb.commit();
    uidDb.close();
  }
}
