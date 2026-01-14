package neon.util.mapstorage;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import org.h2.mvstore.type.DataType;

public interface MapStore {
  void close();

  long commit();

  <K, V> ConcurrentMap<K, V> openMap(String filename);

  <K, V> ConcurrentMap<K, V> openMap(String filename, DataType<K> keyType, DataType<V> valueType);

  boolean isClosed();

  Collection<String> getMapNames();
}
