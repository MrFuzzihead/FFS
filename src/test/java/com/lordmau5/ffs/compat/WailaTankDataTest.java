package com.lordmau5.ffs.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;
import com.lordmau5.ffs.tile.TileEntityValve;

class WailaTankDataTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void detachedValveProducesEmptyData() {
        WailaTankData.Data data = WailaTankData.fromValve(new TileEntityValve(), false);

        assertNotNull(data);
        assertFalse(data.valid);
        assertFalse(data.frame);
        assertFalse(data.autoOutput);
        assertFalse(data.master);
        assertEquals(0, data.fluidAmount);
        assertEquals(0, data.capacity);
        assertEquals("", data.fluidName);
        assertNotNull(data.valveName);
        assertNull(WailaTankData.fromValve(null, true));
    }

    @Test
    void dataRoundTripsThroughNbt() {
        TileEntityValve valve = new TileEntityValve();
        NBTTagCompound tag = new NBTTagCompound();

        WailaTankData.write(tag, valve, false);
        WailaTankData.Data data = WailaTankData.read(tag);

        assertNotNull(data);
        assertEquals(valve.getValveName(), data.valveName);
        assertEquals(valve.getFluidAmount(), data.fluidAmount);
        assertEquals(valve.getCapacity(), data.capacity);
        assertEquals(valve.getAutoOutput(), data.autoOutput);
    }

    @Test
    void missingOrEmptyTagsAreSafe() {
        assertNull(WailaTankData.read(null));
        assertNull(WailaTankData.read(new NBTTagCompound()));

        NBTTagCompound tag = new NBTTagCompound();
        WailaTankData.write(tag, null, true);
        assertNull(WailaTankData.read(tag));
    }
}
