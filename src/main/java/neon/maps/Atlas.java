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
import neon.core.GameStore;
import neon.core.UIEngineContext;
import neon.entities.Door;
import neon.maps.generators.DungeonGenerator;
import neon.maps.services.EngineQuestProvider;
import neon.maps.services.EntityStore;
import neon.maps.services.MapAtlas;
import neon.maps.services.QuestProvider;
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
  private int currentZone = 0;
  private int currentMap = 0;
  private final QuestProvider questProvider;
  private final ZoneActivator zoneActivator;
  private final GameStore gameStore;
  private final MapLoader mapLoader;
  private final UIEngineContext uiEngineContext;

  /**
   * Initializes this {@code Atlas} with the given {@code FileSystem} and cache path. The cache is
   * lazy initialised.
   *
   * @param gameStore a {@code FileSystem}
   * @param path the path to the file used for caching
   * @deprecated Use {@link #Atlas(GameStore, String, EntityStore, ZoneActivator)} to avoid
   *     dependency on Engine singleton
   */
  @Deprecated
  public Atlas(
      GameStore gameStore, String path, MapLoader mapLoader, UIEngineContext uiEngineContext) {
    this(
        gameStore,
        getMVStore(gameStore.getFileSystem(), path),
        new EngineQuestProvider(),
        createDefaultZoneActivator(gameStore),
        mapLoader,
        uiEngineContext);
  }

  /**
   * Initializes this {@code Atlas} with dependency injection.
   *
   * @param atlasStore the MVStore for caching
   * @param questProvider the quest provider service
   * @param zoneActivator the zone activator for physics management
   */
  public Atlas(
      GameStore gameStore,
      MVStore atlasStore,
      QuestProvider questProvider,
      ZoneActivator zoneActivator,
      MapLoader mapLoader,
      UIEngineContext uiEngineContext) {
    this.gameStore = gameStore;
    this.questProvider = questProvider;
    this.zoneActivator = zoneActivator;
    this.db = atlasStore;
    // files.delete(path);
    // String fileName = files.getFullPath(path);
    // log.warn("Creating new MVStore at {}", fileName);

    // db = MVStore.open(fileName);
    maps = atlasStore.openMap("maps");
    this.mapLoader = mapLoader;
    this.uiEngineContext = uiEngineContext;
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
  static ZoneActivator createDefaultZoneActivator(GameStore gameStore) {
    return new ZoneActivator(new neon.maps.services.EnginePhysicsManager(), gameStore);
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
  @Override
  public Map getMap(int uid) {
    if (!maps.containsKey(uid)) {
      Map map = mapLoader.loadMap(gameStore.getUidStore().getMapPath(uid), uid);
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
      new DungeonGenerator(getCurrentZone(), questProvider, uiEngineContext)
          .generate(door, previousZone, this);
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
