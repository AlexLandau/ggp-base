package org.ggp.base.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class GoalTuplePool {
    private static final GoalTuplePoolNode INITIAL_NODE = new GoalTuplePoolNode();

    public static class GoalTuplePoolNode {
        private final ConcurrentMap<Integer, GoalTuplePoolNode> children = new ConcurrentHashMap<Integer, GoalTuplePoolNode>(5, 0.75f, 4);
        private final ImmutableList<Integer> asList;
        private final ImmutableIntArray asArray;

        private GoalTuplePoolNode() {
            this.asList = ImmutableList.of();
            this.asArray = ImmutableIntArray.empty();
        }
        private GoalTuplePoolNode(ImmutableList<Integer> soFar, int addition) {
            this.asList = ImmutableList.copyOf(Iterables.concat(soFar, ImmutableList.of(addition)));
            this.asArray = ImmutableIntArray.copyOf(asList);
        }

        public GoalTuplePoolNode get(Integer value) {
            GoalTuplePoolNode result = children.get(value);
            if (result != null) {
                return result;
            }

            GoalTuplePoolNode newNode = new GoalTuplePoolNode(asList, value);
            children.putIfAbsent(value, newNode);

            return children.get(value);
        }

        public ImmutableList<Integer> getList() {
            return asList;
        }
        public ImmutableIntArray getArray() {
            return asArray;
        }
    }

    /**
     * The lowest-level way to get a goal value tuple. This does not require
     * any superfluous object creation.
     */
    public static GoalTuplePoolNode getInitialNode() {
        return INITIAL_NODE;
    }

    public static ImmutableList<Integer> canonicalize(List<Integer> input) {
        GoalTuplePoolNode curNode = getInitialNode();
        for (Integer goalValue : input) {
            curNode = curNode.get(goalValue);
        }
        return curNode.getList();
    }

    public static ImmutableList<Integer> canonicalize(int... input) {
        GoalTuplePoolNode curNode = getInitialNode();
        for (int goalValue : input) {
            curNode = curNode.get(goalValue);
        }
        return curNode.getList();
    }

    public static ImmutableIntArray canonicalize(ImmutableIntArray input) {
        GoalTuplePoolNode curNode = getInitialNode();
        for (int i = 0; i < input.size(); i++) {
            curNode = curNode.get(input.get(i));
        }
        return curNode.getArray();
    }

    //I expect to get all 0s and all 100s a lot for min and max goal values,
    //and having to create these at node creation time
    private static final LoadingCache<Integer, ImmutableList<Integer>> ALL_ZEROS_CACHE =
            CacheBuilder.newBuilder().build(new CacheLoader<Integer, ImmutableList<Integer>>() {
                @Override
                public ImmutableList<Integer> load(Integer tupleLength) throws Exception {
                    int[] goalResults = new int[tupleLength];
                    return canonicalize(goalResults);
                }
            });
    public static ImmutableList<Integer> getAllZeros(int numRoles) {
        return ALL_ZEROS_CACHE.getUnchecked(numRoles);
    }

    private static final LoadingCache<Integer, ImmutableList<Integer>> ALL_HUNDREDS_CACHE =
            CacheBuilder.newBuilder().build(new CacheLoader<Integer, ImmutableList<Integer>>() {
                @Override
                public ImmutableList<Integer> load(Integer tupleLength) throws Exception {
                    int[] goalResults = new int[tupleLength];
                    Arrays.fill(goalResults, 100);
                    return canonicalize(goalResults);
                }
            });
    public static ImmutableList<Integer> getAllHundreds(int numRoles) {
        return ALL_HUNDREDS_CACHE.getUnchecked(numRoles);
    }
}
