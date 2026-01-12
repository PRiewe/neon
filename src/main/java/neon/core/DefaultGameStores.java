package neon.core;

import lombok.Getter;
import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.MapLoader;
import neon.maps.ZoneFactory;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;
import neon.util.mapstorage.MapStore;
import org.h2.mvstore.MVStore;

@Getter
public class DefaultGameStores implements GameStores {

  private final ResourceManager resources;
  private final FileSystem fileSystem;
  private final UIDStore store;
  private final Atlas atlas;
  private final ZoneFactory zoneFactory;

  public DefaultGameStores(ResourceManager resources, FileSystem fileSystem) {
    this.resources = resources;
    this.fileSystem = fileSystem;
    this.store = new UIDStore(fileSystem.getFullPath("uidstore"));
    MapStore mapStore = Atlas.getMapStore(fileSystem,"zones");
    this.zoneFactory = new ZoneFactory(mapStore,store,resources);
    MapLoader mapLoader = new MapLoader(fileSystem, store, resources,zoneFactory);
    this.atlas = new Atlas(fileSystem, fileSystem.getFullPath("atlas"), store, mapLoader);
  }

  public DefaultGameStores(
      ResourceManager resources, FileSystem fileSystem, UIDStore store, Atlas atlas) {
    this.resources = resources;
    this.fileSystem = fileSystem;
    this.store = store;
    this.atlas = atlas;
    MapStore mapStore = Atlas.getMapStore(fileSystem,"zones");
    this.zoneFactory = new ZoneFactory(mapStore,store,resources);

  }
}
