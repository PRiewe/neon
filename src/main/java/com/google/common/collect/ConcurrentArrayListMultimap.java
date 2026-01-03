package com.google.common.collect;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConcurrentArrayListMultimap<K, V> extends AbstractListMultimap<K, V> {

  public ConcurrentArrayListMultimap() {
    super(new ConcurrentHashMap<>());
  }

  @Override
  List<V> createCollection() {
    return new CopyOnWriteArrayList<V>();
  }
}
