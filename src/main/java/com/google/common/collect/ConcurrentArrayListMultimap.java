package com.google.common.collect;

import com.google.common.collect.AbstractListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConcurrentArrayListMultimap<K,V> extends AbstractListMultimap<K, V> {

    public ConcurrentArrayListMultimap() {
        super(new ConcurrentHashMap<>());
    }

    @Override
    List<V> createCollection() {
        return new CopyOnWriteArrayList<V>();
    }
}
