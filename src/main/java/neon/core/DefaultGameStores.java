package neon.core;

import lombok.Getter;
import neon.entities.Player;
import neon.entities.UIDStore;
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

  public DefaultGameStores(ResourceManager resources, FileSystem fileSystem, Player player) {
    this.resources = resources;
    this.fileSystem = fileSystem;
    this.store = new UIDStore(fileSystem, fileSystem.getFullPath("uidstore"));
    zoneMapStore = Atlas.getMapStore(fileSystem, "zones");
    this.zoneFactory = new ZoneFactory(zoneMapStore, store, resources);
    MapLoader mapLoader = new MapLoader(fileSystem, store, resources, zoneFactory, player);
    atlas = new Atlas(fileSystem, zoneMapStore, store, resources, mapLoader);
  }
}
