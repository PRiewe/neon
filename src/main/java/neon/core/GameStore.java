package neon.core;

import java.io.Closeable;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import neon.entities.ConcreteUIDStore;
import neon.entities.Player;
import neon.entities.UIDStore;
import neon.maps.ZoneFactory;
import neon.maps.services.ResourceProvider;
import neon.resources.ResourceManager;
import neon.systems.files.FileSystem;

@Getter
public class GameStore implements Closeable, UIStorage {
  private final FileSystem fileSystem;
  private final ConcreteUIDStore uidStore;
  private final ResourceManager resourceManager;
  private final String uidStoreFileName;
  private ZoneFactory zoneFactory;

  @Setter private Player player;

  public GameStore(FileSystem fileSystem, ResourceManager resourceManager) {
    this.fileSystem = fileSystem;
    this.uidStoreFileName = fileSystem.getFullPath("uidstore");
    this.uidStore = new ConcreteUIDStore(uidStoreFileName);
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
  public ResourceManager getResourceManageer() {
    return resourceManager;
  }

  @Override
  public UIDStore getStore() {
    return uidStore;
  }
}
