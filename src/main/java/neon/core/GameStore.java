package neon.core;

import java.io.Closeable;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.ZoneFactory;
import neon.maps.services.ResourceProvider;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;

@Getter
public class GameStore implements Closeable, UIStorage {
  private final FileSystem fileSystem;
  private final UIDStore uidStore;
  private final ResourceManager resourceManager;
  private ZoneFactory zoneFactory;

  @Setter private Player player;

  public GameStore(FileSystem fileSystem, ResourceManager resourceManager) {
    this.fileSystem = fileSystem;
    this.uidStore = new UIDStore(fileSystem.getFullPath("uidstore"));
    this.resourceManager = resourceManager;
    this.player = Player.PLACEHOLDER;
  }

  @Override
  public void close() throws IOException {
    if (uidStore != null) {
      uidStore.close();
    }
  }

  public ResourceProvider getResources() {
    return resourceManager;
  }

  @Override
  public UIDStore getStore() {
    return uidStore;
  }
}
