package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.FakeBlockGrid;

class TankGeometryTest {

    @Test
    void measuresAValidShell() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 6, 4, 4);

        TankGeometry.Shape shape = TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 7);

        assertTrue(shape.valid);
        assertEquals(new Position3D(0, 0, 0), shape.bottomDiag);
        assertEquals(new Position3D(6, 4, 4), shape.topDiag);
        assertEquals(3, shape.tankHeight);
        assertEquals(0, shape.lengths[ForgeDirection.WEST.ordinal()]);
        assertEquals(4, shape.lengths[ForgeDirection.EAST.ordinal()]);
        assertEquals(1, shape.lengths[ForgeDirection.DOWN.ordinal()]);
        assertEquals(1, shape.lengths[ForgeDirection.UP.ordinal()]);
        assertEquals(1, shape.lengths[ForgeDirection.NORTH.ordinal()]);
        assertEquals(1, shape.lengths[ForgeDirection.SOUTH.ordinal()]);
    }

    @Test
    void measuresAValveInTheFloor() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);

        TankGeometry.Shape shape = TankGeometry.measure(grid, 2, 0, 2, ForgeDirection.UP, 5);

        assertTrue(shape.valid);
        assertEquals(new Position3D(0, 0, 0), shape.bottomDiag);
        assertEquals(new Position3D(4, 4, 4), shape.topDiag);
        assertEquals(3, shape.tankHeight);
        assertEquals(0, shape.lengths[ForgeDirection.DOWN.ordinal()]);
        assertEquals(2, shape.lengths[ForgeDirection.UP.ordinal()]);
    }

    @Test
    void maxSizeIncludesTheFrame() {
        FakeBlockGrid sevenWide = new FakeBlockGrid().shell(0, 0, 0, 6, 4, 4);

        assertTrue(TankGeometry.measure(sevenWide, 0, 2, 2, ForgeDirection.EAST, 7).valid);
        assertFalse(TankGeometry.measure(sevenWide, 0, 2, 2, ForgeDirection.EAST, 6).valid);

        FakeBlockGrid fiveWide = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        assertTrue(TankGeometry.measure(fiveWide, 0, 2, 2, ForgeDirection.EAST, 5).valid);
    }

    @Test
    void minimumSizeThreeIsSupported() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 2, 2, 2);

        TankGeometry.Shape shape = TankGeometry.measure(grid, 0, 1, 1, ForgeDirection.EAST, 3);

        assertTrue(shape.valid);
        assertEquals(0, shape.lengths[ForgeDirection.WEST.ordinal()]);
        assertEquals(0, shape.lengths[ForgeDirection.EAST.ordinal()]);
        assertEquals(1, shape.tankHeight);
    }

    @Test
    void missingWallIsRejectedInsteadOfUsingStaleLengths() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        assertTrue(TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 5).valid);

        // Remove the east wall block directly in front of the valve.
        grid.air(4, 2, 2);

        TankGeometry.Shape shape = TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 5);
        assertFalse(shape.valid);
    }

    @Test
    void openAreaIsRejected() {
        FakeBlockGrid grid = new FakeBlockGrid();
        assertFalse(TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 5).valid);
    }

    @Test
    void unknownInsideIsRejected() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        assertFalse(TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.UNKNOWN, 5).valid);
    }

    @Test
    void invalidArgumentsAreRejected() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        assertFalse(TankGeometry.measure(null, 0, 2, 2, ForgeDirection.EAST, 5).valid);
        assertFalse(TankGeometry.measure(grid, 0, 2, 2, null, 5).valid);
        assertFalse(TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 2).valid);
    }

    @Test
    void resultsAreIndependentBetweenCalls() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        TankGeometry.Shape first = TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 5);
        assertTrue(first.valid);

        first.lengths[ForgeDirection.EAST.ordinal()] = 999;

        TankGeometry.Shape second = TankGeometry.measure(grid, 0, 2, 2, ForgeDirection.EAST, 5);
        assertTrue(second.valid);
        assertNotEquals(999, second.lengths[ForgeDirection.EAST.ordinal()]);
        assertNotNull(second.bottomDiag);
        assertNotNull(second.topDiag);
    }
}
