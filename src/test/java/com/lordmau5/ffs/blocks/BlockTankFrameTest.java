package com.lordmau5.ffs.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.tile.TileEntityTankFrame;
import com.lordmau5.ffs.util.ModNames;

class BlockTankFrameTest {

    @Test
    void technicalBlockHasANameAndTexture() {
        BlockTankFrame block = new BlockTankFrame();

        assertEquals("tile.blockTankFrame", block.getUnlocalizedName());
        assertTrue(block.hasTileEntity(0));
        assertTrue(block.createTileEntity(null, 0) instanceof TileEntityTankFrame);
        assertEquals("ffs", ModNames.RESOURCE_DOMAIN);
    }
}
