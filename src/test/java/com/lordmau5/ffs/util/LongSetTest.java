package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LongSetTest {

    @Test
    void addsAndFindsValues() {
        LongSet set = new LongSet();
        assertTrue(set.add(1L));
        assertTrue(set.add(2L));
        assertFalse(set.add(1L));
        assertEquals(2, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertFalse(set.contains(3L));
    }

    @Test
    void growsBeyondTheInitialCapacity() {
        LongSet set = new LongSet(2);
        for (long i = 0; i < 10_000L; i++) {
            assertTrue(set.add(i * 31L));
        }
        assertEquals(10_000, set.size());
        for (long i = 0; i < 10_000L; i++) {
            assertTrue(set.contains(i * 31L));
        }
    }

    @Test
    void packsSignedCoordinatesUniquely() {
        long a = LongSet.pack(1, 2, 3);
        long b = LongSet.pack(-1, -2, -3);
        long c = LongSet.pack(1, 2, 4);

        assertEquals(a, LongSet.pack(1, 2, 3));
        assertFalse(a == b);
        assertFalse(a == c);
        assertFalse(b == c);
    }

    @Test
    void negativeCoordinatesWork() {
        LongSet set = new LongSet();
        assertTrue(set.add(LongSet.pack(-100, 64, -300)));
        assertTrue(set.contains(LongSet.pack(-100, 64, -300)));
        assertFalse(set.contains(LongSet.pack(-100, 64, -299)));
    }

    @Test
    void emptySetReportsEmpty() {
        LongSet set = new LongSet();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains(0L));
    }
}
