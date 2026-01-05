package neon.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import neon.resources.RClothing;
import neon.resources.RItem;

class UIDStoreTest {

  @org.junit.jupiter.api.Test
  void addEntity() {
    UIDStore store = new UIDStore("testfile3.dat");
    var id = store.createNewMapUID();
    Entity entity = new Armor(id, new RClothing("one", RItem.Type.armor, "dummy"));
    store.addMap(id, "path1", "path2");

    var result = store.getMapPath(id);
    System.out.println(Arrays.toString(result));
    // assertEquals(entity.getClass(),result.getClass());
    store.getCache().close();
  }

  @org.junit.jupiter.api.Test
  void removeEntity() {}

  @org.junit.jupiter.api.Test
  void getEntity() {}

  @org.junit.jupiter.api.Test
  void createNewEntityUID() {}

  @org.junit.jupiter.api.Test
  void createNewMapUID() {}
}
