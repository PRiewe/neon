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
import neon.core.GameContext;
import neon.core.GameStore;
import neon.entities.Door;
import neon.maps.generators.DungeonGenerator;
import neon.maps.services.MapAtlas;
import neon.maps.services.QuestProvider;
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
  private final MapLoader mapLoader;
  private final GameContext gameContext;
  private int currentZone = 0;
  private int currentMap = 0;
  private final QuestProvider questProvider;
  private final ZoneActivator zoneActivator;
  private final GameStore gameStore;

  /**
   * Initializes this {@code Atlas} with dependency injection.
   *
   * @param atlasStore the MVStore for caching
   * @param questProvider the quest provider service
   * @param zoneActivator the zone activator for physics management
   */
  public Atlas(
      GameStore gameStore,
      MapStore atlasStore,
      QuestProvider questProvider,
      ZoneActivator zoneActivator,
      MapLoader mapLoader,
      GameContext gameContext) {
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
    this.gameContext = gameContext;
  }

  private MapStore getMapStore(FileSystem files, String fileName) {
    files.delete(fileName);

    log.warn("Creating new MVStore at {}", fileName);

    return new MapStoreMVStoreAdapter(MVStore.open(fileName));
  }

  /**
   * Creates a default zone activator using Engine singleton (for backward compatibility).
   *
   * @return a zone activator
   */
  static ZoneActivator createDefaultZoneActivator(GameStore gameStore) {
    return new ZoneActivator(new neon.maps.services.EnginePhysicsManager(), gameStore);
  }

  public MapStore getCache() {
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
      if (gameStore.getUidStore().getMapPath(uid) == null) {
        throw new RuntimeException(String.format("No existing mappath for uid %d", uid));
      }

      Map map = mapLoader.loadMap(gameStore.getUidStore().getMapPath(uid), uid);
      System.out.println("Loaded map " + map.toString());
      maps.put(uid, map);
    }
    return maps.get(uid);
  }

  public void putMapIfNeeded(Map map) {}

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
      new DungeonGenerator(getCurrentZone(), questProvider, gameContext)
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
