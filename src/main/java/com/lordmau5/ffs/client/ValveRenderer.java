package com.lordmau5.ffs.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.Position3D;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ValveRenderer extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler {

    public static final int id = RenderingRegistry.getNextAvailableRenderId();

    private void preGL() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(TextureMap.locationBlocksTexture);
    }

    private void postGL() {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float f) {
        TileEntityValve valve = (TileEntityValve) tile;
        if (valve != null && valve.isValid()) {
            Tessellator t = Tessellator.instance;
            if (valve.isMaster()) {
                Position3D bottomDiag = valve.bottomDiagFrame;
                Position3D topDiag = valve.topDiagFrame;
                if (bottomDiag == null || topDiag == null) {
                    return;
                }

                int height = topDiag.getY() - bottomDiag.getY();
                int xSize = topDiag.getX() - bottomDiag.getX()
                    + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);
                int zSize = topDiag.getZ() - bottomDiag.getZ()
                    + (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 0 : 1);
                if (valve.getCapacity() == 0) {
                    return;
                }

                int capacity = valve.getCapacity();
                FluidStack fluid = valve.getFluid();
                int fluidAmount = fluid == null ? 0 : fluid.amount;
                if (fluidAmount == 0) {
                    return;
                }

                double fillPercentage = (double) fluidAmount / (double) capacity;
                boolean isNegativeDensity = fluid.getFluid()
                    .getDensity(fluid) < 0;
                float pureRenderHeight = FluidRenderMath.pureHeight(height, fillPercentage);
                if (fillPercentage <= 0.0D || pureRenderHeight <= 0.0F) {
                    return;
                }

                if (fillPercentage > 0.0D) {
                    this.preGL();
                    if (isNegativeDensity) {
                        GL11.glColor4d(1.0, 1.0, 1.0, 0.125 + fillPercentage - 0.125 * fillPercentage);
                    }
                    IIcon flowing = FluidHelper.getFluidTexture(fluid.getFluid(), true);
                    IIcon still = FluidHelper.getFluidTexture(fluid.getFluid(), false);
                    float stillMinU = still.getMinU();
                    float stillMaxU = still.getMaxU();
                    float stillMinV = still.getMinV();
                    float stillMaxV = still.getMaxV();
                    float flowMinU = flowing.getMinU();
                    float flowMaxU = flowMinU + (stillMaxU - stillMinU);
                    float flowMinV_ = flowing.getMinV();
                    float flowMaxV_ = flowMinV_ + (stillMaxV - stillMinV);
                    GL11.glTranslatef((float) x, (float) y, (float) z);
                    GL11.glTranslatef(
                        bottomDiag.getX() - tile.xCoord,
                        (float) bottomDiag.getY() - tile.yCoord + 1.0F,
                        bottomDiag.getZ() - tile.zCoord);
                    t.startDrawingQuads();
                    int visibleLayers = FluidRenderMath.visibleLayers(pureRenderHeight);

                    for (int rY = 0; rY < visibleLayers; rY++) {
                        float renderHeight = FluidRenderMath.layerHeight(pureRenderHeight, rY);

                        float flowMinV = flowMinV_;
                        if (renderHeight - rY < 1.0F) {
                            flowMinV = flowMinV_ + (flowMaxV_ - flowMinV_) * (1.0F - (renderHeight - rY));
                        }

                        for (int rX = FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0; rX < xSize; rX++) {
                            for (int rZ = FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0; rZ < zSize; rZ++) {
                                float zMinOffset = 0.0F;
                                float zMaxOffset = 0.0F;
                                float xMinOffset = 0.0F;
                                float xMaxOffset = 0.0F;
                                if (rZ == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                                    zMinOffset = 0.005F;
                                    t.addVertexWithUV(rX, rY, rZ + zMinOffset, flowMaxU, flowMaxV_);
                                    t.addVertexWithUV(rX, renderHeight, rZ + zMinOffset, flowMaxU, flowMinV);
                                    t.addVertexWithUV(rX + 1, renderHeight, rZ + zMinOffset, flowMinU, flowMinV);
                                    t.addVertexWithUV(rX + 1, rY, rZ + zMinOffset, flowMinU, flowMaxV_);
                                }

                                if (rZ == zSize - 1) {
                                    zMaxOffset = 0.005F;
                                    t.addVertexWithUV(rX, rY, rZ + 1 - zMaxOffset, flowMaxU, flowMaxV_);
                                    t.addVertexWithUV(rX + 1, rY, rZ + 1 - zMaxOffset, flowMinU, flowMaxV_);
                                    t.addVertexWithUV(rX + 1, renderHeight, rZ + 1 - zMaxOffset, flowMinU, flowMinV);
                                    t.addVertexWithUV(rX, renderHeight, rZ + 1 - zMaxOffset, flowMaxU, flowMinV);
                                }

                                if (rX == (FancyFluidStorage.instance.TANK_RENDER_INSIDE ? 1 : 0)) {
                                    xMinOffset = 0.005F;
                                    t.addVertexWithUV(rX + xMinOffset, rY, rZ, flowMaxU, flowMaxV_);
                                    t.addVertexWithUV(rX + xMinOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                                    t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                    t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ, flowMaxU, flowMinV);
                                }

                                if (rX == xSize - 1) {
                                    xMaxOffset = 0.005F;
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, rY, rZ, flowMaxU, flowMaxV_);
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ, flowMaxU, flowMinV);
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ + 1, flowMinU, flowMinV);
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, rY, rZ + 1, flowMinU, flowMaxV_);
                                }

                                if (rY == Math.floor(pureRenderHeight) || rY + 1 == Math.ceil(pureRenderHeight)) {
                                    t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ, stillMinU, stillMinV);
                                    t.addVertexWithUV(rX + xMinOffset, renderHeight, rZ + 1, stillMinU, stillMaxV);
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ + 1, stillMaxU, stillMaxV);
                                    t.addVertexWithUV(rX + 1 - xMaxOffset, renderHeight, rZ, stillMaxU, stillMinV);
                                }

                                if (rY == 0) {
                                    t.addVertexWithUV(rX + 1, 0.01F, rZ + zMinOffset, stillMinU, stillMinV);
                                    t.addVertexWithUV(rX + 1, 0.01F, rZ + 1 - zMaxOffset, stillMinU, stillMaxV);
                                    t.addVertexWithUV(rX, 0.01F, rZ + 1 - zMaxOffset, stillMaxU, stillMaxV);
                                    t.addVertexWithUV(rX, 0.01F, rZ + zMinOffset, stillMaxU, stillMinV);
                                }
                            }
                        }
                    }

                    if (isNegativeDensity) {
                        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glTranslatef(bottomDiag.getX() - topDiag.getX() - 1, -height + 1, 0.0F);
                    }

                    t.draw();
                    this.postGL();
                }
            }
        }
    }

    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        for (int side = 0; side < 6; side++) {
            this.renderInventoryFace(renderer, block, side);
        }
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private void renderInventoryFace(RenderBlocks renderer, Block block, int side) {
        Tessellator tessellator = Tessellator.instance;
        IIcon icon = renderer.getIconSafe(FancyFluidStorage.proxy.tex_ValveItem);
        tessellator.startDrawingQuads();

        switch (side) {
            case 0:
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                renderer.renderFaceYNeg(block, 0.0, 0.0, 0.0, icon);
                break;
            case 1:
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                renderer.renderFaceYPos(block, 0.0, 0.0, 0.0, icon);
                break;
            case 2:
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                renderer.renderFaceZNeg(block, 0.0, 0.0, 0.0, icon);
                break;
            case 3:
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                renderer.renderFaceZPos(block, 0.0, 0.0, 0.0, icon);
                break;
            case 4:
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                renderer.renderFaceXNeg(block, 0.0, 0.0, 0.0, icon);
                break;
            default:
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                renderer.renderFaceXPos(block, 0.0, 0.0, 0.0, icon);
                break;
        }

        tessellator.draw();
    }

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (world == null) {
            return false;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityValve)) {
            return false;
        }

        TileEntityValve valve = (TileEntityValve) tile;
        // BlockValve.getIcon supplies the valve body (and the accent for the
        // inside face once formed). Unformed valves need the accent overlaid on
        // the solid body; otherwise the transparent tankValve/tankMaster texture
        // is all that is drawn and the block looks invisible.
        renderer.renderStandardBlock(block, x, y, z);
        if (!valve.isValid()) {
            renderer.setOverrideBlockTexture(
                valve.isMaster() ? FancyFluidStorage.proxy.tex_MasterValve[0]
                    : FancyFluidStorage.proxy.tex_SlaveValve[0]);
            renderer.renderStandardBlock(block, x, y, z);
            renderer.clearOverrideBlockTexture();
        }
        return true;
    }

    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    public int getRenderId() {
        return id;
    }
}
