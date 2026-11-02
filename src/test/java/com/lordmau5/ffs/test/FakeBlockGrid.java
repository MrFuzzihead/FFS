package com.lordmau5.ffs.test;

import java.util.HashSet;
import java.util.Set;

import com.lordmau5.ffs.util.TankGeometry;

/**
 * Tiny in-memory block grid used by the headless geometry tests. Everything is
 * air unless marked solid.
 */
public final class FakeBlockGrid implements TankGeometry.BlockView {

    private final Set<Long> solid = new HashSet<>();

    public FakeBlockGrid solid(int x, int y, int z) {
        solid.add(key(x, y, z));
        return this;
    }

    public FakeBlockGrid air(int x, int y, int z) {
        solid.remove(key(x, y, z));
        return this;
    }

    /** Marks the shell of the inclusive box as solid, leaving the inside air. */
    public FakeBlockGrid shell(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2) {
                        solid.add(key(x, y, z));
                    } else {
                        solid.remove(key(x, y, z));
                    }
                }
            }
        }
        return this;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return !solid.contains(key(x, y, z));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
