package com.lordmau5.ffs.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBlock;

class TankFrameRendererTest {

    @Test
    void opaqueFramesOnlyRenderInTheSolidPass() {
        Block opaque = new TestBlock("ffs_test_opaque_frame");

        assertTrue(TankFrameRenderer.shouldRenderInPass(opaque, true, 0));
        assertFalse(TankFrameRenderer.shouldRenderInPass(opaque, true, 1));
        assertFalse(TankFrameRenderer.shouldRenderInPass(opaque, true, 2));
    }

    @Test
    void transparentFramesOnlyRenderWhereTheyAreAllowed() {
        Block transparent = new TestBlock("ffs_test_transparent_frame") {

            @Override
            public boolean isOpaqueCube() {
                return false;
            }

            @Override
            public boolean canRenderInPass(int pass) {
                return pass == 1;
            }
        };

        assertFalse(TankFrameRenderer.shouldRenderInPass(transparent, false, 0));
        assertTrue(TankFrameRenderer.shouldRenderInPass(transparent, false, 1));
        assertFalse(TankFrameRenderer.shouldRenderInPass(transparent, false, 2));
    }
}
