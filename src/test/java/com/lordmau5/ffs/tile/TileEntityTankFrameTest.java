package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBlock;
import com.lordmau5.ffs.test.TestBootstrap;
import com.lordmau5.ffs.util.ExtendedBlock;
import com.lordmau5.ffs.util.Position3D;

class TileEntityTankFrameTest {

    @BeforeAll
    static void bootstrapVanillaRegistry() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void emptyNbtLoadsWithoutBlockOrValve() {
        TileEntityTankFrame frame = new TileEntityTankFrame();
        frame.readFromNBT(new NBTTagCompound());

        assertFalse(frame.hasBlock());
        assertNull(frame.getStoredBlock());
        assertEquals(0, frame.getStoredMetadata());
        assertNull(frame.getValve());
        assertNull(frame.getValvePos());
        assertFalse(frame.isOwnedByOther(null));
    }

    @Test
    void unknownBlockIdDoesNotThrowAndIsDropped() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("blockId", 9999);
        tag.setInteger("metadata", 4);

        TileEntityTankFrame frame = new TileEntityTankFrame();
        frame.readFromNBT(tag);

        assertFalse(frame.hasBlock());
        assertNull(frame.getStoredBlock());

        NBTTagCompound out = new NBTTagCompound();
        frame.writeToNBT(out);
        assertFalse(out.hasKey("blockId"));
    }

    @Test
    void valvePositionIsPersistedWithoutLoadingTheMaster() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("valveX", 10);
        tag.setInteger("valveY", 64);
        tag.setInteger("valveZ", -3);

        TileEntityTankFrame frame = new TileEntityTankFrame();
        frame.readFromNBT(tag);

        // No world is attached, so the master cannot resolve, but the position
        // must survive a save/load cycle instead of being silently lost.
        assertNull(frame.getValve());
        assertEquals(new Position3D(10, 64, -3), frame.getValvePos());
        assertFalse(frame.isOwnedByOther(null));

        NBTTagCompound out = new NBTTagCompound();
        frame.writeToNBT(out);
        assertEquals(10, out.getInteger("valveX"));
        assertEquals(64, out.getInteger("valveY"));
        assertEquals(-3, out.getInteger("valveZ"));
    }

    @Test
    void missingValveTagsAreNotWrittenBack() {
        TileEntityTankFrame frame = new TileEntityTankFrame();
        frame.readFromNBT(new NBTTagCompound());

        NBTTagCompound out = new NBTTagCompound();
        frame.writeToNBT(out);
        assertFalse(out.hasKey("valveX"));
        assertFalse(out.hasKey("valveY"));
        assertFalse(out.hasKey("valveZ"));
    }

    @Test
    void initializeStoresTheBlockAndIsSafeWithoutAWorld() {
        TileEntityTankFrame frame = new TileEntityTankFrame();
        TestBlock block = new TestBlock("ffs_test_frame");

        frame.initialize(null, new ExtendedBlock(block, 3));

        assertTrue(frame.hasBlock());
        assertSame(block, frame.getStoredBlock());
        assertEquals(3, frame.getStoredMetadata());
    }

    @Test
    void framesDoNotTick() {
        assertFalse(new TileEntityTankFrame().canUpdate());
    }

    @Test
    void valvePositionChangesAreDetected() {
        TileEntityTankFrame frame = new TileEntityTankFrame();

        assertTrue(frame.setValvePos(new Position3D(1, 2, 3)));
        assertFalse(frame.setValvePos(new Position3D(1, 2, 3)));
        assertTrue(frame.setValvePos(new Position3D(4, 5, 6)));
        assertTrue(frame.setValvePos(null));
        assertFalse(frame.setValvePos(null));
    }

    @Test
    void worldDependentMethodsAreSafeWithoutAWorld() {
        TileEntityTankFrame frame = new TileEntityTankFrame();

        assertFalse(frame.tryBurning());
        assertTrue(frame.isFrameInvalid());
        assertNotNull(frame.getNeighborBlockOrAir(null));
        assertTrue(
            frame.getNeighborBlockOrAir(null)
                .isEmpty());

        // Must not throw when a stale frame is broken after a chunk unload.
        frame.breakFrame();
    }
}
