package neon.util.mapstorage;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import org.h2.mvstore.MVStore;

public class MapStoreMVStoreAdapter implements MapStore {
  private final MVStore mvStore;

  public MapStoreMVStoreAdapter(MVStore mvStore) {
    this.mvStore = mvStore;
  }

  @Override
  public void close() {
    mvStore.close();
  }

  @Override
  public long commit() {
    return mvStore.commit();
  }

  @Override
  public <K, V> ConcurrentMap<K, V> openMap(String filename) {
    return mvStore.openMap(filename);
  }

  @Override
  public boolean isClosed() {
    return mvStore.isClosed();
  }

  public Collection<String> getMapNames() {
    return mvStore.getMapNames();
  }
}
