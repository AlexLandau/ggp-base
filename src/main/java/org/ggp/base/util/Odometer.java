package org.ggp.base.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

//Not thread-safe
/**
 * The Odometer is a way of iterating through a Cartesian product.
 *
 * The 0-indexed value will update most often, followed by the 1-index,
 * and so on.
 *
 * Odometer is not thread-safe.
 */
public class Odometer<T> implements Iterator<List<T>> {
    private final List<Integer> odometer;
    private final ImmutableList<ImmutableList<T>> values;
    private final ImmutableList<ImmutableMap<T, Integer>> valuesIndex;

    public Odometer(ImmutableList<ImmutableList<T>> values,
            ImmutableList<ImmutableMap<T, Integer>> valuesIndex) {
        Preconditions.checkArgument(values.size() > 0, "The size of an odometer cannot be 0");
        this.values = values;
        this.valuesIndex = valuesIndex;
        this.odometer = Lists.newArrayListWithCapacity(values.size());
        for (int i = 0; i < values.size(); i++) {
            odometer.add(0);
        }
    }

    public static <T> Odometer<T> create(List<List<T>> values) {
        ImmutableList<ImmutableList<T>> immutableValues = values.stream()
                .map(ImmutableList::copyOf)
                .collect(Immutables.collectList());
        ImmutableList<ImmutableMap<T, Integer>> valuesIndex = values.stream()
                .map(AlloyUtils::getListIndex)
                .collect(Immutables.collectList());
        return new Odometer<T>(immutableValues, valuesIndex);
    }

    @Override
    public boolean hasNext() {
        int lastIndex = values.size() - 1;
        return odometer.get(lastIndex) < values.get(lastIndex).size();
    }

    @Override
    public List<T> next() {
        List<T> result = collectValues();
        increment();
        return result;
    }

    private void increment() {
        increment(0);
    }

    private void increment(int slotToUpdate) {
        Preconditions.checkElementIndex(slotToUpdate, odometer.size());
        int lastIndex = odometer.size() - 1;
        while (slotToUpdate < lastIndex) {
            int curValue = odometer.get(slotToUpdate);
            int numValues = values.get(slotToUpdate).size();
            if (curValue + 1 < numValues) {
                break;
            }
            slotToUpdate++;
        }
        int newValue = odometer.get(slotToUpdate) + 1;
        odometer.set(slotToUpdate, newValue);
        for (int i = 0; i < slotToUpdate; i++) {
            odometer.set(i, 0);
        }
    }

    private List<T> collectValues() {
        List<T> result = Lists.newArrayListWithCapacity(values.size());
        for (int i = 0; i < values.size(); i++) {
            int curIndex = odometer.get(i);
            T valueForSlot = values.get(i).get(curIndex);
            result.add(valueForSlot);
        }
        return result;
    }

    public void skipPastValueInSlot(int slot, T value) {
        int odometerValue = valuesIndex.get(slot).get(value);
        skipPastValueInSlot(slot, odometerValue);
    }

    private void skipPastValueInSlot(int slot, int odometerValue) {
        if (odometer.get(slot) == odometerValue) {
            increment(slot);
        }
    }

}
