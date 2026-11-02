package com.lordmau5.ffs.test;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityTankFrame;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the minimum FML metadata needed by the unit tests.
 *
 * <p>
 * The vanilla block/fluid registries are deliberately not initialised here:
 * {@code Block.registerBlocks()} requires a LaunchClassLoader (FML's
 * {@code Loader.instance()} casts the class loader), which a plain JUnit JVM
 * does not have. Code that needs those registries is covered by the headless
 * {@code runServer} harness in CI instead.
 */
public final class TestBootstrap {

    private static boolean initialized;

    private TestBootstrap() {}

    public static synchronized void ensureGameRegistry() {
        if (initialized) {
            return;
        }
        initialized = true;

        // TileEntityValve reads its config defaults from the mod instance.
        if (FancyFluidStorage.instance == null) {
            FancyFluidStorage.instance = new FancyFluidStorage();
        }

        // TileEntity.writeToNBT refuses to serialise unmapped classes.
        GameRegistry.registerTileEntity(TileEntityTankFrame.class, "ffs_test_tank_frame");
    }
}
