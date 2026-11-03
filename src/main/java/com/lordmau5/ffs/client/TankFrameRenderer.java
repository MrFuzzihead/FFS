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

    static boolean shouldRenderInPass(Block renderBlock, boolean opaque, int pass) {
        if (pass == 0) {
            return opaque || renderBlock.canRenderInPass(0);
        }
        if (pass == 1) {
            return !opaque || renderBlock.canRenderInPass(1);
        }
        return false;
    }

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        return this.renderWorldBlock(world, x, y, z, block, renderer, getPassForFrameRender());
    }

    public boolean renderWorldBlock(IBlockAccess ba, int x, int y, int z, Block block, RenderBlocks rb, int pass) {
        TileEntity tile = ba.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityTankFrame)) {
            // The client may briefly have the frame block before its S35
            // description packet creates/populates the TE. Draw a solid fallback
            // in the solid pass rather than making the whole tank shell vanish.
            return pass == 0 && rb.renderStandardBlock(Blocks.stone, x, y, z);
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
            boolean rendered = false;
            if (shouldRenderInPass(renderBlock, isFrameBlockOpaque, pass)) {
                if (invalidRender) {
                    rendered = rb.renderStandardBlock(renderBlock, x, y, z);
                } else {
                    // Reuse the stateless wrapper: allocating one per frame block per
                    // render pass causes tens of thousands of short-lived objects.
                    if (this.wrapper == null || this.wrapperBase != origBa) {
                        this.wrapperBase = origBa;
                        this.wrapper = new FrameBlockAccessWrapper(origBa);
                    }
                    rb.blockAccess = this.wrapper;

                    try {
                        rendered = rb.renderBlockByRenderType(renderBlock, x, y, z);
                    } catch (Exception e) {
                        rb.blockAccess = origBa;
                        if (this.failedBlocks.add(renderBlock)) {
                            FMLLog.log(Level.WARN, e, "FFS: failed to render tank frame block %s", renderBlock);
                        }
                        rendered = rb.renderStandardBlock(Blocks.stone, x, y, z);
                    }
                }

                rb.blockAccess = origBa;
            }

            // Returning true without emitting vertices makes WorldRenderer try to
            // sort an empty buffer on pass 1 (IllegalArgumentException from
            // PriorityQueue). Report exactly what was drawn.
            return rendered;
        }
    }

    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    public int getRenderId() {
        return id;
    }
}
