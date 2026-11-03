package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TankMathTest {

    @Test
    void capacityUsesTheRequestedVolume() {
        assertEquals(1_600_000, TankMath.capacityInt(true, 12, 40, 100, 16000));
        assertEquals((12 + 40 + 100) * 16000L, TankMath.capacityInt(false, 12, 40, 100, 16000));
    }

    @Test
    void capacityTreatsInvalidMbValuesAsOne() {
        assertEquals(100, TankMath.capacityInt(true, 0, 0, 100, 0));
        assertEquals(100, TankMath.capacityInt(true, 0, 0, 100, -500));
    }

    @Test
    void capacitySaturatesInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE, TankMath.capacityInt(false, 40_000, 40_000, 40_000, 50_000));
        assertEquals(Integer.MAX_VALUE, TankMath.capacityInt(true, 0, 0, 2, Integer.MAX_VALUE));
    }

    @Test
    void fillFractionIsClampedAndSafe() {
        assertEquals(0.0D, TankMath.fillFraction(0, 100));
        assertEquals(0.5D, TankMath.fillFraction(50, 100));
        assertEquals(1.0D, TankMath.fillFraction(100, 100));
        assertEquals(1.0D, TankMath.fillFraction(200, 100));
        assertEquals(0.0D, TankMath.fillFraction(50, 0));
        assertEquals(0.0D, TankMath.fillFraction(-50, 100));
    }

    @Test
    void fillPercentageUsesZeroToOneHundred() {
        assertEquals(0.0D, TankMath.fillPercentage(0, 100));
        assertEquals(50.0D, TankMath.fillPercentage(50, 100));
        assertEquals(100.0D, TankMath.fillPercentage(100, 100));
        assertEquals(0.0D, TankMath.fillPercentage(10, 0));
    }

    @Test
    void comparatorOutputIsZeroToFifteen() {
        assertEquals(0, TankMath.comparatorOutput(0, 100));
        assertEquals(7, TankMath.comparatorOutput(50, 100));
        assertEquals(15, TankMath.comparatorOutput(100, 100));
        assertEquals(15, TankMath.comparatorOutput(200, 100));
        assertEquals(0, TankMath.comparatorOutput(10, 0));
    }

    @Test
    void autoOutputUsesTheFluidHeight() {
        assertTrue(TankMath.canAutoOutput(0.5D, 10, 2, false));
        assertFalse(TankMath.canAutoOutput(0.1D, 10, 2, false));
        assertFalse(TankMath.canAutoOutput(0.5D, 10, 2, true));
        assertFalse(TankMath.canAutoOutput(0.0D, 10, 2, false));
        assertFalse(TankMath.canAutoOutput(0.5D, 0, 2, false));
        assertTrue(TankMath.canAutoOutput(0.01D, 10, 0, false));
    }

    @Test
    void fillHeightRespectsAutoOutput() {
        assertTrue(TankMath.canFillFromHeight(2, 10, 0.0D, true));
        assertTrue(TankMath.canFillFromHeight(8, 10, 0.5D, true));
        assertFalse(TankMath.canFillFromHeight(2, 10, 0.5D, true));
        assertTrue(TankMath.canFillFromHeight(2, 10, 0.5D, false));
        assertTrue(TankMath.canFillFromHeight(2, 0, 0.5D, true));
    }

    @Test
    void gasAutoOutputUsesTheLowerSurface() {
        assertTrue(TankMath.canAutoOutput(1.0D, 10, 0, true));
        assertTrue(TankMath.canAutoOutput(0.5D, 10, 5, true));
        assertFalse(TankMath.canAutoOutput(0.5D, 10, 4, true));
        assertFalse(TankMath.canAutoOutput(0.0D, 10, 9, true));
    }

    @Test
    void gasFillHeightUsesTheLowerSurface() {
        assertTrue(TankMath.canFillFromHeight(4, 10, 0.5D, true, true));
        assertFalse(TankMath.canFillFromHeight(5, 10, 0.5D, true, true));
        assertTrue(TankMath.canFillFromHeight(0, 10, 0.0D, true, true));
        assertFalse(TankMath.canFillFromHeight(9, 10, 1.0D, true, true));
        assertTrue(TankMath.canFillFromHeight(9, 10, 0.5D, false, true));
    }

    @Test
    void leakHardnessNeverProducesANonPositiveBound() {
        assertEquals(1, TankMath.leakHardnessTicks(0.0F));
        assertEquals(1, TankMath.leakHardnessTicks(-1.0F));
        assertEquals(1, TankMath.leakHardnessTicks(Float.NaN));
        assertEquals(1, TankMath.leakHardnessTicks(0.001F));
        assertEquals(50, TankMath.leakHardnessTicks(0.5F));
        assertEquals(100, TankMath.leakHardnessTicks(1.0F));
        assertEquals(Integer.MAX_VALUE, TankMath.leakHardnessTicks(Float.MAX_VALUE));
    }

    @Test
    void leakHardnessValidityMatchesTheFilter() {
        assertFalse(TankMath.isValidLeakHardness(0.0F));
        assertFalse(TankMath.isValidLeakHardness(-1.0F));
        assertFalse(TankMath.isValidLeakHardness(Float.NaN));
        assertTrue(TankMath.isValidLeakHardness(0.5F));
        assertTrue(TankMath.isValidLeakHardness(1.0F));
        assertFalse(TankMath.isValidLeakHardness(1.01F));
    }

    @Test
    void fluidSyncIsThrottled() {
        assertTrue(TankMath.shouldSyncFluid(0, -1, 2));
        assertTrue(TankMath.shouldSyncFluid(10, 0, 2));
        assertFalse(TankMath.shouldSyncFluid(1, 0, 2));
        assertTrue(TankMath.shouldSyncFluid(2, 0, 2));
        assertFalse(TankMath.shouldSyncFluid(0, 0, 0));
        assertTrue(TankMath.shouldSyncFluid(1, 0, 0));
        assertFalse(TankMath.shouldSyncFluid(0, 10, 2));
    }
}
