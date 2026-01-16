package neon.core;

import lombok.Getter;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.entities.mvstore.EntityDataType;
import neon.entities.mvstore.ModDataType;
import neon.entities.serialization.EntityFactory;
import neon.maps.Atlas;
import neon.maps.MapLoader;
import neon.maps.ZoneFactory;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.util.mapstorage.MapStore;

@Getter
public class DefaultGameStores implements GameStores {

  private final ResourceManager resources;
  private final FileSystem fileSystem;
  private final UIDStore store;
  private final Atlas atlas;
  private final ZoneFactory zoneFactory;
  private final MapStore zoneMapStore;

  public DefaultGameStores(ResourceManager resources, FileSystem fileSystem,UIDStore store) {
    this.resources = resources;
    this.fileSystem = fileSystem;

    // Phase 1: Create UIDStore without DataTypes
    this.store = store;

    // Phase 2: Create EntityFactory with dependencies (GameContext is null at this stage)
    EntityFactory entityFactory = new EntityFactory(resources, this.store);
    // Phase 3: Create DataTypes
    EntityDataType entityDataType = new EntityDataType(entityFactory);
    ModDataType modDataType = new ModDataType();

    // Phase 4: Initialize UIDStore's maps with DataTypes
    store.setDataTypes(entityDataType, modDataType);

    // Continue with rest of initialization
    zoneMapStore = Atlas.getMapStore(fileSystem, "zones");
    this.zoneFactory = new ZoneFactory(zoneMapStore, store, resources);
    MapLoader mapLoader = new MapLoader(fileSystem, store, resources, zoneFactory);
    atlas = new Atlas(fileSystem, zoneMapStore, store, resources, mapLoader);
  }
}
