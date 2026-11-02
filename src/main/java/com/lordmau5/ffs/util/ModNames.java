package com.lordmau5.ffs.util;

/**
 * Single source of truth for names that must match the mod container, resource
 * pack and tile-entity registry. Lowercase is required for resource domains on
 * case-sensitive filesystems.
 */
public final class ModNames {

    public static final String MODID = "ffs";
    public static final String RESOURCE_DOMAIN = "ffs";
    public static final String LEGACY_MODID = "FFS";
    public static final String BLOCK_VALVE = "blockValve";
    public static final String BLOCK_FRAME = "blockTankFrame";
    public static final String LEGACY_BLOCK_VALVE = LEGACY_MODID + ":" + BLOCK_VALVE;
    public static final String LEGACY_BLOCK_FRAME = LEGACY_MODID + ":" + BLOCK_FRAME;

    public static final String TE_VALVE = "ffs:tileEntityValve";
    public static final String TE_VALVE_LEGACY = "tileEntityValve";
    public static final String TE_FRAME = "ffs:tileEntityTankFrame";
    public static final String TE_FRAME_LEGACY = "tileEntityTankFrame";

    private ModNames() {}

    public static String resource(String path) {
        return RESOURCE_DOMAIN + ":" + path;
    }
}
