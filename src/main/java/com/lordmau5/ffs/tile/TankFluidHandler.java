package com.lordmau5.ffs.tile;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.util.TankMath;

/** Fluid-handling rules, extracted from the block entity for readability/testing. */
public final class TankFluidHandler {

    private TankFluidHandler() {}

    public static int fill(TileEntityValve valve, FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }

        TileEntityValve master = valve.getMaster();
        if (master == null) {
            return 0;
        }
        if (master != valve) {
            return master.fill(resource, doFill);
        }

        FluidStack current = valve.getFluidStackInternal();
        int capacity = valve.getFluidCapacityInternal();
        if (!valve.isValid() || (current != null && !current.isFluidEqual(resource))) {
            return 0;
        }

        if (!doFill) {
            return current == null ? Math.min(capacity, resource.amount)
                : Math.min(capacity - current.amount, resource.amount);
        }

        if (current == null) {
            FluidStack created = new FluidStack(resource, Math.min(capacity, resource.amount));
            valve.setFluidStackInternal(created);
            valve.updateFluidTemperature();
            valve.setNeedsUpdate();
            return created.amount;
        }

        int filled = capacity - current.amount;
        if (resource.amount < filled) {
            current.amount = current.amount + resource.amount;
            filled = resource.amount;
        } else {
            current.amount = capacity;
        }

        master.setNeedsUpdate();
        return filled;
    }

    public static FluidStack drain(TileEntityValve valve, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }

        TileEntityValve master = valve.getMaster();
        if (master == null) {
            return null;
        }
        if (master != valve) {
            return master.drain(maxDrain, doDrain);
        }

        FluidStack current = valve.getFluidStackInternal();
        if (!valve.isValid() || current == null) {
            return null;
        }

        int drained = Math.min(maxDrain, current.amount);
        FluidStack stack = new FluidStack(current, drained);
        if (doDrain) {
            current.amount -= drained;
            if (current.amount <= 0) {
                valve.setFluidStackInternal(null);
                valve.updateFluidTemperature();
            }
            master.setNeedsUpdate();
        }

        return stack;
    }

    public static boolean canFillIncludingContainers(TileEntityValve valve, ForgeDirection from, Fluid fluid) {
        if (fluid == null || !valve.isValid()) {
            return false;
        }

        FluidStack current = valve.getFluid();
        if (current != null && current.getFluid() != fluid) {
            return false;
        }

        return valve.getFluidAmount() < valve.getCapacity();
    }

    public static boolean canFill(TileEntityValve valve, ForgeDirection from, Fluid fluid) {
        if (!canFillIncludingContainers(valve, from, fluid)) {
            return false;
        }

        FluidStack stack = valve.getFluid();
        boolean negativeDensity = stack != null && stack.getFluid() != null
            && stack.getFluid()
                .getDensity(stack) < 0;

        return TankMath.canFillFromHeight(
            valve.valveHeightPosition,
            valve.getTankHeight(),
            TankMath.fillFraction(valve.getFluidAmount(), valve.getCapacity()),
            valve.getAutoOutput(),
            negativeDensity);
    }

    public static boolean canDrain(TileEntityValve valve, ForgeDirection from, Fluid fluid) {
        if (fluid == null || !valve.isValid()) {
            return false;
        }

        FluidStack current = valve.getFluid();
        return current != null && current.getFluid() == fluid && valve.getFluidAmount() > 0;
    }
}
