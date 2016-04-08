package org.ggp.base.util;

import gnu.trove.map.TIntDoubleMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

public class Probs {

    private static final double EPSILON = 1e-6;

    public static boolean containsProbAboveOne(Collection<Double> values) {
        for (double prob : values) {
            if (prob > 1.0 + EPSILON) {
                return true;
            }
        }
        return false;
    }

    public static void validateNoneAboveOne(Collection<Double> values) {
        for (double prob : values) {
            Preconditions.checkState(prob <= 1.0 + EPSILON, "Probability had value above 1: %s", prob);
        }
    }

    public static void validateNoneAboveOne(double[] values) {
        for (double prob : values) {
            Preconditions.checkState(prob <= 1.0 + EPSILON, "Probability had value above 1: %s", prob);
        }
    }

    public static void verifyAddsUpToAroundOne(Collection<Double> values) {
        double sum = 0.0;
        for (double val : values) {
            sum += val;
        }
        Preconditions.checkState(Math.abs(1.0 - sum) < EPSILON, "sum is %s", sum);
    }

    public static void verifyAddsUpToAroundOne(double[] values) {
        double sum = 0.0;
        for (double val : values) {
            sum += val;
        }
        Preconditions.checkState(Math.abs(1.0 - sum) < EPSILON, "sum is %s", sum);
    }

    //Does nothing if no key has a non-zero probability.
    public static <K> void normalize(Map<K, Double> dist) {
        double sum = 0.0;
        for (double prob : dist.values()) {
            sum += prob;
        }
        if (sum > 0.0) {
            for (Entry<K, Double> entry : dist.entrySet()) {
                double prob = entry.getValue();
                double reweightedProb = prob / sum;
                entry.setValue(reweightedProb);
            }
        }
    }

    //Does nothing if no key has a non-zero probability.
    public static void normalize(TIntDoubleMap dist) {
        double sum = 0.0;
        for (double prob : dist.values()) {
            sum += prob;
        }
        if (sum > 0.0) {
            for (int key : dist.keys()) {
                double prob = dist.get(key);
                double reweightedProb = prob / sum;
                dist.put(key, reweightedProb);
            }
        }
    }

    public static <K> void increment(Map<K, Double> distribution,
            K key, double increment) {
        Double curValue = distribution.get(key);
        if (curValue == null) {
            distribution.put(key, increment);
        } else {
            distribution.put(key, curValue + increment);
        }
    }

    public static void increment(TIntDoubleMap distribution,
            int key, double increment) {
        distribution.adjustOrPutValue(key, increment, increment);
    }

    //Returns the highest key with a non-zero value
    public static int getHighestKey(
            Map<Integer, Double> overallScoreDistribution) {
        return Ordering.natural().max(Maps.filterValues(overallScoreDistribution,
                new Predicate<Double>() {
            @Override
            public boolean apply(Double input) {
                return input > 0.0;
            }
        }).keySet());
    }

}
