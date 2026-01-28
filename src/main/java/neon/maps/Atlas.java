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
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neon.core.GameContext;
import neon.core.GameStore;
import neon.entities.Door;
import neon.maps.mvstore.IntegerDataType;
import neon.maps.mvstore.MapDataType;
import neon.maps.mvstore.WorldDataType;
import neon.maps.services.MapAtlas;
import neon.maps.services.QuestProvider;
import neon.util.mapstorage.MapStore;

/**
 * This class keeps track of all loaded maps and their connections.
 *
 * @author mdriesen
 */
@Slf4j
public class Atlas implements Closeable, MapAtlas {
  @Getter private final MapStore atlasMapStore;
  private final ConcurrentMap<Integer, Map> maps;
  private final MapLoader mapLoader;
  private final ZoneFactory zoneFactory;
  private final WorldDataType worldDataType;
  private final Dungeon.DungeonDataType dungeonDataType;
  private final MapDataType mapDataType;
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
      ZoneFactory zoneFactory,
      MapLoader mapLoader,
      GameContext gameContext) {
    this.gameStore = gameStore;
    this.questProvider = questProvider;
    this.zoneActivator = zoneActivator;
    this.atlasMapStore = atlasStore;
    this.mapLoader = mapLoader;
    this.zoneFactory = zoneFactory;
    worldDataType = new WorldDataType(zoneFactory);
    dungeonDataType = new Dungeon.DungeonDataType(zoneFactory);
    mapDataType = new MapDataType(worldDataType, dungeonDataType);
    maps = atlasMapStore.openMap("maps", IntegerDataType.INSTANCE, mapDataType);
    this.gameContext = gameContext;
  }

  /**
   * @return the current map
   */
  public Map getCurrentMap() {
    return maps.get(currentMap);
  }

  public void setCurrentMap(Map map) {
    putMapIfNeeded(map);
    currentMap = map.getUID();
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

  public Map getMap(int uid, String... path) {
    Map map = mapLoader.loadMap(path, uid);
    return map;
  }

  public void putMapIfNeeded(Map map) {
    if (!maps.containsKey(map.getUID())) {
      // could be a random map that's not in the database yet
      maps.put(map.getUID(), map);
    }
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
  }

  @Override
  public void close() {
    atlasMapStore.close();
  }

  public void setMap(Map world) {
    setCurrentMap(world);
  }
}
