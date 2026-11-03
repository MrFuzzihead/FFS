package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.FakeBlockGrid;
import com.lordmau5.ffs.test.TestBlock;

class TankStructureTest {

    private static final Block SHELL = new TestBlock("ffs_test_shell");
    private static final Block RESIDUAL_HEAT = new TestBlock("ffs_test_residual_heat") {

        @Override
        public String getUnlocalizedName() {
            return "railcraft.residual.heat";
        }
    };

    @Test
    void classifiesInteriorFaceAndEdge() {
        assertEquals(TankStructure.CellType.INTERIOR, TankStructure.classify(2, 2, 2, 0, 0, 0, 4, 4, 4));
        assertEquals(TankStructure.CellType.FACE, TankStructure.classify(0, 2, 2, 0, 0, 0, 4, 4, 4));
        assertEquals(TankStructure.CellType.FACE, TankStructure.classify(2, 4, 2, 0, 0, 0, 4, 4, 4));
        assertEquals(TankStructure.CellType.EDGE, TankStructure.classify(0, 0, 2, 0, 0, 0, 4, 4, 4));
        assertEquals(TankStructure.CellType.EDGE, TankStructure.classify(0, 0, 0, 0, 0, 0, 4, 4, 4));
    }

    @Test
    void scansAShellWithoutAllocatingInteriorCells() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        TankStructure structure = TankStructure
            .scan(view(grid, SHELL), new Position3D(0, 0, 0), new Position3D(4, 4, 4));

        assertTrue(structure.validBounds);
        assertTrue(structure.interiorAirValid);
        assertEquals(44, structure.edges.size()); // 12 * (5 - 2) + 8
        assertEquals(54, structure.faces.size()); // 6 * (5 - 2)^2
        assertEquals(27, structure.interiorCount); // (5 - 2)^3
        assertTrue(structure.interior.contains(LongSet.pack(1, 1, 1)));
        assertTrue(structure.interior.contains(LongSet.pack(3, 3, 3)));
        assertFalse(structure.interior.contains(LongSet.pack(0, 0, 0)));
        assertTrue(structure.edges.get(0).block.hasBlock());
        assertTrue(structure.faces.get(0).block.hasBlock());
    }

    @Test
    void scansAMinimumThreeByThreeTank() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 2, 2, 2);
        TankStructure structure = TankStructure
            .scan(view(grid, SHELL), new Position3D(0, 0, 0), new Position3D(2, 2, 2));

        assertEquals(20, structure.edges.size());
        assertEquals(6, structure.faces.size());
        assertEquals(1, structure.interiorCount);
        assertTrue(structure.interior.contains(LongSet.pack(1, 1, 1)));
    }

    @Test
    void nonAirInteriorIsRejected() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        grid.solid(2, 2, 2);

        TankStructure structure = TankStructure
            .scan(view(grid, SHELL), new Position3D(0, 0, 0), new Position3D(4, 4, 4));

        assertTrue(structure.validBounds);
        assertFalse(structure.interiorAirValid);
    }

    @Test
    void residualHeatInteriorIsAccepted() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        grid.solid(2, 2, 2);

        TankStructure structure = TankStructure
            .scan(view(grid, RESIDUAL_HEAT), new Position3D(0, 0, 0), new Position3D(4, 4, 4));

        assertTrue(structure.interiorAirValid);
    }

    @Test
    void invertedBoundsAndNullArgumentsAreRejected() {
        FakeBlockGrid grid = new FakeBlockGrid().shell(0, 0, 0, 4, 4, 4);
        TankStructure.CellView view = view(grid, SHELL);

        assertFalse(TankStructure.scan(view, new Position3D(4, 4, 4), new Position3D(0, 0, 0)).validBounds);
        assertFalse(
            TankStructure
                .scan((TankStructure.CellView) null, new Position3D(0, 0, 0), new Position3D(4, 4, 4)).validBounds);
        assertFalse(TankStructure.scan(view, null, new Position3D(4, 4, 4)).validBounds);
    }

    private static TankStructure.CellView view(FakeBlockGrid grid, Block block) {
        return new TankStructure.CellView() {

            @Override
            public Block getBlock(int x, int y, int z) {
                return grid.isAir(x, y, z) ? null : block;
            }

            @Override
            public int getMetadata(int x, int y, int z) {
                return 0;
            }

            @Override
            public boolean isAir(int x, int y, int z) {
                return grid.isAir(x, y, z);
            }
        };
    }
}
