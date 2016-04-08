package org.ggp.base.util;

// Primitive version of an ArrayDeque<Integer>.
public class IntArrayQueue {
    //TODO: This is very delicate, any better options here?
    public static final int NOTHING_TO_RETURN_VALUE = 0;

    private int[] elements;
    private int head;
    private int tail;

    public IntArrayQueue() {
        elements = new int[16];
        //		blankOutArray(elements);
    }

    //	private void blankOutArray(int[] array) {
    //		for (int i = 0; i < array.length; i++) {
    //			array[i] = NOTHING_TO_RETURN_VALUE;
    //		}
    //	}

    public void add(int e) {
        elements[tail] = e;
        if ( (tail = (tail + 1) & (elements.length - 1)) == head)
            doubleCapacity();
    }

    private void doubleCapacity() {
        assert head == tail;
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = n << 1;
        if (newCapacity < 0)
            throw new IllegalStateException("Sorry, deque too big");
        int[] a = new int[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }

    public int poll() {
        int h = head;
        int result = elements[h]; // Element is null if deque empty
        if (result == NOTHING_TO_RETURN_VALUE)
            return NOTHING_TO_RETURN_VALUE;
        elements[h] = NOTHING_TO_RETURN_VALUE;     // Must null out slot
        head = (h + 1) & (elements.length - 1);
        return result;
    }
}
