package com.lordmau5.ffs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBlock;
import com.lordmau5.ffs.test.TestBootstrap;

/**
 * Harness smoke test: proves that the test JVM is headless, the patched
 * Minecraft/Forge classes are on the test classpath, and the registry-free
 * paths used by the unit suite are usable.
 */
class HeadlessHarnessTest {

    @BeforeAll
    static void bootstrapFmlMetadata() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void runsWithoutADisplay() {
        assertTrue(GraphicsEnvironment.isHeadless());
    }

    @Test
    void forgeDirectionsHaveTheExpectedLayout() {
        assertEquals(1, ForgeDirection.UP.offsetY);
        assertEquals(-1, ForgeDirection.DOWN.offsetY);
        assertEquals(0, ForgeDirection.UNKNOWN.offsetX);
    }

    @Test
    void nbtAndUnregisteredBlocksAreUsable() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("key", "value");
        assertEquals("value", tag.getString("key"));
        assertTrue(tag.hasKey("key"));
        assertFalse(tag.hasNoTags());

        Block block = new TestBlock("ffs_harness_block");
        assertNotNull(block);
        assertEquals("tile.ffs_harness_block", block.getUnlocalizedName());
    }
}
