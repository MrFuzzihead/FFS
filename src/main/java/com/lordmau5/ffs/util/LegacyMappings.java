package com.lordmau5.ffs.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Forge's missing-mapping hook for the old uppercase {@code FFS} mod id.
 *
 * <p>
 * A 1.7.10 world stores the registry names of blocks and items (for example
 * {@code FFS:blockValve}) alongside their numeric ids. Changing the mod id to
 * {@code ffs} makes those names unknown to Forge, which would otherwise remove
 * every existing tank block on load. Remapping the old names to the new blocks
 * keeps both the blocks and their numeric ids intact.
 */
public final class LegacyMappings {

    private LegacyMappings() {}

    /** True for the two block/item names written by the old uppercase mod id. */
    public static boolean handles(String name) {
        return ModNames.LEGACY_BLOCK_VALVE.equals(name) || ModNames.LEGACY_BLOCK_FRAME.equals(name);
    }

    /**
     * Remaps legacy block entries and their ItemBlock entries. Call this from
     * an {@link FMLMissingMappingsEvent} handler while the current blocks are
     * registered.
     */
    public static void remap(FMLMissingMappingsEvent event, Block valve, Block frame) {
        remap(event, valve, frame, Item.getItemFromBlock(valve), Item.getItemFromBlock(frame));
    }

    static void remap(FMLMissingMappingsEvent event, Block valve, Block frame, Item valveItem, Item frameItem) {
        for (MissingMapping mapping : event.getAll()) {
            Block target;
            if (ModNames.LEGACY_BLOCK_VALVE.equals(mapping.name)) {
                target = valve;
            } else if (ModNames.LEGACY_BLOCK_FRAME.equals(mapping.name)) {
                target = frame;
            } else {
                continue;
            }

            if (target == null) {
                mapping.warn();
            } else if (mapping.type == GameRegistry.Type.BLOCK) {
                mapping.remap(target);
            } else {
                Item item = target == valve ? valveItem : frameItem;
                if (item == null) {
                    mapping.warn();
                } else {
                    mapping.remap(item);
                }
            }
        }
    }
}
