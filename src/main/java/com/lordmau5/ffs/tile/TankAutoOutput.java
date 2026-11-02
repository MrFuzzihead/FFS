package com.lordmau5.ffs.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.lordmau5.ffs.util.TankMath;

/** Auto-output neighbour push, extracted from the block entity. */
public final class TankAutoOutput {

    private static final int MAX_TO_VALVE = 1000;
    private static final int MAX_TO_HANDLER = 50;

    private TankAutoOutput() {}

    public static void tick(TileEntityValve valve) {
        if (valve.getWorldObj() == null) {
            return;
        }

        FluidStack fluid = valve.getFluid();
        if (fluid == null || valve.getFluidAmount() == 0) {
            return;
        }

        if (!(valve.getAutoOutput() || valve.valveHeightPosition == 0)) {
            return;
        }

        int capacity = valve.getCapacity();
        if (capacity <= 0) {
            return;
        }

        boolean negativeDensity = fluid.getFluid() != null && fluid.getFluid()
            .getDensity(fluid) < 0;
        double fill = (double) valve.getFluidAmount() / (double) capacity;
        if (!TankMath.canAutoOutput(fill, valve.getTankHeight(), valve.valveHeightPosition, negativeDensity)) {
            return;
        }

        ForgeDirection outside = valve.getInside()
            .getOpposite();
        TileEntity tile = valve.getWorldObj()
            .getTileEntity(
                valve.xCoord + outside.offsetX,
                valve.yCoord + outside.offsetY,
                valve.zCoord + outside.offsetZ);
        if (tile == null
            || !(tile instanceof TileEntityValve || valve.getAutoOutput() || valve.valveHeightPosition != 0)) {
            return;
        }

        int maxAmount = tile instanceof TileEntityValve ? MAX_TO_VALVE
            : tile instanceof IFluidHandler ? MAX_TO_HANDLER : 0;
        if (maxAmount == 0) {
            return;
        }

        FluidStack fillStack = fluid.copy();
        fillStack.amount = Math.min(valve.getFluidAmount(), maxAmount);
        int accepted = ((IFluidHandler) tile).fill(valve.getInside(), fillStack, true);
        if (accepted > 0) {
            valve.drain(accepted, true);
        }
    }
}
