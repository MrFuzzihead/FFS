package com.lordmau5.ffs.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidRenderMathTest {

    @Test
    void fillIsClamped() {
        assertEquals(0.0D, FluidRenderMath.clampFill(-1.0D));
        assertEquals(0.0D, FluidRenderMath.clampFill(Double.NaN));
        assertEquals(0.5D, FluidRenderMath.clampFill(0.5D));
        assertEquals(1.0D, FluidRenderMath.clampFill(1.5D));
    }

    @Test
    void pureHeightUsesTheInteriorHeight() {
        assertEquals(0.0F, FluidRenderMath.pureHeight(1, 0.5D));
        assertEquals(4.5F, FluidRenderMath.pureHeight(10, 0.5D));
        assertEquals(9.0F, FluidRenderMath.pureHeight(10, 1.0D));
        assertEquals(0.0F, FluidRenderMath.pureHeight(10, 0.0D));
    }

    @Test
    void visibleLayersCoverThePartialLayer() {
        assertEquals(0, FluidRenderMath.visibleLayers(0.0F));
        assertEquals(1, FluidRenderMath.visibleLayers(0.01F));
        assertEquals(5, FluidRenderMath.visibleLayers(4.5F));
        assertEquals(9, FluidRenderMath.visibleLayers(9.0F));
    }

    @Test
    void layerHeightsAreMonotonicAndKeepTheBottomVisible() {
        float pure = 4.5F;
        assertEquals(1.0F, FluidRenderMath.layerHeight(pure, 0));
        assertEquals(2.0F, FluidRenderMath.layerHeight(pure, 1));
        assertEquals(4.5F, FluidRenderMath.layerHeight(pure, 4));

        assertEquals(0.01F, FluidRenderMath.layerHeight(0.0F, 0));
        assertEquals(0.01F, FluidRenderMath.layerHeight(0.005F, 0));
    }
}
