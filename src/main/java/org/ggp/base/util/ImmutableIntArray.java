package org.ggp.base.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;

import net.alloyggp.escaperope.rope.ropify.CoreWeavers;
import net.alloyggp.escaperope.rope.ropify.ListWeaver;
import net.alloyggp.escaperope.rope.ropify.RopeBuilder;
import net.alloyggp.escaperope.rope.ropify.RopeList;
import net.alloyggp.escaperope.rope.ropify.Weaver;

public class ImmutableIntArray {
    private final int[] array;

    private ImmutableIntArray(int[] array) {
        if (array == null) {
            throw new NullPointerException();
        }
        this.array = array;
    }

    public static ImmutableIntArray copyOf(int[] input) {
        //Copying an array is the main justified use of clone()
        return new ImmutableIntArray(input.clone());
    }

    /**
     * This should only be called on an array that is known to not be referenced
     * elsewhere, ideally a newly created array.
     */
    public static ImmutableIntArray wrap(int... inputs) {
        return new ImmutableIntArray(inputs);
    }

    public int get(int index) {
        return array[index];
    }

    public int size() {
        return array.length;
    }

    public void forEach(IntConsumer consumer) {
        for (int i = 0; i < array.length; i++) {
            consumer.accept(array[i]);
        }
    }

    public static interface IndexedIntConsumer {
        void accept(int index, int value);
    }

    public void forEachIndexed(IndexedIntConsumer consumer) {
        for (int i = 0; i < array.length; i++) {
            consumer.accept(i, array[i]);
        }
    }

    //TODO: Tweak this?
    //	private final static HashFunction HASH_FUNCTION = Hashing.goodFastHash(32);
    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
        //		Hasher hasher = HASH_FUNCTION.newHasher();
        //		for (int i = 0; i < array.length; i++) {
        //			hasher.putInt(array[i]);
        //		}
        //		return hasher.hash().asInt();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableIntArray other = (ImmutableIntArray) obj;
        if (!Arrays.equals(array, other.array))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }

    public static ImmutableIntArray empty() {
        return EMPTY;
    }

    private static final ImmutableIntArray EMPTY = wrap();

    public static ImmutableIntArray copyOf(Collection<Integer> collection) {
        int[] array = new int[collection.size()];
        int i = 0;
        Iterator<Integer> itr = collection.iterator();
        while (itr.hasNext()) {
            array[i] = itr.next();
            i++;
        }
        return wrap(array);
    }

    public IntStream stream() {
        return Arrays.stream(array);
    }

    public ImmutableList<Integer> asList() {
        return stream().boxed().collect(Immutables.collectList());
    }

    public static final Weaver<ImmutableIntArray> WEAVER = new ListWeaver<ImmutableIntArray>() {
        @Override
        protected void addToList(ImmutableIntArray object, RopeBuilder list) {
            list.add(object.array, CoreWeavers.INT_ARRAY);
        }

        @Override
        protected ImmutableIntArray fromRope(RopeList list) {
            return wrap(list.get(0, CoreWeavers.INT_ARRAY));
        }
    };
}
