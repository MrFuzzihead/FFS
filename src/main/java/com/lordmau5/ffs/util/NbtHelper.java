package com.lordmau5.ffs.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * NBT helpers that never throw on missing, legacy or corrupt data. This is the
 * single place where tank saves are decoded, so the unit tests can cover every
 * broken-save case without a running world.
 */
public final class NbtHelper {

    /** NBT key of the modern (tag-preserving) FluidStack compound. */
    public static final String FLUID_TAG = "fluid";

    private NbtHelper() {}

    public static void writeFluidStack(NBTTagCompound tag, String key, FluidStack stack) {
        if (tag == null) {
            return;
        }

        if (stack == null || stack.getFluid() == null
            || stack.amount <= 0
            || FluidRegistry.getFluidName(stack.getFluid()) == null) {
            tag.removeTag(key);
            return;
        }

        NBTTagCompound fluidTag = new NBTTagCompound();
        stack.writeToNBT(fluidTag);
        tag.setTag(key, fluidTag);
    }

    public static FluidStack readFluidStack(NBTTagCompound tag, String key, int capacity) {
        if (tag == null || !tag.hasKey(key)) {
            return null;
        }

        NBTTagCompound fluidTag = tag.getCompoundTag(key);
        return clamp(FluidStack.loadFluidStackFromNBT(fluidTag), capacity);
    }

    /**
     * Reads the pre-1.4 save format ({@code fluidName}/{@code fluidID} plus
     * {@code fluidAmount}). Missing or unregistered fluids return {@code null}
     * instead of throwing from the {@link FluidStack} constructor.
     */
    public static FluidStack readLegacyFluid(NBTTagCompound tag, String nameKey, String idKey, String amountKey,
        int capacity) {
        if (tag == null || !tag.hasKey(amountKey)) {
            return null;
        }

        Fluid fluid = null;
        String name = legacyFluidName(tag, nameKey);
        if (name != null) {
            fluid = FluidRegistry.getFluid(name);
        }
        if (fluid == null) {
            Integer id = legacyFluidId(tag, idKey);
            if (id != null) {
                fluid = FluidRegistry.getFluid(id);
            }
        }
        if (fluid == null) {
            return null;
        }

        int amount = clampAmount(tag.getInteger(amountKey), capacity);
        return amount <= 0 ? null : new FluidStack(fluid, amount);
    }

    /** Pure amount rule used by both save formats. */
    public static int clampAmount(int amount, int capacity) {
        if (amount <= 0) {
            return 0;
        }

        if (capacity > 0 && amount > capacity) {
            return capacity;
        }

        return amount;
    }

    /** Pure legacy-name selection (null when absent or empty). */
    public static String legacyFluidName(NBTTagCompound tag, String nameKey) {
        if (tag == null || !tag.hasKey(nameKey)) {
            return null;
        }

        String name = tag.getString(nameKey);
        return name == null || name.isEmpty() ? null : name;
    }

    /** Pure legacy-ID selection (null when absent). */
    public static Integer legacyFluidId(NBTTagCompound tag, String idKey) {
        return tag != null && tag.hasKey(idKey) ? tag.getInteger(idKey) : null;
    }

    public static Position3D readPosition(NBTTagCompound tag, String key) {
        if (tag == null || !tag.hasKey(key)) {
            return null;
        }

        int[] pos = tag.getIntArray(key);
        if (pos == null || pos.length < 3) {
            return null;
        }

        return new Position3D(pos[0], pos[1], pos[2]);
    }

    public static void writePosition(NBTTagCompound tag, String key, Position3D pos) {
        if (tag == null) {
            return;
        }

        if (pos == null) {
            tag.removeTag(key);
            return;
        }

        tag.setIntArray(key, new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    private static FluidStack clamp(FluidStack stack, int capacity) {
        if (stack == null || stack.getFluid() == null) {
            return null;
        }

        int amount = clampAmount(stack.amount, capacity);
        if (amount <= 0) {
            return null;
        }

        stack.amount = amount;
        return stack;
    }
}
