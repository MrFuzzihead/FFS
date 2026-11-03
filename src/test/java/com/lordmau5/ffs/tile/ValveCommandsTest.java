package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;

class ValveCommandsTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void invalidValveReportsEmptyFluid() {
        TileEntityValve valve = new TileEntityValve();

        assertNull(ValveCommands.fluidName(valve));
        assertEquals(0, ValveCommands.fluidAmount(valve));
        assertEquals(0, ValveCommands.fluidCapacity(valve));
    }

    @Test
    void autoOutputMapContainsTheValve() {
        TileEntityValve valve = new TileEntityValve();

        Map<String, Boolean> outputs = ValveCommands.autoOutputs(valve, null);
        assertNotNull(outputs);
        assertEquals(1, outputs.size());
        assertTrue(outputs.containsKey(valve.getValveName()));
        assertFalse(outputs.get(valve.getValveName()));
    }

    @Test
    void unknownValveNameReturnsNull() {
        TileEntityValve valve = new TileEntityValve();

        assertNull(ValveCommands.autoOutputs(valve, "missing"));
        assertNull(ValveCommands.setAutoOutputByName(valve, "missing", true));
    }

    @Test
    void settingOutputOnADetachedValveDoesNotThrow() {
        TileEntityValve valve = new TileEntityValve();

        ValveCommands.setAutoOutput(valve, true);
        assertNull(ValveCommands.setAutoOutputByName(valve, "missing", false));
    }
}
