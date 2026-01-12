package neon.core;

import neon.entities.UIDStore;
import neon.maps.Atlas;
import neon.maps.ZoneFactory;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;

public interface GameStores {
  Atlas getAtlas();

  UIDStore getStore();

  ResourceManager getResources();

  FileSystem getFileSystem();

  ZoneFactory getZoneFactory();
}
