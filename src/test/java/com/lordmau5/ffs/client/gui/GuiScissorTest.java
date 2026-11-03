package com.lordmau5.ffs.client.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class GuiScissorTest {

    @Test
    void flipsTheGuiYAxis() {
        // GUI (10, 20), 64x50, scale 2, 480px tall framebuffer.
        assertArrayEquals(new int[] { 20, 340, 128, 100 }, GuiScissor.scissorFor(10, 20, 64, 50, 2, 480));
    }

    @Test
    void clampsScaleAndNegativeSizes() {
        assertArrayEquals(new int[] { 10, 0, 0, 0 }, GuiScissor.scissorFor(10, 480, 0, 0, 0, 480));
        assertArrayEquals(new int[] { 10, 480, 0, 0 }, GuiScissor.scissorFor(10, 0, -5, -5, 1, 480));
    }

    @Test
    void userScaleOneIsIdentityForTheYFlip() {
        assertArrayEquals(new int[] { 10, 430, 64, 50 }, GuiScissor.scissorFor(10, 0, 64, 50, 1, 480));
    }
}
