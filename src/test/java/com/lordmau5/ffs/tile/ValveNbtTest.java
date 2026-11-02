package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;
import com.lordmau5.ffs.util.Position3D;

class ValveNbtTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void masterStateRoundTrips() {
        TileEntityValve source = new TileEntityValve();
        source.isValid = true;
        source.setInside(ForgeDirection.EAST);
        source.setMaster(true);
        source.tankHeight = 4;
        source.valveHeightPosition = 2;
        source.setAutoOutputFlag(true);
        source.setValveName("test_valve");
        source.bottomDiagFrame = new Position3D(1, 2, 3);
        source.topDiagFrame = new Position3D(5, 6, 7);
        source.setFluidCapacityInternal(16000);

        NBTTagCompound tag = new NBTTagCompound();
        ValveNbt.writeFrom(source, tag);

        TileEntityValve target = new TileEntityValve();
        ValveNbt.apply(target, tag, "fallback");

        assertTrue(target.isValid);
        assertEquals(ForgeDirection.EAST, target.getInside());
        assertTrue(target.isMaster());
        assertEquals(4, target.tankHeight);
        assertEquals(2, target.valveHeightPosition);
        assertTrue(target.getAutoOutputFlag());
        assertEquals("test_valve", target.getValveName());
        assertEquals(new Position3D(1, 2, 3), target.bottomDiagFrame);
        assertEquals(new Position3D(5, 6, 7), target.topDiagFrame);
        assertEquals(16000, target.getFluidCapacityInternal());
        assertNull(target.getFluidStackInternal());
    }

    @Test
    void slaveMasterLinkRoundTrips() {
        TileEntityValve source = new TileEntityValve();
        source.setMaster(false);
        source.setMasterPos(new Position3D(10, 64, -3));

        NBTTagCompound tag = new NBTTagCompound();
        ValveNbt.writeFrom(source, tag);

        TileEntityValve target = new TileEntityValve();
        ValveNbt.apply(target, tag, "fallback");

        assertFalse(target.isMaster());
        assertEquals(new Position3D(10, 64, -3), target.getMasterValvePos());
    }

    @Test
    void missingTagsProduceSafeDefaults() {
        TileEntityValve target = new TileEntityValve();
        ValveNbt.apply(target, new NBTTagCompound(), "fallback");

        assertFalse(target.isValid);
        assertEquals(ForgeDirection.DOWN, target.getInside());
        assertFalse(target.isMaster());
        assertEquals(0, target.tankHeight);
        assertEquals(0, target.valveHeightPosition);
        assertFalse(target.getAutoOutputFlag());
        assertEquals("fallback", target.getValveName());
        assertNull(target.bottomDiagFrame);
        assertNull(target.topDiagFrame);
        assertEquals(0, target.getFluidCapacityInternal());
    }

    @Test
    void stateCodecHandlesPositions() {
        ValveState state = new ValveState();
        state.isValid = true;
        state.masterValvePos = new Position3D(1, 2, 3);
        state.bottomDiagFrame = new Position3D(4, 5, 6);
        state.topDiagFrame = new Position3D(7, 8, 9);
        state.valveName = "state";

        NBTTagCompound tag = new NBTTagCompound();
        ValveNbt.write(tag, state);
        ValveState loaded = ValveNbt.read(tag, "fallback");

        assertTrue(loaded.isValid);
        assertEquals(new Position3D(1, 2, 3), loaded.masterValvePos);
        assertEquals(new Position3D(4, 5, 6), loaded.bottomDiagFrame);
        assertEquals(new Position3D(7, 8, 9), loaded.topDiagFrame);
        assertEquals("state", loaded.valveName);
    }
}
