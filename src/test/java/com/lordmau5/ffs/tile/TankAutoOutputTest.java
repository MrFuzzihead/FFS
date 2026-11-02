package com.lordmau5.ffs.tile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.lordmau5.ffs.test.TestBootstrap;

class TankAutoOutputTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.ensureGameRegistry();
    }

    @Test
    void detachedValveIsSafe() {
        assertDoesNotThrow(() -> TankAutoOutput.tick(new TileEntityValve()));
    }
}
