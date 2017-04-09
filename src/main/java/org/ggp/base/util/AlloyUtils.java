package org.ggp.base.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormDomain;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class AlloyUtils {
    private AlloyUtils() {
        //Uninstantiable
    }

    public static <T> ImmutableMap<T, Integer> getListIndex(List<T> input) {
        ImmutableMap.Builder<T, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < input.size(); i++) {
            builder.put(input.get(i), i);
        }
        return builder.build();
    }

    public static <K, V> void printEachLine(Map<K, V> map) {
        for (Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry);
        }
    }

    public static <T> void printEachLine(List<T> list) {
        for (T entry : list) {
            System.out.println(entry);
        }
    }

    private static final LoadingCache<Long, AtomicInteger> EVERY_NTH_TIME_COUNTERS = //Maps.newConcurrentMap();
            CacheBuilder.newBuilder().build(new CacheLoader<Long, AtomicInteger>() {
                @Override
                public AtomicInteger load(Long key) throws Exception {
                    return new AtomicInteger();
                }
            });
    public static void printEveryNTimes(int nTimes, long uniqueId, String toPrint) {
        int timesSoFar = EVERY_NTH_TIME_COUNTERS.getUnchecked(uniqueId).incrementAndGet();
        if (timesSoFar % nTimes == 0) {
            System.out.println(toPrint + ": " + timesSoFar + " times");
        }
    }

    public static void print(SentenceDomainModel model) {
        for (SentenceForm form : model.getSentenceForms()) {
            SentenceFormDomain domain = model.getDomain(form);
            System.out.println(form + ": " + domain);
        }
    }

    public static <T> Set<T> intersectAll(Collection<Set<T>> sets) {
        Preconditions.checkArgument(!sets.isEmpty());
        Set<T> intersection = Sets.newHashSet(Iterables.get(sets, 0));
        for (Set<T> set : Iterables.skip(sets, 1)) {
            intersection.retainAll(set);
        }
        return intersection;
    }

    public static <T> Iterable<T> randomize(Iterable<T> input) {
        List<T> values = Lists.newArrayList(input);
        Collections.shuffle(values);
        return values;
    }

    //TODO: Better source of randomness?
    private static final Random RANDOM = new Random();
    public static <T> T pickOneAtRandom(List<T> list) {
        return pickOneAtRandom(list, RANDOM);
    }

    public static <T> T pickOneAtRandom(List<T> list, Random random) {
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() == 0) {
            throw new IllegalArgumentException("List must have at least one element");
        } else {
            int index = random.nextInt(list.size());
            return list.get(index);
        }
    }

    public static String removeOuterBrackets(String string) {
        string = string.trim();
        int length = string.length();
        if (length >= 2) {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
