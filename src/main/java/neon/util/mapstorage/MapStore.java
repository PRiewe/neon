package neon.util.mapstorage;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

public interface MapStore {
  void close();

  long commit();

  <K, V> ConcurrentMap<K, V> openMap(String filename);

  boolean isClosed();

  Collection<String> getMapNames();
}
