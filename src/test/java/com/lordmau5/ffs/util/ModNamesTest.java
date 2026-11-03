package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModNamesTest {

    @Test
    void modAndResourceDomainAreLowercase() {
        assertEquals("ffs", ModNames.MODID);
        assertEquals("ffs", ModNames.RESOURCE_DOMAIN);
        assertEquals("ffs:textures/gui/gui_tank.png", ModNames.resource("textures/gui/gui_tank.png"));
    }

    @Test
    void tileEntityNamesAreNamespacedAndKeepLegacyAliases() {
        assertTrue(ModNames.TE_VALVE.startsWith("ffs:"));
        assertTrue(ModNames.TE_FRAME.startsWith("ffs:"));
        assertFalse(ModNames.TE_VALVE_LEGACY.contains(":"));
        assertFalse(ModNames.TE_FRAME_LEGACY.contains(":"));
    }
}
