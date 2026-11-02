package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/**
 * Covers the registry-free NBT logic. The Forge {@code FluidStack} path
 * ({@code readFluidStack}/{@code writeFluidStack}) needs a launched game
 * registry and is exercised by the headless {@code runServer} CI harness.
 */
class NbtHelperTest {

    @Test
    void positionRoundTrips() {
        NBTTagCompound tag = new NBTTagCompound();
        Position3D pos = new Position3D(1, 2, 3);
        NbtHelper.writePosition(tag, "pos", pos);

        Position3D loaded = NbtHelper.readPosition(tag, "pos");
        assertNotNull(loaded);
        assertEquals(pos, loaded);
    }

    @Test
    void positionRejectsMissingAndShortArrays() {
        NBTTagCompound tag = new NBTTagCompound();
        assertNull(NbtHelper.readPosition(tag, "missing"));

        tag.setIntArray("short", new int[] { 1, 2 });
        assertNull(NbtHelper.readPosition(tag, "short"));

        tag.setIntArray("empty", new int[0]);
        assertNull(NbtHelper.readPosition(tag, "empty"));

        assertNull(NbtHelper.readPosition(null, "pos"));
    }

    @Test
    void writingNullPositionRemovesIt() {
        NBTTagCompound tag = new NBTTagCompound();
        NbtHelper.writePosition(tag, "pos", new Position3D(0, 0, 0));
        assertTrue(tag.hasKey("pos"));

        NbtHelper.writePosition(tag, "pos", null);
        assertFalse(tag.hasKey("pos"));
    }

    @Test
    void amountIsClampedToTheCapacity() {
        assertEquals(0, NbtHelper.clampAmount(0, 100));
        assertEquals(0, NbtHelper.clampAmount(-1, 100));
        assertEquals(50, NbtHelper.clampAmount(50, 100));
        assertEquals(100, NbtHelper.clampAmount(150, 100));
        assertEquals(150, NbtHelper.clampAmount(150, -1));
        assertEquals(150, NbtHelper.clampAmount(150, 0));
    }

    @Test
    void legacyFluidNameSelectionIsSafe() {
        NBTTagCompound tag = new NBTTagCompound();
        assertNull(NbtHelper.legacyFluidName(tag, "fluidName"));
        assertNull(NbtHelper.legacyFluidName(null, "fluidName"));

        tag.setString("fluidName", "");
        assertNull(NbtHelper.legacyFluidName(tag, "fluidName"));

        tag.setString("fluidName", "water");
        assertEquals("water", NbtHelper.legacyFluidName(tag, "fluidName"));
    }

    @Test
    void legacyFluidIdSelectionIsSafe() {
        NBTTagCompound tag = new NBTTagCompound();
        assertNull(NbtHelper.legacyFluidId(tag, "fluidID"));
        assertNull(NbtHelper.legacyFluidId(null, "fluidID"));

        tag.setInteger("fluidID", 0);
        assertEquals(Integer.valueOf(0), NbtHelper.legacyFluidId(tag, "fluidID"));
    }

    @Test
    void writingNeverThrowsForNullArguments() {
        NbtHelper.writePosition(null, "pos", new Position3D(0, 0, 0));
        NbtHelper.writeFluidStack(null, "fluid", null);

        NBTTagCompound tag = new NBTTagCompound();
        NbtHelper.writeFluidStack(tag, "fluid", null);
        assertFalse(tag.hasKey("fluid"));
        assertTrue(tag.hasNoTags());
    }
}
