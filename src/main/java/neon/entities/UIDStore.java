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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.*;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neon.entities.mvstore.EntityDataType;
import neon.entities.mvstore.LongDataType;
import neon.entities.mvstore.ModDataType;
import neon.entities.mvstore.ShortDataType;
import neon.maps.services.EntityStore;
import neon.systems.files.FileSystem;
import neon.util.mapstorage.MapStore;
import neon.util.mapstorage.MapStoreMVStoreAdapter;
import org.h2.mvstore.MVStore;

/**
 * This class stores the UIDs of every object, map and mod currently in the game. It can give out
 * new UIDs to objects created during gameplay. Positive UIDs are used in resources loaded from a
 * mod. Negative UIDs are reserved for random generation.
 *
 * @author mdriesen
 */
@Slf4j
public class UIDStore implements EntityStore, Closeable {
  // dummy uid for objects that don't actually exist
  public static final long DUMMY = 0;
  @Getter private final FileSystem fileSystem;
  // uid database
  @Getter private final MapStore uidDb;
  // uids of all objects in the game
  private Map<Long, Entity> objects;
  // uids of all loaded mods
  private Map<Short, ModDataType.Mod> mods;
  // uids of all loaded maps
  private final BiMap<Integer, String> maps = HashBiMap.create();

  /**
   * Tells this UIDStore to use the given jdbm3 cache.
   *
   * @param file
   */
  public UIDStore(FileSystem fileSystem, String file) {
    this.fileSystem = fileSystem;
    uidDb = new MapStoreMVStoreAdapter(MVStore.open(file));
    // Maps will be opened after DataTypes are set via setDataTypes()
  }

  public UIDStore(FileSystem fileSystem, MapStore mapStore) {
    this.fileSystem = fileSystem;
    uidDb = mapStore;
    // Maps will be opened after DataTypes are set via setDataTypes()
  }

  /**
   * Sets the DataTypes for entity and mod serialization and opens the maps. This must be called
   * after construction to initialize the UIDStore.
   *
   * @param entityDataType the DataType for entity serialization
   * @param modDataType the DataType for mod serialization
   */
  public void setDataTypes(EntityDataType entityDataType, ModDataType modDataType) {
    this.objects = uidDb.openMap("object", LongDataType.INSTANCE, entityDataType);
    this.mods = uidDb.openMap("mods", ShortDataType.INSTANCE, modDataType);
  }

  /**
   * @return the jdbm3 cache used by this UIDStore
   */
  public MapStore getCache() {
    return uidDb;
  }

  /**
   * @param name the name of a mod
   * @return the unique identifier of this mod
   */
  public short getModUID(String name) {
    for (ModDataType.Mod mod : mods.values()) {
      if (mod.name().equals(name)) {
        return mod.uid();
      }
    }
    throw new RuntimeException("Mod " + name + " not found");
    // System.out.println("Mod " + name + " not found");
    // return 0;
  }

  public boolean isModUIDLoaded(String name) {
    for (ModDataType.Mod mod : mods.values()) {
      if (mod.name().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a {@code Map} with the given uid and path.
   *
   * @param uid
   * @param path
   */
  public void addMap(Integer uid, String... path) {
    maps.put(uid, toString(path));
  }

  /**
   * Adds an object to the list.
   *
   * @param entity the object to be added
   */
  public void addEntity(Entity entity) {
    objects.put(entity.getUID(), entity);
    if (objects.size() % 1000 == 0) { // do a commit every 1000 entities
      uidDb.commit();
    }
  }

  /**
   * Removes the object with the given UID.
   *
   * @param uid the UID of the object to be removed
   */
  public void removeEntity(long uid) {
    objects.remove(uid);
  }

  /**
   * Returns the entity with the given UID. If the UID is a {@code DUMMY}, {@code null} is returned.
   *
   * @param uid the UID of an object
   * @return the object with the given UID
   */
  public Entity getEntity(long uid) {
    return (uid == DUMMY ? null : objects.get(uid));
  }

  /**
   * Adds a mod with the given id.
   *
   * @param id
   */
  public void addMod(String id) {
    short uid = (short) (Math.random() * Short.MAX_VALUE);
    while (mods.containsKey(uid) || uid == 0) {
      uid++;
    }
    ModDataType.Mod mod = new ModDataType.Mod(uid, id);
    mods.put(mod.uid(), mod);
  }

  /**
   * @param uid the unique identifier of a map
   * @return the full path of a map
   */
  public String[] getMapPath(int uid) {
    if (maps.get(uid) != null) {
      return maps.get(uid).split(",");
    } else {
      return null;
    }
  }

  /**
   * @param path the path to a map
   * @return the uid of the given map
   */
  public int getMapUID(String... path) {
    var uid = maps.inverse().get(toString(path));
    if (uid == null) {
      log.warn("{} doesn't have uid", (Object) path);
    }
    return maps.inverse().get(toString(path));
  }

  /**
   * Creates a new uid for an entity.
   *
   * @return
   */
  public long createNewEntityUID() {
    // random objects have a random negative long as uid
    long uid = (long) (Math.random() * Long.MIN_VALUE);
    while (objects.containsKey(uid)) {
      uid = (uid >= 0) ? Long.MIN_VALUE : uid + 1;
    }
    return uid;
  }

  /**
   * Creates a new uid for a map.
   *
   * @return
   */
  public int createNewMapUID() {
    // random maps have a random negative int as uid
    int uid = (int) (Math.random() * Integer.MIN_VALUE);
    while (maps.containsKey(uid)) {
      uid = (uid >= 0) ? Integer.MIN_VALUE : uid + 1;
    }
    return uid;
  }

  private static String toString(String... strings) {
    StringBuilder result = new StringBuilder();
    for (String s : strings) {
      result.append(s);
      result.append(",");
    }
    // remove last ","
    result.replace(result.length(), result.length(), "");
    return result.toString();
  }

  /**
   * @param map
   * @param object
   * @return the full object UID
   */
  public static long getObjectUID(long map, long object) {
    // this to avoid problems with two's complement
    return (map << 32) | ((object << 32) >>> 32);
  }

  /**
   * @param mod
   * @param map
   * @return the full map UID
   */
  public static int getMapUID(int mod, int map) {
    // this to avoid problems with two's complement
    return (mod << 16) | ((map << 16) >>> 16);
  }

  @Override
  public void close() throws IOException {
    uidDb.commit();
    uidDb.close();
  }
}
