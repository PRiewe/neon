package neon.entities;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryUIDStore extends UIDStore {
  public MemoryUIDStore() {
    objects = new ConcurrentHashMap<>();
    mods = new ConcurrentHashMap<>();
  }

  @Override
  public boolean isModUIDLoaded(String name) {
    return false;
  }

  @Override
  public void commit() {
    // noop
  }

  @Override
  public void close() throws IOException {
    // noop
  }
}
