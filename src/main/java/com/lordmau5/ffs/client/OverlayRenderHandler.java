package com.lordmau5.ffs.client;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankEntity;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankValve;
import com.lordmau5.ffs.util.TankManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashSet;

public class OverlayRenderHandler {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation OVERLAY_TEXTURE_RESLOC = ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "block/overlay/tank_overlay_anim");
    private static final int MAX_TICKS = 20 * 5;
    private static BlockPos lastPos;
    private static float ticksRemaining;
    private static TextureAtlasSprite OVERLAY_TEXTURE;

    private static void updateLastPos(float deltaTick) {
        if (ticksRemaining > 0) {
            ticksRemaining -= deltaTick;
        }

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (lastPos != null && !pos.equals(lastPos)) {
            ticksRemaining = MAX_TICKS;
        }
        lastPos = pos;
    }

    private static TextureAtlasSprite getOverlayTexture() {
        if (OVERLAY_TEXTURE == null) {
            OVERLAY_TEXTURE = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(OVERLAY_TEXTURE_RESLOC);
        }

        return OVERLAY_TEXTURE;
    }

    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Level level = mc.level;
        if (level == null) return;

        updateLastPos(event.getPartialTick().getGameTimeDeltaTicks());
        if (lastPos == null || ticksRemaining <= 0) return;

        AbstractTankValve valve = null;

        BlockEntity tile = level.getBlockEntity(lastPos);
        if (tile instanceof AbstractTankEntity) {
            valve = ((AbstractTankEntity) tile).getMainValve();
        } else {
            if (TankManager.INSTANCE.isPartOfTank(level, lastPos)) {
                valve = TankManager.INSTANCE.getValveForBlock(level, lastPos);
            }
        }

        if (valve == null || !valve.isValid()) return;

        HashSet<BlockPos> tankBlocks = TankManager.INSTANCE.getAllFrameBlocksForValve(valve);
        tankBlocks.add(valve.getBlockPos());

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        TextureAtlasSprite sprite = getOverlayTexture();
        if (sprite == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (BlockPos targetPos : tankBlocks) {
            for (Direction face : Direction.values()) {
                if (!level.isEmptyBlock(targetPos.relative(face))) continue;

                renderBlockOverlay(level, targetPos, poseStack, bufferSource, sprite);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static void renderBlockOverlay(Level level, BlockPos pos, PoseStack poseStack,
                                           MultiBufferSource bufferSource, TextureAtlasSprite sprite) {
        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer builder = bufferSource.getBuffer(RenderType.translucent());

        BlockState state = level.getBlockState(pos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            if (!Block.shouldRenderFace(state, level, pos, dir, neighborPos)) continue;

            renderFace(matrix, builder, dir, sprite);
        }

        poseStack.popPose();
    }

    private static void renderFace(Matrix4f matrix, VertexConsumer builder, Direction face,
                                   TextureAtlasSprite sprite) {
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();

        float offset = 0.002f; // Z-fighting offset
        float dx = face.getStepX() * offset;
        float dy = face.getStepY() * offset;
        float dz = face.getStepZ() * offset;

        switch (face) {
            case UP -> {
                // Top face (y=1) — counter-clockwise when looking down from above
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 1 + dz).setUv(uMin, vMin), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 1 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 0 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 0 + dz).setUv(uMin, vMax), face);
            }
            case DOWN -> {
                // Bottom face (y=0) — counter-clockwise when looking up from below
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 0 + dz).setUv(uMin, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 0 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 1 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 1 + dz).setUv(uMin, vMin), face);
            }
            case NORTH -> {
                // North face (z=0)
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 0 + dz).setUv(uMin, vMax), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 0 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 0 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 0 + dz).setUv(uMin, vMin), face);
            }
            case SOUTH -> {
                // South face (z=1)
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 1 + dz).setUv(uMin, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 1 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 1 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 1 + dz).setUv(uMin, vMin), face);
            }
            case WEST -> {
                // West face (x=0)
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 0 + dz).setUv(uMin, vMax), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 0 + dy, 1 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 1 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 0 + dx, 1 + dy, 0 + dz).setUv(uMin, vMin), face);
            }
            case EAST -> {
                // East face (x=1)
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 1 + dz).setUv(uMin, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 0 + dy, 0 + dz).setUv(uMax, vMax), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 0 + dz).setUv(uMax, vMin), face);
                applyToVertex(builder.addVertex(matrix, 1 + dx, 1 + dy, 1 + dz).setUv(uMin, vMin), face);
            }
        }
    }

    private static void applyToVertex(VertexConsumer consumer, Direction face) {
        int light = LightTexture.FULL_BRIGHT;
        float alpha = ticksRemaining / MAX_TICKS * 0.5f;

        int normalX = face.getStepX();
        int normalY = face.getStepY();
        int normalZ = face.getStepZ();

        consumer.setColor(0.0f, 0.8f, 1f, alpha).setLight(light).setNormal(normalX, normalY, normalZ);
    }

}
