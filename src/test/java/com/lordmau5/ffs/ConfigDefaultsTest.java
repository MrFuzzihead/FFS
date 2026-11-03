package com.lordmau5.ffs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ConfigDefaultsTest {

    @Test
    void defaultsMatchTheDocumentedPackaging() {
        FancyFluidStorage mod = new FancyFluidStorage();

        assertEquals("ffs", FancyFluidStorage.MODID);
        assertEquals(16, mod.MAX_SIZE);
        assertEquals(16000, mod.MB_PER_TANK_BLOCK);
        assertEquals(1300, mod.MIN_BURNABLE_TEMPERATURE);
        assertEquals(FancyFluidStorage.TankFrameMode.SAME_BLOCK, mod.TANK_FRAME_MODE);
        assertFalse(mod.INSIDE_CAPACITY);
    }
}
