package neon.core;

import neon.entities.AbstractUIDStore;
import neon.entities.Player;
import neon.maps.services.ResourceProvider;
import neon.systems.files.FileSystem;

public interface UIStorage {
  Player getPlayer();

  ResourceProvider getResources();

  AbstractUIDStore getStore();

  FileSystem getFileSystem();
}
