package com.lordmau5.ffs.util;

import net.minecraft.block.Block;

public class ExtendedBlock {

    private final Block block;
    private final int metadata;

    public ExtendedBlock(Block block, int metadata) {
        this.block = block;
        this.metadata = metadata;
    }

    public Block getBlock() {
        return this.block;
    }

    public int getMetadata() {
        return this.metadata;
    }

    public boolean hasBlock() {
        return this.block != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExtendedBlock)) {
            return false;
        }

        ExtendedBlock other = (ExtendedBlock) obj;
        if (this.block == null || other.block == null) {
            return false;
        }

        return this.block == other.block && other.metadata == this.metadata;
    }

    public boolean equalsIgnoreMetadata(ExtendedBlock block) {
        return this.block != null && block != null && block.block != null && block.block == this.block;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(this.block) + this.metadata;
    }

    @Override
    public String toString() {
        return (this.block == null ? "null" : this.block.getUnlocalizedName()) + ":" + this.metadata;
    }
}
