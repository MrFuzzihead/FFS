package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;

class TankFluidHandlerTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void detachedValveIsEmptyAndRejectsFluid() {
        TileEntityValve valve = new TileEntityValve();

        assertEquals(0, TankFluidHandler.fill(valve, null, true));
        assertNull(TankFluidHandler.drain(valve, 1000, true));
        assertFalse(TankFluidHandler.canFillIncludingContainers(valve, ForgeDirection.UNKNOWN, null));
        assertFalse(TankFluidHandler.canFill(valve, ForgeDirection.UNKNOWN, null));
        assertFalse(TankFluidHandler.canDrain(valve, ForgeDirection.UNKNOWN, null));
        assertNull(valve.getFluidStackInternal());
        assertEquals(0, valve.getFluidCapacityInternal());
    }
}
