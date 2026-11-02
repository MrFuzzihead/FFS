package com.lordmau5.ffs.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

class ValveFaceTest {

    @Test
    void onlyTheInsideFaceIsAccented() {
        assertTrue(ValveFace.isActiveFace(ForgeDirection.UP, ForgeDirection.UP.ordinal()));
        assertTrue(ValveFace.isActiveFace(ForgeDirection.EAST, ForgeDirection.EAST.ordinal()));
        assertFalse(ValveFace.isActiveFace(ForgeDirection.UP, ForgeDirection.DOWN.ordinal()));
        assertFalse(ValveFace.isActiveFace(ForgeDirection.UNKNOWN, 0));
        assertFalse(ValveFace.isActiveFace(null, 0));
    }
}
