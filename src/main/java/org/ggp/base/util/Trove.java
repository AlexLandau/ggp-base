package org.ggp.base.util;

import gnu.trove.TIntCollection;
import gnu.trove.function.TIntFunction;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

public class Trove {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private static final TIntIntIterator EMPTY_INT_INT_ITERATOR =
            new TIntIntIterator() {
        @Override
        public void advance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int key() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setValue(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int value() {
            throw new UnsupportedOperationException();
        }
    };

    private static final TIntIterator EMPTY_INT_ITERATOR = new TIntIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int next() {
            throw new UnsupportedOperationException();
        }
    };

    private static final TIntHashSet EMPTY_INT_SET = new TIntHashSet() {
        @Override
        public boolean add(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Integer> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(int arg0) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> arg0) {
            return arg0.isEmpty();
        }

        @Override
        public boolean containsAll(TIntCollection arg0) {
            return arg0.isEmpty();
        }

        @Override
        public boolean containsAll(int[] arg0) {
            return arg0.length == 0;
        }

        @Override
        public boolean forEach(TIntProcedure arg0) {
            return true;
        }

        @Override
        public int getNoEntryValue() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public TIntIterator iterator() {
            return EMPTY_INT_ITERATOR;
        }

        @Override
        public boolean remove(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public int[] toArray() {
            return EMPTY_INT_ARRAY;
        }

        @Override
        public int[] toArray(int[] arg0) {
            return EMPTY_INT_ARRAY;
        }
    };

    private static final TIntCollection EMPTY_INT_COLLECTION = new TIntCollection() {
        @Override
        public boolean add(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Integer> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(int arg0) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> arg0) {
            return arg0.isEmpty();
        }

        @Override
        public boolean containsAll(TIntCollection arg0) {
            return arg0.isEmpty();
        }

        @Override
        public boolean containsAll(int[] arg0) {
            return arg0.length == 0;
        }

        @Override
        public boolean forEach(TIntProcedure arg0) {
            return true;
        }

        @Override
        public int getNoEntryValue() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public TIntIterator iterator() {
            return EMPTY_INT_ITERATOR;
        }

        @Override
        public boolean remove(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(TIntCollection arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(int[] arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int[] toArray() {
            return EMPTY_INT_ARRAY;
        }

        @Override
        public int[] toArray(int[] arg0) {
            return EMPTY_INT_ARRAY;
        }

    };

    private static final TIntIntMap EMPTY_INT_INT_MAP =
            new TIntIntMap() {
        @Override
        public int adjustOrPutValue(int arg0, int arg1, int arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean adjustValue(int arg0, int arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(int arg0) {
            return false;
        }

        @Override
        public boolean containsValue(int arg0) {
            return false;
        }

        @Override
        public boolean forEachEntry(TIntIntProcedure arg0) {
            return true;
        }

        @Override
        public boolean forEachKey(TIntProcedure arg0) {
            return true;
        }

        @Override
        public boolean forEachValue(TIntProcedure arg0) {
            return true;
        }

        @Override
        public int get(int arg0) {
            return 0;
        }

        @Override
        public int getNoEntryKey() {
            return 0;
        }

        @Override
        public int getNoEntryValue() {
            return 0;
        }

        @Override
        public boolean increment(int arg0) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public TIntIntIterator iterator() {
            return EMPTY_INT_INT_ITERATOR;
        }

        @Override
        public TIntSet keySet() {
            return EMPTY_INT_SET;
        }

        @Override
        public int[] keys() {
            return EMPTY_INT_ARRAY;
        }

        @Override
        public int[] keys(int[] arg0) {
            return EMPTY_INT_ARRAY;
        }

        @Override
        public int put(int arg0, int arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(
                Map<? extends Integer, ? extends Integer> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(TIntIntMap arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int putIfAbsent(int arg0, int arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int remove(int arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainEntries(TIntIntProcedure arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void transformValues(TIntFunction arg0) {
            //Do nothing
        }

        @Override
        public TIntCollection valueCollection() {
            return EMPTY_INT_COLLECTION;
        }

        @Override
        public int[] values() {
            return EMPTY_INT_ARRAY;
        }

        @Override
        public int[] values(int[] arg0) {
            return EMPTY_INT_ARRAY;
        }};
        public static TIntIntMap emptyIntIntMap() {
            return EMPTY_INT_INT_MAP;
        }
        public static TIntSet emptyIntSet() {
            return EMPTY_INT_SET;
        }

        public static TIntSet singletonSet(final int value) {
            return new TIntSet() {
                @Override
                public int getNoEntryValue() {
                    return 0;
                }

                @Override
                public int size() {
                    return 1;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean contains(int entry) {
                    return entry == value;
                }

                @Override
                public TIntIterator iterator() {
                    return singletonIterator(value);
                }

                @Override
                public int[] toArray() {
                    return new int[] { value };
                }

                @Override
                public int[] toArray(int[] dest) {
                    dest[0] = value;
                    return dest;
                }

                @Override
                public boolean add(int entry) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean remove(int entry) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean containsAll(Collection<?> collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean containsAll(TIntCollection collection) {
                    return containsAll(collection.toArray());
                }

                @Override
                public boolean containsAll(int[] array) {
                    for (int i = 0; i < array.length; i++) {
                        if (array[i] != value) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public boolean addAll(Collection<? extends Integer> collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean addAll(TIntCollection collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean addAll(int[] array) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean retainAll(Collection<?> collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean retainAll(TIntCollection collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean retainAll(int[] array) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(Collection<?> collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(TIntCollection collection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(int[] array) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean forEach(TIntProcedure procedure) {
                    return procedure.execute(value);
                }

            };
        }
        public static TIntIterator singletonIterator(final int value) {
            return new TIntIterator() {
                private boolean done = false;
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public int next() {
                    if (done) {
                        throw new NoSuchElementException();
                    }
                    done = true;
                    return value;
                }
            };
        }

}
