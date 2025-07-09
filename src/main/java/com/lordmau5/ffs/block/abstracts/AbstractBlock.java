package com.lordmau5.ffs.block.abstracts;

import com.lordmau5.ffs.blockentity.abstracts.AbstractTankEntity;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankValve;
import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public abstract class AbstractBlock extends Block implements EntityBlock {

    protected AbstractBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> eb) {
        return null;
    }

    @Override
    @Nullable
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof AbstractTankEntity tankEntity && tankEntity.isValid()) {
            tankEntity.getMainValve().breakTank();
        }

        super.wasExploded(level, pos, explosion);
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!worldIn.isClientSide() && newState.isAir()) {
            BlockEntity tile = worldIn.getBlockEntity(pos);
            if (tile instanceof AbstractTankEntity tankEntity && tankEntity.isValid()) {
                tankEntity.getMainValve().breakTank();
            }
        }

        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof AbstractTankEntity tankEntity && tankEntity.isValid()) {
                tankEntity.getMainValve().breakTank();
            }
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) return ItemInteractionResult.FAIL;

        AbstractTankEntity tankEntity = (AbstractTankEntity) level.getBlockEntity(pos);
        if (tankEntity == null) {
            return ItemInteractionResult.FAIL;
        }

        if (tankEntity.isValid()) {
            if (GenericUtil.isFluidContainer(player.getMainHandItem())) {
                if (GenericUtil.fluidContainerHandler(level, tankEntity.getMainValve(), player)) {
                    tankEntity.getMainValve().markForUpdateNow();
                    return ItemInteractionResult.CONSUME;
                }
            }

            NetworkHandler.sendPacketToPlayer(new FFSPacket.Client.OpenGUI(tankEntity, false), (ServerPlayer) player);
        } else if (tankEntity instanceof AbstractTankValve valve) {
            valve.buildTank(player, hitResult.getDirection().getOpposite());
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof AbstractTankValve valve) {
            return valve.getComparatorOutput();
        }
        return 0;
    }
}
