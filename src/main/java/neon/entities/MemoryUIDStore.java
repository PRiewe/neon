package neon.entities;

import java.util.concurrent.ConcurrentHashMap;

public class MemoryUIDStore extends AbstractUIDStore {
  public MemoryUIDStore() {
    objects = new ConcurrentHashMap<>();
    mods = new ConcurrentHashMap<>();
  }
}
