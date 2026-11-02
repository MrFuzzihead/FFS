package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;

class ValveCommandAdapterTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void readOnlyCommandsReturnEmptyTankValues() throws Exception {
        TileEntityValve valve = new TileEntityValve();
        ValveCommandAdapter.ObjectArgs args = new ValveCommandAdapter.ObjectArgs(new Object[0]);

        assertNull(ValveCommandAdapter.execute(valve, "getFluidName", args));
        assertArrayEquals(new Object[] { 0 }, ValveCommandAdapter.execute(valve, "getFluidAmount", args));
        assertArrayEquals(new Object[] { 0 }, ValveCommandAdapter.execute(valve, "getFluidCapacity", args));
    }

    @Test
    void autoOutputCommandsUseTheValveName() throws Exception {
        TileEntityValve valve = new TileEntityValve();
        valve.setMaster(true);
        String name = valve.getValveName();

        Object[] set = ValveCommandAdapter
            .execute(valve, "setAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[] { true }));
        assertArrayEquals(new Object[] { true }, set);

        Object[] all = ValveCommandAdapter
            .execute(valve, "doesAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[0]));
        assertTrue(all[0] instanceof Map);
        assertEquals(1, ((Map<?, ?>) all[0]).size());

        Object[] byName = ValveCommandAdapter
            .execute(valve, "setAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[] { name, true }));
        assertNotNull(byName);
        assertEquals(1, ((java.util.List<?>) byName[0]).size());

        Object[] outputs = ValveCommandAdapter
            .execute(valve, "doesAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[] { name }));
        assertEquals(1, ((Map<?, ?>) outputs[0]).size());
    }

    @Test
    void invalidArgumentsAndMethodsThrow() {
        TileEntityValve valve = new TileEntityValve();

        assertThrows(
            ValveCommandAdapter.CommandException.class,
            () -> ValveCommandAdapter
                .execute(valve, "setAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[] { "notBoolean" })));
        assertThrows(
            ValveCommandAdapter.CommandException.class,
            () -> ValveCommandAdapter
                .execute(valve, "doesAutoOutput", new ValveCommandAdapter.ObjectArgs(new Object[] { 1 })));
        assertThrows(
            ValveCommandAdapter.CommandException.class,
            () -> ValveCommandAdapter.execute(valve, "unknown", new ValveCommandAdapter.ObjectArgs(new Object[0])));
        assertThrows(
            ValveCommandAdapter.CommandException.class,
            () -> ValveCommandAdapter.execute(valve, null, new ValveCommandAdapter.ObjectArgs(new Object[0])));
    }

    @Test
    void nullArgumentsBehaveLikeAnEmptyCall() {
        ValveCommandAdapter.ObjectArgs args = new ValveCommandAdapter.ObjectArgs(null);
        assertEquals(0, args.count());
        assertNull(args.get(0));
    }
}
