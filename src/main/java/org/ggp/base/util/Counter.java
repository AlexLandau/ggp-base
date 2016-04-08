package org.ggp.base.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

//TODO: Replace with Multisets (?)
//No, because longs are better than ints
public class Counter<T> {
    private final Map<T, Long> counts = Maps.newHashMap();

    public static <T> Counter<T> create() {
        return new Counter<T>();
    }

    public synchronized long get(T key) {
        if (counts.containsKey(key)) {
            return counts.get(key);
        } else {
            return 0L;
        }
    }

    public synchronized void increment(T key) {
        if (!counts.containsKey(key)) {
            counts.put(key, 1L);
        } else {
            counts.put(key, 1L + counts.get(key));
        }
    }

    public synchronized void add(T key, long toAdd) {
        if (!counts.containsKey(key)) {
            counts.put(key, toAdd);
        } else {
            counts.put(key, toAdd + counts.get(key));
        }
    }
    public synchronized Set<T> keySet() {
        return ImmutableSet.copyOf(counts.keySet());
    }

    public synchronized Set<Entry<T, Long>> entrySet() {
        return ImmutableSet.copyOf(counts.entrySet());
    }

    public synchronized boolean isEmpty() {
        return counts.isEmpty();
    }

    public synchronized void clear() {
        counts.clear();
    }
}
