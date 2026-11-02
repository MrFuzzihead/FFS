package com.lordmau5.ffs.client;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

import org.apache.logging.log4j.Level;

import com.lordmau5.ffs.tile.TileEntityTankFrame;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLLog;

public class TankFrameRenderer implements ISimpleBlockRenderingHandler {

    public static final int id = RenderingRegistry.getNextAvailableRenderId();

    private final Set<Block> failedBlocks = new HashSet<>();
    private FrameBlockAccessWrapper wrapper;
    private IBlockAccess wrapperBase;

    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {}

    public static int getPassForFrameRender() {
        return MinecraftForgeClient.getRenderPass();
    }

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        return this.renderWorldBlock(world, x, y, z, block, renderer, getPassForFrameRender());
    }

    public boolean renderWorldBlock(IBlockAccess ba, int x, int y, int z, Block block, RenderBlocks rb, int pass) {
        TileEntity tile = ba.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityTankFrame)) {
            return false;
        } else {
            boolean invalidRender = true;
            Block renderBlock = Blocks.stone;
            TileEntityTankFrame te = (TileEntityTankFrame) tile;
            Block exBlock = te.getStoredBlock();
            if (exBlock != null) {
                renderBlock = exBlock;
                invalidRender = false;
            }

            IBlockAccess origBa = rb.blockAccess;
            boolean isFrameBlockOpaque = renderBlock.isOpaqueCube();
            if ((isFrameBlockOpaque || renderBlock.canRenderInPass(0)) && pass == 0
                || (!isFrameBlockOpaque || renderBlock.canRenderInPass(1)) && pass == 1) {
                if (invalidRender) {
                    rb.renderStandardBlock(renderBlock, x, y, z);
                } else {
                    // Reuse the stateless wrapper: allocating one per frame block per
                    // render pass causes tens of thousands of short-lived objects.
                    if (this.wrapper == null || this.wrapperBase != origBa) {
                        this.wrapperBase = origBa;
                        this.wrapper = new FrameBlockAccessWrapper(origBa);
                    }
                    rb.blockAccess = this.wrapper;

                    try {
                        rb.renderBlockByRenderType(renderBlock, x, y, z);
                    } catch (Exception e) {
                        rb.blockAccess = origBa;
                        if (this.failedBlocks.add(renderBlock)) {
                            FMLLog.log(Level.WARN, e, "FFS: failed to render tank frame block %s", renderBlock);
                        }
                        rb.renderStandardBlock(Blocks.stone, x, y, z);
                    }
                }

                rb.blockAccess = origBa;
            }

            return true;
        }
    }

    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    public int getRenderId() {
        return id;
    }
}
