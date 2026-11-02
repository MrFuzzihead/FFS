package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blocks.BlockTankFrame;
import com.lordmau5.ffs.blocks.BlockValve;

import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.Action;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;

class LegacyMappingsTest {

    @Test
    void recognisesOnlyTheOldUppercaseBlockNames() {
        assertTrue(LegacyMappings.handles(ModNames.LEGACY_BLOCK_VALVE));
        assertTrue(LegacyMappings.handles(ModNames.LEGACY_BLOCK_FRAME));
        assertEquals("FFS:blockValve", ModNames.LEGACY_BLOCK_VALVE);
        assertEquals("FFS:blockTankFrame", ModNames.LEGACY_BLOCK_FRAME);
        assertFalse(LegacyMappings.handles(ModNames.BLOCK_VALVE));
        assertFalse(LegacyMappings.handles(ModNames.BLOCK_FRAME));
    }

    @Test
    void remapsLegacyBlocksToTheCurrentBlocks() {
        Block valve = new BlockValve();
        Block frame = new BlockTankFrame();
        RecordingMapping valveMapping = new RecordingMapping("\u0001" + ModNames.LEGACY_BLOCK_VALVE);
        RecordingMapping frameMapping = new RecordingMapping("\u0001" + ModNames.LEGACY_BLOCK_FRAME);

        LegacyMappings.remap(event(valveMapping, frameMapping), valve, frame);

        assertSame(valve, valveMapping.remapped);
        assertSame(frame, frameMapping.remapped);
        assertEquals(Action.DEFAULT, valveMapping.getAction());
        assertEquals(Action.DEFAULT, frameMapping.getAction());
    }

    @Test
    void remapsLegacyItemBlockNamesToTheCurrentItems() {
        Block valve = new BlockValve();
        Block frame = new BlockTankFrame();
        Item valveItem = new Item();
        Item frameItem = new Item();
        RecordingMapping valveMapping = new RecordingMapping("" + ModNames.LEGACY_BLOCK_VALVE);
        RecordingMapping frameMapping = new RecordingMapping("" + ModNames.LEGACY_BLOCK_FRAME);

        LegacyMappings.remap(event(valveMapping, frameMapping), valve, frame, valveItem, frameItem);

        assertSame(valveItem, valveMapping.remappedItem);
        assertSame(frameItem, frameMapping.remappedItem);
    }

    @Test
    void leavesCurrentNamesUnhandled() {
        RecordingMapping current = new RecordingMapping("\u0001ffs:blockValve");

        LegacyMappings.remap(event(current), new BlockValve(), new BlockTankFrame());

        assertEquals(Action.DEFAULT, current.getAction());
        assertNull(current.remapped);
    }

    @Test
    void modRegistersTheMissingMappingHandler() throws Exception {
        Method method = FancyFluidStorage.class.getMethod("missingMappings", FMLMissingMappingsEvent.class);

        assertNotNull(method.getAnnotation(EventHandler.class));
    }

    private static FMLMissingMappingsEvent event(MissingMapping... mappings) {
        ArrayListMultimap<String, MissingMapping> missing = ArrayListMultimap.create();
        for (MissingMapping mapping : mappings) {
            missing.put("FFS", mapping);
        }
        return new FMLMissingMappingsEvent(missing);
    }

    private static final class RecordingMapping extends MissingMapping {

        private Block remapped;
        private Item remappedItem;

        private RecordingMapping(String name) {
            super(name, 1);
        }

        @Override
        public void remap(Block target) {
            this.remapped = target;
        }

        @Override
        public void remap(Item target) {
            this.remappedItem = target;
        }
    }
}
