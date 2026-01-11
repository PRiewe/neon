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

import java.io.Closeable;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import neon.maps.services.EngineEntityStore;
import neon.maps.services.EntityStore;
import neon.maps.services.MapAtlas;
import neon.systems.files.FileSystem;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * This class keeps track of all loaded maps and their connections.
 *
 * @author mdriesen
 */
@Slf4j
public class Atlas implements Closeable, MapAtlas {
  private final MVStore db;
  private final MVMap<Integer, Map> maps;
  private final EntityStore entityStore;
  private final FileSystem fileSystem;

  /**
   * Initializes this {@code Atlas} with the given {@code FileSystem} and cache path. The cache is
   * lazy initialised.
   *
   * @param fileSystem a {@code FileSystem}
   * @param path the path to the file used for caching
   * @deprecated Use {@link #Atlas(FileSystem, String, EntityStore, ZoneActivator)} to avoid
   *     dependency on Engine singleton
   */
  @Deprecated
  public Atlas(FileSystem fileSystem, String path) {
    this(fileSystem, path, new EngineEntityStore());
  }

  /**
   * Initializes this {@code Atlas} with dependency injection.
   *
   * @param fileSystem the file system
   * @param atlasStore the MVStore for caching
   * @param entityStore the entity store service
   */
  public Atlas(FileSystem fileSystem, String path, EntityStore entityStore) {
    this.fileSystem = fileSystem;
    this.entityStore = entityStore;
    this.db = getMVStore(fileSystem, path);
    // files.delete(path);
    // String fileName = files.getFullPath(path);
    // log.warn("Creating new MVStore at {}", fileName);

    // db = MVStore.open(fileName);
    maps = db.openMap("maps");
  }

  private static MVStore getMVStore(FileSystem files, String fileName) {
    files.delete(fileName);

    log.warn("Creating new MVStore at {}", fileName);

    return MVStore.open(fileName);
  }

  public MVStore getCache() {
    return db;
  }

  /**
   * @param uid the unique identifier of a map
   * @return the map with the given uid
   */
  @Override
  public Map getMap(int uid) {
    if (!maps.containsKey(uid)) {
      Map map = MapLoader.loadMap(entityStore.getMapPath(uid), uid, fileSystem);
      System.out.println("Loaded map " + map.toString());
      maps.put(uid, map);
    }
    return maps.get(uid);
  }

  public void putMapIfNeeded(Map map) {
    if (!maps.containsKey(map.getUID())) {
      // could be a random map that's not in the database yet
      maps.put(map.getUID(), map);
    }
  }

  @Override
  public void close() throws IOException {
    db.close();
  }
}
