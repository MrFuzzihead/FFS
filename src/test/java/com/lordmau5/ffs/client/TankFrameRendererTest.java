package com.lordmau5.ffs.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBlock;

class TankFrameRendererTest {

    @Test
    void tracksThePassSuppliedByTheChunkBuilder() {
        try {
            TankFrameRenderer.setPass(0);
            assertEquals(0, TankFrameRenderer.getPassForFrameRender());

            TankFrameRenderer.setPass(1);
            assertEquals(1, TankFrameRenderer.getPassForFrameRender());
        } finally {
            TankFrameRenderer.setPass(-1);
        }
    }

    @Test
    void opaqueFramesOnlyRenderInTheSolidPass() {
        Block opaque = new TestBlock("ffs_test_opaque_frame");

        assertTrue(TankFrameRenderer.shouldRenderInPass(opaque, true, 0));
        assertFalse(TankFrameRenderer.shouldRenderInPass(opaque, true, 1));
        assertFalse(TankFrameRenderer.shouldRenderInPass(opaque, true, 2));
    }

    @Test
    void transparentFramesOnlyRenderInTheirOwnPass() {
        Block passOne = new TestBlock("ffs_test_transparent_frame") {

            @Override
            public boolean isOpaqueCube() {
                return false;
            }

            @Override
            public boolean canRenderInPass(int pass) {
                return pass == 1;
            }
        };
        Block passZero = new TestBlock("ffs_test_glass_frame") {

            @Override
            public boolean isOpaqueCube() {
                return false;
            }

            @Override
            public boolean canRenderInPass(int pass) {
                return pass == 0;
            }
        };

        assertFalse(TankFrameRenderer.shouldRenderInPass(passOne, false, 0));
        assertTrue(TankFrameRenderer.shouldRenderInPass(passOne, false, 1));
        assertFalse(TankFrameRenderer.shouldRenderInPass(passOne, false, 2));
        assertTrue(TankFrameRenderer.shouldRenderInPass(passZero, false, 0));
        assertFalse(TankFrameRenderer.shouldRenderInPass(passZero, false, 1));
    }
}
