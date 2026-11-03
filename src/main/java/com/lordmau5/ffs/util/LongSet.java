package com.lordmau5.ffs.util;

import java.util.Arrays;

/**
 * Minimal primitive open-addressing set for packed block positions. Replaces the
 * retained {@code Map<Position3D, ExtendedBlock>} interior map (one object per
 * interior block) with ~8 bytes per position.
 */
public final class LongSet {

    private static final long EMPTY = Long.MIN_VALUE;
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private long[] table;
    private int size;
    private int threshold;

    public LongSet() {
        this(16);
    }

    public LongSet(int expectedSize) {
        int capacity = 4;
        int target = Math.max(4, expectedSize + expectedSize / 2);
        while (capacity < target) {
            capacity <<= 1;
        }
        this.table = new long[capacity];
        Arrays.fill(this.table, EMPTY);
        this.threshold = capacity * 2 / 3;
    }

    public boolean add(long value) {
        if (value == EMPTY) {
            throw new IllegalArgumentException("EMPTY is reserved as the sentinel");
        }
        if (this.size >= this.threshold) {
            resize();
        }

        int mask = this.table.length - 1;
        int index = hash(value) & mask;
        while (this.table[index] != EMPTY) {
            if (this.table[index] == value) {
                return false;
            }
            index = (index + 1) & mask;
        }

        this.table[index] = value;
        this.size++;
        return true;
    }

    public boolean contains(long value) {
        int mask = this.table.length - 1;
        int index = hash(value) & mask;
        while (this.table[index] != EMPTY) {
            if (this.table[index] == value) {
                return true;
            }
            index = (index + 1) & mask;
        }
        return false;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    private void resize() {
        long[] old = this.table;
        long[] next = new long[old.length << 1];
        Arrays.fill(next, EMPTY);
        int mask = next.length - 1;

        for (long value : old) {
            if (value == EMPTY) {
                continue;
            }
            int index = hash(value) & mask;
            while (next[index] != EMPTY) {
                index = (index + 1) & mask;
            }
            next[index] = value;
        }

        this.table = next;
        this.threshold = next.length * 2 / 3;
    }

    private static int hash(long value) {
        long mixed = value * MIX;
        return (int) (mixed ^ (mixed >>> 32));
    }

    /** Packs signed world coordinates into 21 bits per axis. */
    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
