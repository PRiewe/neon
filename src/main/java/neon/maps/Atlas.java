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
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import neon.maps.services.EntityStore;
import neon.maps.services.MapAtlas;
import neon.systems.files.FileSystem;
import neon.util.mapstorage.MapStore;
import neon.util.mapstorage.MapStoreMVStoreAdapter;
import org.h2.mvstore.MVStore;

/**
 * This class keeps track of all loaded maps and their connections.
 *
 * @author mdriesen
 */
@Slf4j
public class Atlas implements Closeable, MapAtlas {
  private final MapStore db;
  private final ConcurrentMap<Integer, Map> maps;
  private final EntityStore entityStore;
  private final FileSystem fileSystem;
  private final MapLoader mapLoader;

  public Atlas(
      FileSystem fileSystem, MapStore mapStore, EntityStore entityStore, MapLoader mapLoader) {
    this.fileSystem = fileSystem;
    this.entityStore = entityStore;
    this.db = mapStore;
    // files.delete(path);
    // String fileName = files.getFullPath(path);
    // log.warn("Creating new MVStore at {}", fileName);
    this.mapLoader = mapLoader;
    // db = MVStore.open(fileName);
    maps = db.openMap("maps");
  }

  /** Initializes this {@code Atlas} with dependency injection. */
  public Atlas(FileSystem fileSystem, String path, EntityStore entityStore, MapLoader mapLoader) {
    this.fileSystem = fileSystem;
    this.entityStore = entityStore;
    this.db = getMapStore(fileSystem, path);
    // files.delete(path);
    // String fileName = files.getFullPath(path);
    // log.warn("Creating new MVStore at {}", fileName);
    this.mapLoader = mapLoader;
    // db = MVStore.open(fileName);
    maps = db.openMap("maps");
  }

  private MapStore getMapStore(FileSystem files, String fileName) {
    files.delete(fileName);

    log.warn("Creating new MVStore at {}", fileName);

    return new MapStoreMVStoreAdapter(MVStore.open(fileName));
  }

  public MapStore getCache() {
    return db;
  }

  /**
   * @param uid the unique identifier of a map
   * @return the map with the given uid
   */
  @Override
  public Map getMap(int uid) {
    if (!maps.containsKey(uid)) {
      if (entityStore.getMapPath(uid) == null) {
        throw new RuntimeException(String.format("No existing mappath for uid %d", uid));
      }
      Map map = mapLoader.load(entityStore.getMapPath(uid), uid);
      System.out.println("Loaded map " + map.toString());
      maps.put(uid, map);
    }
    return maps.get(uid);
  }

  @Override
  public Map getMap(int uid, String... path) {
    Map map = mapLoader.load(path, uid);
    return map;
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
