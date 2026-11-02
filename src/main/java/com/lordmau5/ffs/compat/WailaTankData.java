package com.lordmau5.ffs.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.lordmau5.ffs.tile.TileEntityValve;

/**
 * Compact WAILA sync payload. Keeping this pure makes the server/client data
 * contract testable without a running game.
 */
public final class WailaTankData {

    private static final String HAS_DATA = "ffs.hasData";
    private static final String FRAME = "ffs.frame";
    private static final String VALID = "ffs.valid";
    private static final String FLUID_NAME = "ffs.fluidName";
    private static final String FLUID_AMOUNT = "ffs.fluidAmount";
    private static final String CAPACITY = "ffs.capacity";
    private static final String TEMPERATURE = "ffs.temperature";
    private static final String VALVE_NAME = "ffs.valveName";
    private static final String AUTO_OUTPUT = "ffs.autoOutput";
    private static final String MASTER = "ffs.master";
    private static final String TANK_HEIGHT = "ffs.tankHeight";
    private static final String VALVE_HEIGHT = "ffs.valveHeight";
    private static final String WIDTH = "ffs.width";
    private static final String HEIGHT = "ffs.height";
    private static final String DEPTH = "ffs.depth";

    public static final class Data {

        public final boolean valid;
        public final boolean frame;
        public final boolean autoOutput;
        public final boolean master;
        public final int fluidAmount;
        public final int capacity;
        public final int temperature;
        public final int tankHeight;
        public final int valveHeight;
        public final int width;
        public final int height;
        public final int depth;
        public final String fluidName;
        public final String valveName;

        private Data(boolean valid, boolean frame, boolean autoOutput, boolean master, int fluidAmount, int capacity,
            int temperature, int tankHeight, int valveHeight, int width, int height, int depth, String fluidName,
            String valveName) {
            this.valid = valid;
            this.frame = frame;
            this.autoOutput = autoOutput;
            this.master = master;
            this.fluidAmount = fluidAmount;
            this.capacity = capacity;
            this.temperature = temperature;
            this.tankHeight = tankHeight;
            this.valveHeight = valveHeight;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.fluidName = fluidName;
            this.valveName = valveName;
        }
    }

    private WailaTankData() {}

    public static void write(NBTTagCompound tag, TileEntityValve valve, boolean frame) {
        if (tag == null) {
            return;
        }

        Data data = fromValve(valve, frame);
        tag.setBoolean(FRAME, frame);
        if (data == null) {
            tag.setBoolean(HAS_DATA, false);
            return;
        }

        tag.setBoolean(HAS_DATA, true);
        tag.setBoolean(VALID, data.valid);
        tag.setBoolean(AUTO_OUTPUT, data.autoOutput);
        tag.setBoolean(MASTER, data.master);
        tag.setInteger(FLUID_AMOUNT, data.fluidAmount);
        tag.setInteger(CAPACITY, data.capacity);
        tag.setInteger(TEMPERATURE, data.temperature);
        tag.setInteger(TANK_HEIGHT, data.tankHeight);
        tag.setInteger(VALVE_HEIGHT, data.valveHeight);
        tag.setInteger(WIDTH, data.width);
        tag.setInteger(HEIGHT, data.height);
        tag.setInteger(DEPTH, data.depth);
        tag.setString(FLUID_NAME, data.fluidName);
        tag.setString(VALVE_NAME, data.valveName);
    }

    public static Data read(NBTTagCompound tag) {
        if (tag == null || !tag.getBoolean(HAS_DATA)) {
            return null;
        }

        return new Data(
            tag.getBoolean(VALID),
            tag.getBoolean(FRAME),
            tag.getBoolean(AUTO_OUTPUT),
            tag.getBoolean(MASTER),
            tag.getInteger(FLUID_AMOUNT),
            tag.getInteger(CAPACITY),
            tag.getInteger(TEMPERATURE),
            tag.getInteger(TANK_HEIGHT),
            tag.getInteger(VALVE_HEIGHT),
            tag.getInteger(WIDTH),
            tag.getInteger(HEIGHT),
            tag.getInteger(DEPTH),
            tag.getString(FLUID_NAME),
            tag.getString(VALVE_NAME));
    }

    public static Data fromValve(TileEntityValve valve, boolean frame) {
        if (valve == null) {
            return null;
        }

        FluidStack stack = valve.getFluid();
        Fluid fluid = stack == null ? null : stack.getFluid();
        String fluidName = fluid == null ? "" : fluid.getName();
        int temperature = fluid == null ? 0 : fluid.getTemperature(stack);

        int width = 0;
        int height = 0;
        int depth = 0;
        if (valve.bottomDiagFrame != null && valve.topDiagFrame != null) {
            width = valve.topDiagFrame.getX() - valve.bottomDiagFrame.getX() + 1;
            height = valve.topDiagFrame.getY() - valve.bottomDiagFrame.getY() + 1;
            depth = valve.topDiagFrame.getZ() - valve.bottomDiagFrame.getZ() + 1;
        }

        return new Data(
            valve.isValid(),
            frame,
            valve.getAutoOutput(),
            valve.isMaster(),
            valve.getFluidAmount(),
            valve.getCapacity(),
            temperature,
            valve.tankHeight,
            valve.valveHeightPosition,
            width,
            height,
            depth,
            fluidName,
            valve.getValveName());
    }
}
