package org.ggp.base.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class GoalTuplePoolTest extends Assert {
    @Test
    public void testCanonicalizeLists() {
        List<Integer> before1 = ImmutableList.of(100, 0, 50);
        List<Integer> before2 = ImmutableList.of(100, 0, 50);
        assertEquals(before1, before2);
        assertNotSame(before1, before2);
        List<Integer> after1 = GoalTuplePool.canonicalize(before1);
        List<Integer> after2 = GoalTuplePool.canonicalize(before2);
        assertEquals(before1, after1);
        assertEquals(before2, after2);
        assertEquals(after1, after2);
        assertSame(after1, after2);
    }

    @Test
    public void testCanonicalizeTLists() {
        ImmutableIntArray before1 = ImmutableIntArray.wrap(new int[] {100, 0, 50});
        ImmutableIntArray before2 = ImmutableIntArray.wrap(new int[] {100, 0, 50});
        assertEquals(before1, before2);
        assertNotSame(before1, before2);
        ImmutableIntArray after1 = GoalTuplePool.canonicalize(before1);
        ImmutableIntArray after2 = GoalTuplePool.canonicalize(before2);
        assertEquals(before1, after1);
        assertEquals(before2, after2);
        assertEquals(after1, after2);
        assertSame(after1, after2);
    }
}
