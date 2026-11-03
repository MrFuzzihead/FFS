package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;

import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBlock;

class ExtendedBlockTest {

    private static final Block STONE = new TestBlock("ffs_test_stone");
    private static final Block DIRT = new TestBlock("ffs_test_dirt");
    private static final Block SAME_NAME = new TestBlock("ffs_test_stone");

    @Test
    void equalityUsesBlockIdentityAndMetadata() {
        ExtendedBlock a = new ExtendedBlock(STONE, 3);
        ExtendedBlock b = new ExtendedBlock(STONE, 3);

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new ExtendedBlock(STONE, 4));
        assertNotEquals(a, new ExtendedBlock(DIRT, 3));
        // Two mods (or two variants) can share an unlocalized name. Equality must
        // use the block instance, not the name, or tank frames mix materials.
        assertNotEquals(a, new ExtendedBlock(SAME_NAME, 3));
        assertNotEquals(a, null);
        assertNotEquals(a, "not a block");
    }

    @Test
    void metadataCanBeIgnored() {
        ExtendedBlock a = new ExtendedBlock(STONE, 0);
        ExtendedBlock b = new ExtendedBlock(STONE, 15);

        assertTrue(a.equalsIgnoreMetadata(b));
        assertTrue(b.equalsIgnoreMetadata(a));
        assertFalse(a.equalsIgnoreMetadata(new ExtendedBlock(DIRT, 0)));
        assertFalse(a.equalsIgnoreMetadata(null));
    }

    @Test
    void missingBlockIsHandledGracefully() {
        ExtendedBlock missing = new ExtendedBlock(null, 0);

        assertFalse(missing.hasBlock());
        assertFalse(missing.equals(new ExtendedBlock(null, 0)));
        assertFalse(missing.equalsIgnoreMetadata(new ExtendedBlock(STONE, 0)));
        assertTrue(
            missing.toString()
                .startsWith("null:"));
        missing.hashCode();
    }

    @Test
    void toStringKeepsMetadata() {
        assertTrue(
            new ExtendedBlock(STONE, 7).toString()
                .endsWith(":7"));
    }
}
