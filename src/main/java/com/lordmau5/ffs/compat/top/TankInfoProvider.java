package com.lordmau5.ffs.compat.top;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.blockentity.abstracts.AbstractTankValve;
import com.lordmau5.ffs.blockentity.util.TankConfig;
import com.lordmau5.ffs.util.TankManager;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.ProbeConfig;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import mcjty.theoneprobe.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TankInfoProvider implements IProbeInfoProvider {
    @Override
    public ResourceLocation getID() {
        return ResourceLocation.fromNamespaceAndPath("tank_info_provider", FancyFluidStorage.MOD_ID);
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, Player playerEntity, Level world, BlockState blockState, IProbeHitData iProbeHitData) {
        AbstractTankValve valve = null;
        if (TankManager.INSTANCE.isPartOfTank(world, iProbeHitData.getPos())) {
            valve = TankManager.INSTANCE.getValveForBlock(world, iProbeHitData.getPos());
        }

        if (valve != null) {
            IProbeInfo vert = iProbeInfo.vertical();
            vert.text(
                    (Component.translatable("top.ffs.part_of_tank"))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
            addFluidInfo(vert, Config.getDefaultConfig(), valve.getTankConfig());
        }
    }

    private void addFluidInfo(IProbeInfo probeInfo, ProbeConfig config, TankConfig tankConfig) {
        FluidStack fluidStack = tankConfig.getFluidOrLockedFluid();

        int maxContents = tankConfig.getFluidCapacity();
        int contents = fluidStack.isEmpty() ? 0 : fluidStack.getAmount();
        if (tankConfig.isEmpty()) {
            contents = 0;
        }

        if (!fluidStack.is(FluidStack.EMPTY.getFluidType())) {
            probeInfo.text(Component.translatable("top.ffs.fluid", fluidStack.getHoverName()));
        }

        if (config.getTankMode() == 1) {
            probeInfo.progress(contents, maxContents, probeInfo.defaultProgressStyle().suffix("mB").filledColor(Config.tankbarFilledColor).alternateFilledColor(Config.tankbarAlternateFilledColor)
                    .borderColor(Config.tankbarBorderColor).numberFormat(Config.tankFormat.get()));
        } else {
            probeInfo.text(
                    (ElementProgress.format(contents, Config.tankFormat.get(), Component.literal("mB")))
            );
        }
    }
}
