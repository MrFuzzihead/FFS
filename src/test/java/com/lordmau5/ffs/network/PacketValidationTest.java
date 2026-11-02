package com.lordmau5.ffs.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PacketValidationTest {

    @Test
    void reachUsesTheBlockCentre() {
        assertTrue(PacketValidation.isWithinReach(0, 0, 0, 0.5D, 0.5D, 0.5D, PacketValidation.MAX_REACH));
        assertTrue(PacketValidation.isWithinReach(8, 0, 0, 0.5D, 0.5D, 0.5D, PacketValidation.MAX_REACH));
        assertFalse(PacketValidation.isWithinReach(9, 0, 0, 0.5D, 0.5D, 0.5D, PacketValidation.MAX_REACH));
        assertFalse(PacketValidation.isWithinReach(0, 20, 0, 0.5D, 0.5D, 0.5D, PacketValidation.MAX_REACH));
    }

    @Test
    void valveNamesAreLengthLimited() {
        assertFalse(PacketValidation.isValidValveName(null));
        assertFalse(PacketValidation.isValidValveName(""));
        assertTrue(PacketValidation.isValidValveName("valve_1"));
        assertTrue(PacketValidation.isValidValveName(repeat('a', PacketValidation.MAX_NAME_LENGTH)));
        assertFalse(PacketValidation.isValidValveName(repeat('a', PacketValidation.MAX_NAME_LENGTH + 1)));
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
