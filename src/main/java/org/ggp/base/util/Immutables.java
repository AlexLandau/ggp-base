package org.ggp.base.util;

import java.util.Comparator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;

import net.alloyggp.escaperope.rope.ropify.ListRopeWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.RopeWeaver;

public class Immutables {
    private Immutables() {
        // Not instantiable
    }

    public static <T> Collector<T, ?, ImmutableList<T>> collectList() {
        return new Collector<T, ImmutableList.Builder<T>, ImmutableList<T>>() {
            @Override
            public Supplier<ImmutableList.Builder<T>> supplier() {
                return () -> ImmutableList.builder();
            }

            @Override
            public BiConsumer<ImmutableList.Builder<T>, T> accumulator() {
                return (builder, elem) -> builder.add(elem);
            }

            @Override
            public BinaryOperator<ImmutableList.Builder<T>> combiner() {
                return (b1, b2) -> b1.addAll(b2.build());
            }

            @Override
            public Function<ImmutableList.Builder<T>, ImmutableList<T>> finisher() {
                return builder -> builder.build();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics() {
                return ImmutableSet.of();
            }
        };
    }

    public static <T extends Comparable<T>> Collector<T, ?, ImmutableSortedSet<T>> collectSortedSet() {
        return new Collector<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>>() {
            @Override
            public Supplier<ImmutableSortedSet.Builder<T>> supplier() {
                return () -> ImmutableSortedSet.naturalOrder();
            }

            @Override
            public BiConsumer<ImmutableSortedSet.Builder<T>, T> accumulator() {
                return (builder, elem) -> builder.add(elem);
            }

            @Override
            public BinaryOperator<ImmutableSortedSet.Builder<T>> combiner() {
                return (b1, b2) -> b1.addAll(b2.build());
            }

            @Override
            public Function<ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>> finisher() {
                return builder -> builder.build();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics() {
                return ImmutableSet.of();
            }
        };
    }

    public static <T> Collector<T, ?, ImmutableSortedSet<T>> collectSortedSet(Comparator<? super T> comparator) {
        return new Collector<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>>() {
            @Override
            public Supplier<ImmutableSortedSet.Builder<T>> supplier() {
                return () -> new ImmutableSortedSet.Builder<T>(comparator);
            }

            @Override
            public BiConsumer<ImmutableSortedSet.Builder<T>, T> accumulator() {
                return (builder, elem) -> builder.add(elem);
            }

            @Override
            public BinaryOperator<ImmutableSortedSet.Builder<T>> combiner() {
                return (b1, b2) -> b1.addAll(b2.build());
            }

            @Override
            public Function<ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>> finisher() {
                return builder -> builder.build();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics() {
                return ImmutableSet.of();
            }
        };
    }

    public static <T> Collector<T, ?, ImmutableSet<T>> collectSet() {
        return new Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>>() {
            @Override
            public Supplier<ImmutableSet.Builder<T>> supplier() {
                return () -> ImmutableSet.builder();
            }

            @Override
            public BiConsumer<ImmutableSet.Builder<T>, T> accumulator() {
                return (builder, elem) -> builder.add(elem);
            }

            @Override
            public BinaryOperator<ImmutableSet.Builder<T>> combiner() {
                return (b1, b2) -> b1.addAll(b2.build());
            }

            @Override
            public Function<ImmutableSet.Builder<T>, ImmutableSet<T>> finisher() {
                return builder -> builder.build();
            }

            @Override
            public Set<java.util.stream.Collector.Characteristics> characteristics() {
                return ImmutableSet.of();
            }
        };
    }

    public static <T> RopeWeaver<ImmutableList<T>> listWeaver(RopeWeaver<T> innerWeaver) {
        return new ListRopeWeaver<ImmutableList<T>>() {
            @Override
            protected void addToList(ImmutableList<T> objects, RopeBuilder list) {
                for (T object : objects) {
                    list.add(object, innerWeaver);
                }
            }

            @Override
            protected ImmutableList<T> fromRope(RopeList list) {
                ImmutableList.Builder<T> builder = ImmutableList.builder();
                for (int i = 0; i < list.size(); i++) {
                    builder.add(list.get(i, innerWeaver));
                }
                return builder.build();
            }
        };
    }

    public static <K, V> RopeWeaver<ImmutableSetMultimap<K, V>> setMultimapWeaver(RopeWeaver<K> keyWeaver, RopeWeaver<V> valueWeaver) {
        return new ListRopeWeaver<ImmutableSetMultimap<K,V>>() {
            @Override
            protected void addToList(ImmutableSetMultimap<K, V> map, RopeBuilder list) {
                for (K key : map.keySet()) {
                    list.add(key, keyWeaver);
                    list.add(map.get(key), setWeaver(valueWeaver));
                }
            }

            @Override
            protected ImmutableSetMultimap<K, V> fromRope(RopeList list) {
                Preconditions.checkArgument(list.size() % 2 == 0);
                ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
                for (int i = 0; i < list.size(); i += 2) {
                    K key = list.get(i, keyWeaver);
                    ImmutableSet<V> values = list.get(i + 1, setWeaver(valueWeaver));
                    builder.putAll(key, values);
                }
                return builder.build();
            }
        };
    }

    public static <T> RopeWeaver<ImmutableSet<T>> setWeaver(RopeWeaver<T> innerWeaver) {
        return new ListRopeWeaver<ImmutableSet<T>>() {
            @Override
            protected void addToList(ImmutableSet<T> set, RopeBuilder list) {
                for (T element : set) {
                    list.add(element, innerWeaver);
                }
            }

            @Override
            protected ImmutableSet<T> fromRope(RopeList list) {
                ImmutableSet.Builder<T> builder = ImmutableSet.builder();
                for (int i = 0; i < list.size(); i++) {
                    builder.add(list.get(i, innerWeaver));
                }
                return builder.build();
            }
        };
    }
}
