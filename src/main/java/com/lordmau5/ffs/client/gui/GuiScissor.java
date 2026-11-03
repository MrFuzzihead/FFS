package com.lordmau5.ffs.client.gui;

/**
 * Converts scaled GUI coordinates into the framebuffer-pixel rectangle expected
 * by {@code glScissor} (origin bottom-left, Y flipped).
 */
public final class GuiScissor {

    private GuiScissor() {}

    /** @return {@code {x, y, width, height}} in framebuffer pixels */
    public static int[] scissorFor(int guiX, int guiY, int width, int height, int scaleFactor, int displayHeight) {
        int scale = Math.max(1, scaleFactor);
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);

        return new int[] { guiX * scale, displayHeight - (guiY + safeHeight) * scale, safeWidth * scale,
            safeHeight * scale };
    }
}
