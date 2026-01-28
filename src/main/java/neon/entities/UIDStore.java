package neon.entities;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.Closeable;
import java.util.Map;
import neon.maps.services.EntityStore;

public abstract class UIDStore implements EntityStore, Closeable {
  // dummy uid for objects that don't actually exist
  public static final long DUMMY = 0;
  // uids of all objects in the game
  protected Map<Long, Entity> objects;
  // uids of all loaded mods
  protected Map<Short, Mod> mods;
  // uids of all loaded maps
  private final BiMap<Integer, String> maps = HashBiMap.create();

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

  /**
   * @param name the name of a mod
   * @return the unique identifier of this mod
   */
  public short getModUID(String name) {
    for (Mod mod : mods.values()) {
      if (mod.name().equals(name)) {
        return mod.uid();
      }
    }
    System.out.println("Mod " + name + " not found");
    return 0;
  }

  /**
   * Adds a {@code Map} with the given uid and path.
   *
   * @param uid
   * @param path
   */
  public void addMap(Integer uid, String... path) {
    maps.put(uid, UIDStore.toString(path));
  }

  /**
   * Adds an object to the list.
   *
   * @param entity the object to be added
   */
  public void addEntity(Entity entity) {
    objects.put(entity.getUID(), entity);
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
    Mod mod = new Mod(uid, id);
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
    return maps.inverse().get(UIDStore.toString(path));
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

  public abstract void commit();
}
