package com.lordmau5.ffs.block.valves;

import com.lordmau5.ffs.block.abstracts.AbstractBlock;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankValve;
import com.lordmau5.ffs.blockentity.util.TankConfig;
import com.lordmau5.ffs.blockentity.valves.BlockEntityFluidValve;
import com.lordmau5.ffs.holder.FFSBlockEntities;
import com.lordmau5.ffs.holder.FFSDataComponentType;
import com.lordmau5.ffs.util.FFSStateProps;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockFluidValve extends AbstractBlock {

    public BlockFluidValve() {
        super(Properties.of().requiresCorrectToolForDrops().strength(5.0f, 6.0f));

        registerDefaultState(getStateDefinition().any().setValue(FFSStateProps.TILE_VALID, false).setValue(FFSStateProps.TILE_MAIN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FFSStateProps.TILE_MAIN, FFSStateProps.TILE_VALID);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return FFSBlockEntities.tileEntityFluidValve.get().create(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == FFSBlockEntities.tileEntityFluidValve.get() ? BlockEntityFluidValve::tick : null;
    }

    private void addTankConfigToStack(ItemStack stack, AbstractTankValve valve, HolderLookup.Provider registries) {
        TankConfig tankConfig = valve.getTankConfig();

        if (tankConfig.isEmpty()) return;

        CompoundTag tag = stack.has(FFSDataComponentType.TANK_CONFIG) ? stack.get(FFSDataComponentType.TANK_CONFIG) : new CompoundTag();

        tankConfig.writeToNBT(tag, registries);

        stack.set(FFSDataComponentType.TANK_CONFIG, tag);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof BlockEntityFluidValve valve) {
            if (!level.isClientSide() && player.isCreative() && !valve.getTankConfig().isEmpty()) {
                ItemStack stack = new ItemStack(this);

                addTankConfigToStack(stack, valve, level.registryAccess());

                ItemEntity itementity = new ItemEntity(level, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, stack);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams)
    {
        List<ItemStack> drops = new ArrayList<>();

        BlockEntity tile = pParams.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tile instanceof BlockEntityFluidValve valve) {
            ItemStack stack = new ItemStack(this);

            addTankConfigToStack(stack, valve, pParams.getLevel().registryAccess());

            drops.add(stack);
        }

        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);

        if (player.isShiftKeyDown()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof BlockEntityFluidValve valve) {
                addTankConfigToStack(stack, valve, level.registryAccess());
            }
        }

        return stack;
    }

    private @Nonnull FluidStack loadFluidStackFromTankConfig(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.has(FFSDataComponentType.TANK_CONFIG)) {
            return FluidStack.EMPTY;
        }

        CompoundTag tag = stack.get(FFSDataComponentType.TANK_CONFIG);
        if (tag == null || !tag.contains("Fluid")) {
            return FluidStack.EMPTY;
        }

        return FluidStack.parse(registries, tag.getCompound("Fluid")).orElse(FluidStack.EMPTY);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);

        FluidStack fluidStack = loadFluidStackFromTankConfig(stack, worldIn.registryAccess());

        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof BlockEntityFluidValve valve) {
            valve.getTankConfig().setFluidStack(fluidStack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        FluidStack fluidStack = loadFluidStackFromTankConfig(stack, context.registries());

        if (fluidStack.isEmpty()) return;

        tooltipComponents.add(
                Component.translatable("description.ffs.fluid_valve.fluid", fluidStack.getHoverName().getString())
                        .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.translatable("description.ffs.fluid_valve.amount", GenericUtil.intToFancyNumber(fluidStack.getAmount()) + "mB")
                        .withStyle(ChatFormatting.GRAY)
        );
    }
}
