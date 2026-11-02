package com.lordmau5.ffs.tile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fluids.FluidStack;

/**
 * Shared command semantics for the ComputerCraft peripheral and the
 * OpenComputers component. Argument type checking stays in each adapter; this
 * class owns the actual tank/valve operations.
 */
public final class ValveCommands {

    private ValveCommands() {}

    public static String fluidName(TileEntityValve valve) {
        FluidStack stack = valve.getFluid();
        return stack == null ? null : stack.getLocalizedName();
    }

    public static int fluidAmount(TileEntityValve valve) {
        return valve.getFluidAmount();
    }

    public static int fluidCapacity(TileEntityValve valve) {
        return valve.getCapacity();
    }

    public static void setAutoOutput(TileEntityValve valve, boolean value) {
        for (TileEntityValve other : valve.getAllValves()) {
            other.setAutoOutput(value);
        }
    }

    /** @return the affected valve names, or {@code null} when the name matches nothing */
    public static List<String> setAutoOutputByName(TileEntityValve valve, String name, boolean value) {
        List<TileEntityValve> valves = valve.getValvesByName(name);
        if (valves.isEmpty()) {
            return null;
        }

        List<String> names = new java.util.ArrayList<>(valves.size());
        for (TileEntityValve other : valves) {
            other.setAutoOutput(value);
            names.add(other.getValveName());
        }

        return names;
    }

    /**
     * @param name {@code null} for every valve in the tank, otherwise a valve name
     * @return the output state map, or {@code null} when a name matches nothing
     */
    public static Map<String, Boolean> autoOutputs(TileEntityValve valve, String name) {
        List<TileEntityValve> valves = name == null ? valve.getAllValves() : valve.getValvesByName(name);
        if (name != null && valves.isEmpty()) {
            return null;
        }

        Map<String, Boolean> outputs = new LinkedHashMap<>();
        for (TileEntityValve other : valves) {
            outputs.put(other.getValveName(), other.getAutoOutput());
        }

        return outputs;
    }
}
