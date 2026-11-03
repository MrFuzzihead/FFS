package com.lordmau5.ffs.client;

/** Pure math for the fluid volume; unit-tested without an OpenGL context. */
public final class FluidRenderMath {

    private FluidRenderMath() {}

    public static double clampFill(double fill) {
        if (Double.isNaN(fill) || fill <= 0.0D) {
            return 0.0D;
        }
        return Math.min(fill, 1.0D);
    }

    /** Height of the fluid column inside the frame (the interior is height - 1). */
    public static float pureHeight(int frameHeight, double fill) {
        return (float) (Math.max(0, frameHeight - 1) * clampFill(fill));
    }

    /** Number of one-block layers to emit for a column height. */
    public static int visibleLayers(float pureHeight) {
        return pureHeight <= 0.0F ? 0 : Math.max(1, (int) Math.ceil(pureHeight));
    }

    /** Top of a layer, never below 0.01 so the lowest layer remains visible. */
    public static float layerHeight(float pureHeight, int layer) {
        float height = Math.min(pureHeight - layer, 1.0F) + layer;
        return layer == 0 ? Math.max(0.01F, height) : height;
    }
}
