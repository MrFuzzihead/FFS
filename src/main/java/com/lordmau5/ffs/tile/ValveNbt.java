package com.lordmau5.ffs.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.util.NbtHelper;

/** NBT save/load and compact client-sync codec for a valve. */
public final class ValveNbt {

    private ValveNbt() {}

    public static ValveState read(NBTTagCompound tag, String fallbackName) {
        ValveState state = new ValveState();
        state.isValid = tag.getBoolean("isValid");
        state.inside = ForgeDirection.getOrientation(tag.getInteger("inside"));
        state.isMaster = tag.getBoolean("master");
        state.valveHeightPosition = Math.max(0, tag.getInteger("valveHeightPosition"));

        if (state.isMaster) {
            state.tankHeight = Math.max(0, tag.getInteger("tankHeight"));
            state.fluidCapacity = Math.max(0, tag.getInteger("fluidCapacity"));

            FluidStack loaded = NbtHelper.readFluidStack(tag, NbtHelper.FLUID_TAG, state.fluidCapacity);
            if (loaded == null && (tag.hasKey("fluidName") || tag.hasKey("fluidID"))) {
                loaded = NbtHelper.readLegacyFluid(tag, "fluidName", "fluidID", "fluidAmount", state.fluidCapacity);
            }
            state.fluidStack = loaded;
        } else {
            state.masterValvePos = NbtHelper.readPosition(tag, "masterValve");
        }

        state.autoOutput = tag.getBoolean("autoOutput");
        state.valveName = tag.hasKey("valveName") ? tag.getString("valveName") : fallbackName;
        state.bottomDiagFrame = NbtHelper.readPosition(tag, "bottomDiagF");
        state.topDiagFrame = NbtHelper.readPosition(tag, "topDiagF");
        if (state.bottomDiagFrame == null || state.topDiagFrame == null) {
            state.bottomDiagFrame = null;
            state.topDiagFrame = null;
        }

        return state;
    }

    public static void write(NBTTagCompound tag, ValveState state) {
        tag.setBoolean("isValid", state.isValid);
        tag.setInteger("inside", state.inside == null ? ForgeDirection.UNKNOWN.ordinal() : state.inside.ordinal());
        tag.setBoolean("master", state.isMaster);
        tag.setInteger("valveHeightPosition", state.valveHeightPosition);

        if (state.isMaster) {
            String fluidName = state.fluidStack == null ? null : FluidRegistry.getFluidName(state.fluidStack);
            tag.setBoolean("hasFluid", state.fluidStack != null);
            if (fluidName != null) {
                tag.setString("fluidName", fluidName);
                tag.setInteger("fluidAmount", state.fluidStack.amount);
            }
            NbtHelper.writeFluidStack(tag, NbtHelper.FLUID_TAG, state.fluidStack);

            tag.setInteger("tankHeight", state.tankHeight);
            tag.setInteger("fluidCapacity", state.fluidCapacity);
        } else {
            NbtHelper.writePosition(tag, "masterValve", state.masterValvePos);
        }

        tag.setBoolean("autoOutput", state.autoOutput);
        if (state.valveName != null && !state.valveName.isEmpty()) {
            tag.setString("valveName", state.valveName);
        }

        NbtHelper.writePosition(tag, "bottomDiagF", state.bottomDiagFrame);
        NbtHelper.writePosition(tag, "topDiagF", state.topDiagFrame);
    }

    public static void writeFrom(TileEntityValve valve, NBTTagCompound tag) {
        write(tag, ValveState.capture(valve));
    }

    public static void apply(TileEntityValve valve, NBTTagCompound tag, String fallbackName) {
        ValveState state = read(tag, fallbackName);

        valve.isValid = state.isValid;
        valve.setInside(state.inside);
        valve.setMaster(state.isMaster);
        valve.valveHeightPosition = state.valveHeightPosition;
        valve.setAutoOutputFlag(state.autoOutput);
        valve.setValveName(state.valveName);
        valve.bottomDiagFrame = state.bottomDiagFrame;
        valve.topDiagFrame = state.topDiagFrame;

        if (state.isMaster) {
            valve.tankHeight = state.tankHeight;
            valve.setFluidCapacityInternal(state.fluidCapacity);
            valve.setFluidStackInternal(state.fluidStack);
            valve.updateFluidTemperature();
        } else {
            valve.setMasterPos(state.masterValvePos);
        }
    }
}
