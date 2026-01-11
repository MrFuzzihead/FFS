package com.lordmau5.ffs.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {
    public static final ServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.ConfigValue<Integer> mbPerTankBlock;
    public final ModConfigSpec.ConfigValue<Integer> maxAirBlocks;

    private ServerConfig(ModConfigSpec.Builder builder) {
        mbPerTankBlock = builder
                .comment("How many millibuckets can each block within the tank store?")
                .translation("ffs.config.general.mb_per_tank_block")
                .defineInRange("general.mb_per_tank_block", 16_000, 1, Integer.MAX_VALUE);

        maxAirBlocks = builder
                .comment("Define the maximum number of air blocks a tank can have. 8192 have been tested to not cause any noticeable lag.")
                .translation("ffs.config.general.max_air_blocks")
                .defineInRange("general.max_air_blocks", 8_192, 3, 65_536);
    }

    static {
        Pair<ServerConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(ServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
