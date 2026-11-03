package com.lordmau5.ffs.tile;

import java.util.List;
import java.util.Map;

/** Shared argument handling for the ComputerCraft and OpenComputers adapters. */
public final class ValveCommandAdapter {

    public static final String[] METHOD_NAMES = { "getFluidName", "getFluidAmount", "getFluidCapacity", "setAutoOutput",
        "doesAutoOutput" };

    public interface Args {

        int count();

        boolean isBoolean(int index);

        boolean getBoolean(int index);

        boolean isString(int index);

        String getString(int index);

        Object get(int index);
    }

    public static final class CommandException extends Exception {

        public CommandException(String message) {
            super(message);
        }
    }

    /** Adapter for ComputerCraft's {@code Object[]} arguments. */
    public static final class ObjectArgs implements Args {

        private final Object[] arguments;

        public ObjectArgs(Object[] arguments) {
            this.arguments = arguments == null ? new Object[0] : arguments;
        }

        @Override
        public int count() {
            return this.arguments.length;
        }

        @Override
        public boolean isBoolean(int index) {
            return index >= 0 && index < this.arguments.length && this.arguments[index] instanceof Boolean;
        }

        @Override
        public boolean getBoolean(int index) {
            return (Boolean) this.arguments[index];
        }

        @Override
        public boolean isString(int index) {
            return index >= 0 && index < this.arguments.length && this.arguments[index] instanceof String;
        }

        @Override
        public String getString(int index) {
            return (String) this.arguments[index];
        }

        @Override
        public Object get(int index) {
            return index >= 0 && index < this.arguments.length ? this.arguments[index] : null;
        }
    }

    private ValveCommandAdapter() {}

    public static Object[] execute(TileEntityValve valve, String method, Args args) throws CommandException {
        if (method == null) {
            throw new CommandException("unknown method");
        }

        switch (method) {
            case "getFluidName":
                String fluid = ValveCommands.fluidName(valve);
                return fluid == null ? null : new Object[] { fluid };
            case "getFluidAmount":
                return new Object[] { ValveCommands.fluidAmount(valve) };
            case "getFluidCapacity":
                return new Object[] { ValveCommands.fluidCapacity(valve) };
            case "setAutoOutput":
                return setAutoOutput(valve, args);
            case "doesAutoOutput":
                return doesAutoOutput(valve, args);
            default:
                throw new CommandException("unknown method: " + method);
        }
    }

    private static Object[] setAutoOutput(TileEntityValve valve, Args args) throws CommandException {
        if (args.count() == 1) {
            requireBoolean(args, 0);
            boolean value = args.getBoolean(0);
            ValveCommands.setAutoOutput(valve, value);
            return new Object[] { value };
        }

        if (args.count() != 2) {
            throw new CommandException("insufficient number of arguments found - expected 1 or 2, got " + args.count());
        }

        requireString(args, 0);
        requireBoolean(args, 1);
        List<String> names = ValveCommands.setAutoOutputByName(valve, args.getString(0), args.getBoolean(1));
        if (names == null) {
            throw new CommandException("no valves found");
        }

        return new Object[] { names };
    }

    private static Object[] doesAutoOutput(TileEntityValve valve, Args args) throws CommandException {
        if (args.count() == 0) {
            return new Object[] { ValveCommands.autoOutputs(valve, null) };
        }

        if (args.count() != 1) {
            throw new CommandException("insufficient number of arguments found - expected 1, got " + args.count());
        }

        requireString(args, 0);
        Map<String, Boolean> outputs = ValveCommands.autoOutputs(valve, args.getString(0));
        if (outputs == null) {
            throw new CommandException("no valves found");
        }

        return new Object[] { outputs };
    }

    private static void requireBoolean(Args args, int index) throws CommandException {
        if (!args.isBoolean(index)) {
            throw typeError(index, "boolean", args.get(index));
        }
    }

    private static void requireString(Args args, int index) throws CommandException {
        if (!args.isString(index)) {
            throw typeError(index, "String", args.get(index));
        }
    }

    private static CommandException typeError(int index, String expected, Object actual) {
        String found = actual == null ? "null"
            : actual.getClass()
                .getSimpleName();
        return new CommandException(
            "expected argument " + (index + 1) + " to be of type \"" + expected + "\", found \"" + found + "\"");
    }
}
