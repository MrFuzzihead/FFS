package com.lordmau5.ffs.compat;

import com.lordmau5.ffs.compat.computercraft.CompatibilityComputerCraft;
import com.lordmau5.ffs.compat.top.CompatibilityTOP;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public class Compatibility {

    public static boolean isTOPLoaded;
    public static boolean isCCLoaded;

    public static void init(IEventBus bus) {
        isTOPLoaded = ModList.get().isLoaded("theoneprobe");
        isCCLoaded = ModList.get().isLoaded("computercraft");

        if (isCCLoaded) {
            CompatibilityComputerCraft.initialize(bus);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        if (isTOPLoaded) {
            CompatibilityTOP.register();
        }
    }
}
