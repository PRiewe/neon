package neon.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import neon.resources.RClothing;
import neon.resources.RItem;
import neon.systems.files.FileSystem;
import neon.test.TestEngineContext;
import org.junit.jupiter.api.Test;

class UIDStoreTest {
  FileSystem fileSystem = TestEngineContext.getStubFileSystem();

  @Test
  void addEntity() throws IOException {
    UIDStore store = new UIDStore(fileSystem, "testfile3.dat");
    var id = store.createNewMapUID();
    var entityId = store.createNewEntityUID();
    Entity entity = new Armor(entityId, new RClothing("one", RItem.Type.armor, "dummy"));
    store.addMap(id, "path1", "path2");
    store.addEntity(entity);
    var result = store.getEntity(entityId);

    assertEquals(entity.getClass(), result.getClass());
    store.close();
  }

  @Test
  void removeEntity() throws IOException {
    UIDStore store = new UIDStore(fileSystem, "testfile3.dat");
    var id = store.createNewMapUID();
    var entityId = store.createNewEntityUID();
    Entity entity = new Armor(entityId, new RClothing("one", RItem.Type.armor, "dummy"));
    store.addMap(id, "path1", "path2");
    store.addEntity(entity);
    var result = store.getEntity(entityId);

    assertEquals(entity.getClass(), result.getClass());

    store.removeEntity(entityId);
    var result2 = store.getEntity(entityId);
    assertNull(result2);
    store.close();
  }
}
