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
import neon.core.Engine;
import neon.entities.Door;
import neon.maps.generators.DungeonGenerator;
import neon.maps.services.EngineEntityStore;
import neon.maps.services.EntityStore;
import neon.systems.files.FileSystem;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * This class keeps track of all loaded maps and their connections.
 *
 * @author mdriesen
 */
@Slf4j
public class Atlas implements Closeable {
  private final MVStore db;
  private final MVMap<Integer, Map> maps;
  private int currentZone = 0;
  private int currentMap = 0;
  private final FileSystem files;
  private final EntityStore entityStore;
  private final ZoneActivator zoneActivator;

  /**
   * Initializes this {@code Atlas} with the given {@code FileSystem} and cache path. The cache is
   * lazy initialised.
   *
   * @param files a {@code FileSystem}
   * @param path the path to the file used for caching
   * @deprecated Use {@link #Atlas(FileSystem, String, EntityStore, ZoneActivator)} to avoid
   *     dependency on Engine singleton
   */
  @Deprecated
  public Atlas(FileSystem files, String path) {
    this(files, getMVStore(files, path), new EngineEntityStore(), createDefaultZoneActivator());
  }

  /**
   * Initializes this {@code Atlas} with dependency injection.
   *
   * @param entityStore the entity store service
   * @param zoneActivator the zone activator for physics management
   */
  public Atlas(
      FileSystem files, MVStore atlasStore, EntityStore entityStore, ZoneActivator zoneActivator) {
    this.files = files;
    this.entityStore = entityStore;
    this.zoneActivator = zoneActivator;
    this.db = atlasStore;
    // files.delete(path);
    // String fileName = files.getFullPath(path);
    // log.warn("Creating new MVStore at {}", fileName);

    // db = MVStore.open(fileName);
    maps = atlasStore.openMap("maps");
  }

  private static MVStore getMVStore(FileSystem files, String path) {
    files.delete(path);
    String fileName = files.getFullPath(path);
    log.warn("Creating new MVStore at {}", fileName);

    return MVStore.open(fileName);
  }

  /**
   * Creates a default zone activator using Engine singleton (for backward compatibility).
   *
   * @return a zone activator
   */
  static ZoneActivator createDefaultZoneActivator() {
    return new ZoneActivator(new neon.maps.services.EnginePhysicsManager(), Engine::getPlayer);
  }

  public MVStore getCache() {
    return db;
  }

  /**
   * @return the current map
   */
  public Map getCurrentMap() {
    return maps.get(currentMap);
  }

  /**
   * @return the current zone
   */
  public Zone getCurrentZone() {
    return maps.get(currentMap).getZone(currentZone);
  }

  /**
   * @return the current zone
   */
  public int getCurrentZoneIndex() {
    return currentZone;
  }

  /**
   * @param uid the unique identifier of a map
   * @return the map with the given uid
   */
  public Map getMap(int uid) {
    if (!maps.containsKey(uid)) {
      Map map = MapLoader.loadMap(entityStore.getMapPath(uid), uid, files);
      System.out.println("Loaded map " + map.toString());
      maps.put(uid, map);
    }
    return maps.get(uid);
  }

  /**
   * Sets the current zone.
   *
   * @param i the index of the current zone
   */
  public void setCurrentZone(int i) {
    currentZone = i;
    zoneActivator.activateZone(getCurrentZone());
  }

  /**
   * Enter a new zone through a door.
   *
   * @param door
   * @param previousZone
   */
  public void enterZone(Door door, Zone previousZone) {
    if (door.portal.getDestZone() > -1) {
      setCurrentZone(door.portal.getDestZone());
    } else {
      setCurrentZone(0);
    }

    if (getCurrentMap() instanceof Dungeon && getCurrentZone().isRandom()) {
      new DungeonGenerator(getCurrentZone()).generate(door, previousZone);
    }
  }

  /**
   * Set the current map.
   *
   * @param map the new current map
   */
  public void setMap(Map map) {
    if (!maps.containsKey(map.getUID())) {
      // could be a random map that's not in the database yet
      maps.put(map.getUID(), map);
    }
    currentMap = map.getUID();
  }

  @Override
  public void close() throws IOException {
    db.close();
  }
}
