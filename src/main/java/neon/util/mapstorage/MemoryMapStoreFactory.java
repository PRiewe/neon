package neon.util.mapstorage;

import org.h2.mvstore.type.DataType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryMapStoreFactory implements MapStore{

    Set<String> mapNames = new HashSet<>();
    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public long commit() {
        // nothing to do
        return 0;
    }

    @Override
    public <K, V> ConcurrentMap<K, V> openMap(String filename) {
        mapNames.add(filename);
        return new ConcurrentHashMap<>();
    }

    @Override
    public <K, V> ConcurrentMap<K, V> openMap(String filename, DataType<K> keyType, DataType<V> valueType) {
        mapNames.add(filename);
        return new ConcurrentHashMap<>();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public Collection<String> getMapNames() {
        return mapNames;
    }
}
