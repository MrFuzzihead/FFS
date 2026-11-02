package com.lordmau5.ffs.util;

/**
 * Pure tank math. This class deliberately has no world, tile-entity or client
 * dependencies so that every rule can be exercised by the headless test suite.
 */
public final class TankMath {

    private TankMath() {}

    /**
     * Capacity in mB, computed with {@code long} arithmetic and saturated to the
     * {@code int} range used by {@code IFluidTank#getCapacity()}.
     */
    public static int capacityInt(boolean insideOnly, int edgeCount, int faceCount, int interiorCount, int mbPerBlock) {
        long blocks = insideOnly ? interiorCount : (long) edgeCount + (long) faceCount + (long) interiorCount;
        long perBlock = Math.max(1, mbPerBlock);
        long total = blocks * perBlock;

        if (total < 0 || total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) total;
    }

    /** Fluid level as a 0..1 fraction (never NaN, never above 1). */
    public static double fillFraction(int amount, int capacity) {
        if (capacity <= 0 || amount <= 0) {
            return 0.0D;
        }

        double fraction = (double) amount / (double) capacity;
        return fraction > 1.0D ? 1.0D : fraction;
    }

    /** Fluid level as a 0..100 percentage, for display. */
    public static double fillPercentage(int amount, int capacity) {
        return Math.floor(fillFraction(amount, capacity) * 100.0D);
    }

    /** Comparator signal, 0..15 inclusive (15 = full). */
    public static int comparatorOutput(int amount, int capacity) {
        if (capacity <= 0 || amount <= 0) {
            return 0;
        }

        int signal = (int) Math.floor(fillFraction(amount, capacity) * 15.0D);
        return Math.max(0, Math.min(15, signal));
    }

    /**
     * Whether a valve at {@code valvePosition} (blocks above the tank floor) can
     * auto-output. Negative-density fluids float to the top and are handled by the
     * caller/renderer, not by this bottom-up rule.
     */
    public static boolean canAutoOutput(double fillFraction, int tankHeight, int valvePosition,
        boolean negativeDensity) {
        if (tankHeight <= 0 || fillFraction <= 0.0D) {
            return false;
        }

        if (negativeDensity) {
            // Gases fill from the top; the gas is present down to the lower
            // surface. A valve at or above that surface is submerged.
            double gasBottom = (1.0D - fillFraction) * (double) tankHeight;
            return (double) valvePosition + 0.5D >= gasBottom;
        }

        return fillFraction * (double) tankHeight > (double) valvePosition - 0.5D;
    }

    /**
     * Whether a valve may accept fluid while auto-output is enabled. The valve must
     * be on the empty side of the fluid surface.
     */
    public static boolean canFillFromHeight(int valvePosition, int tankHeight, double fillFraction,
        boolean autoOutput) {
        return canFillFromHeight(valvePosition, tankHeight, fillFraction, autoOutput, false);
    }

    public static boolean canFillFromHeight(int valvePosition, int tankHeight, double fillFraction, boolean autoOutput,
        boolean negativeDensity) {
        if (!autoOutput || tankHeight <= 0) {
            return true;
        }

        double surface = negativeDensity ? (1.0D - fillFraction) * (double) tankHeight
            : fillFraction * (double) tankHeight;

        if (negativeDensity) {
            return (double) valvePosition + 0.5D <= surface;
        }

        return (double) valvePosition + 0.5D >= surface;
    }

    /**
     * Number of 1-in-N ticks used by the leak roll. Hardness 0, negative and NaN
     * would crash {@code Random#nextInt(int)}; they collapse to 1 tick.
     */
    public static int leakHardnessTicks(float hardness) {
        if (Float.isNaN(hardness) || hardness <= 0.0F) {
            return 1;
        }

        double ticks = Math.ceil((double) hardness * 100.0D);
        if (ticks < 1.0D) {
            return 1;
        }
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) ticks;
    }

    /** A frame can only leak if its hardness is positive and at most 1.0. */
    public static boolean isValidLeakHardness(float hardness) {
        return !Float.isNaN(hardness) && hardness > 0.0F && hardness <= 1.0F;
    }
}
