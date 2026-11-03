package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoOutputStateTest {

    @Test
    void togglesState() {
        AutoOutputState state = new AutoOutputState();
        assertFalse(state.isEnabled());
        state.setEnabled(true);
        assertTrue(state.isEnabled());
        state.setEnabled(false);
        assertFalse(state.isEnabled());
    }

    @Test
    void syncIsPendingUntilConsumedAndThrottled() {
        AutoOutputState state = new AutoOutputState();
        state.requestSync();
        assertTrue(state.isPendingSync());
        assertTrue(state.consumeSync(0, 2));
        assertFalse(state.isPendingSync());

        state.requestSync();
        assertFalse(state.consumeSync(1, 2));
        assertTrue(state.isPendingSync());
        assertTrue(state.consumeSync(2, 2));
        assertFalse(state.isPendingSync());
    }
}
