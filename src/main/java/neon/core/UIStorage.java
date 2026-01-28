package neon.core;

import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.services.ResourceProvider;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;

public interface UIStorage {
  Player getPlayer();

  ResourceProvider getResources();

  ResourceManager getResourceManageer();

  UIDStore getStore();

  FileSystem getFileSystem();
}
