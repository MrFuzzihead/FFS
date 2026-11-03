package com.lordmau5.ffs.test;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Minimal unregistered block for unit tests. The production {@link Block}
 * constructor is protected, so tests cannot instantiate it directly.
 */
public class TestBlock extends Block {

    public TestBlock(String unlocalizedName) {
        super(Material.rock);
        this.setBlockName(unlocalizedName);
    }
}
