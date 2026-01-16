package neon.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import neon.core.GameStores;
import neon.entities.mvstore.EntityDataType;
import neon.entities.mvstore.ModDataType;
import neon.entities.property.Gender;
import neon.entities.serialization.EntityFactory;
import neon.maps.Atlas;
import neon.maps.ZoneFactory;
import neon.resources.RClothing;
import neon.resources.RCreature;
import neon.resources.RItem;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.test.TestEngineContext;
import neon.util.mapstorage.MapStore;
import org.junit.jupiter.api.Test;

class UIDStoreTest {
  FileSystem fileSystem = TestEngineContext.getStubFileSystem();

  // Simple GameStores stub for testing
  private static class TestGameStores implements GameStores {
    private final ResourceManager resources = new ResourceManager();
    private final FileSystem fileSystem;
    private final UIDStore store;

    TestGameStores(FileSystem fileSystem, UIDStore store) {
      this.fileSystem = fileSystem;
      this.store = store;
    }

    @Override
    public ResourceManager getResources() {
      return resources;
    }

    @Override
    public FileSystem getFileSystem() {
      return fileSystem;
    }

    @Override
    public UIDStore getStore() {
      return store;
    }

    @Override
    public Atlas getAtlas() {
      return null;
    }

    @Override
    public ZoneFactory getZoneFactory() {
      return null;
    }

    @Override
    public MapStore getZoneMapStore() {
      return null;
    }
  }

  private UIDStore createInitializedUIDStore(String filename) {
    UIDStore store = new UIDStore( fileSystem.getFullPath(filename));
    GameStores gameStores = new TestGameStores(fileSystem, store);
    Player stubPlayer =
            new Player(
                    new RCreature("test"),
                    "TestPlayer",
                    Gender.MALE,
                    Player.Specialisation.combat,
                    "Warrior",
                    store);
    EntityFactory entityFactory =
        new EntityFactory(gameStores.getResources(), gameStores.getStore());
    EntityDataType entityDataType = new EntityDataType(entityFactory);
    ModDataType modDataType = new ModDataType();
    store.setDataTypes(entityDataType, modDataType);
    return store;
  }

  @Test
  void addEntity() throws IOException {
    UIDStore store = createInitializedUIDStore("test_add_entity.dat");
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
    UIDStore store = createInitializedUIDStore("test_remove_entity.dat");
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
