package com.lordmau5.ffs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/** Verifies the processed resources that ship in the jar. */
class PackagingTest {

    @Test
    void mcmodInfoUsesTheLowercaseModIdAndSoftDependencies() {
        String mcmod = read("mcmod.info");

        assertTrue(mcmod.contains("\"modid\": \"ffs\""));
        assertTrue(mcmod.contains("after:waila"));
        assertTrue(mcmod.contains("after:JAKJ_RedstoneInMotion"));
        assertFalse(mcmod.contains("${modId}"));
    }

    @Test
    void languageAndSoundResourcesExistUnderTheLowercaseDomain() {
        String lang = read("assets/ffs/lang/en_US.lang");
        assertTrue(lang.contains("tile.blockValve.name=Tank Valve"));
        assertTrue(lang.contains("tile.blockTankFrame.name=Tank Frame"));

        String sounds = read("assets/ffs/sounds.json");
        assertTrue(sounds.contains("\"fire\""));
    }

    private static String read(String resource) {
        InputStream stream = PackagingTest.class.getClassLoader()
            .getResourceAsStream(resource);
        assertNotNull(stream, "missing resource: " + resource);
        try (Scanner scanner = new Scanner(stream, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
