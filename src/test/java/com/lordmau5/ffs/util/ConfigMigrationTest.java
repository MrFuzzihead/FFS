package com.lordmau5.ffs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMigrationTest {

    @TempDir
    Path dir;

    @Test
    void copiesTheLegacyConfigOnce() throws Exception {
        // Use a distinct name so the case-insensitive Windows filesystem does not
        // collapse the two paths; the production FFS.cfg -> ffs.cfg rename is the
        // same code path on case-sensitive filesystems.
        File legacy = this.dir.resolve("legacy.cfg")
            .toFile();
        File target = this.dir.resolve("ffs.cfg")
            .toFile();
        Files.write(legacy.toPath(), "general { maxSize=13 }".getBytes(StandardCharsets.UTF_8));

        assertTrue(ConfigMigration.copyLegacyIfNeeded(target, legacy));
        assertTrue(target.exists());
        assertEquals(
            new String(Files.readAllBytes(legacy.toPath()), StandardCharsets.UTF_8),
            new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
        assertFalse(ConfigMigration.copyLegacyIfNeeded(target, legacy));
    }

    @Test
    void doesNotOverwriteAnExistingTarget() throws Exception {
        File legacy = this.dir.resolve("FFS.cfg")
            .toFile();
        File target = this.dir.resolve("ffs.cfg")
            .toFile();
        Files.write(legacy.toPath(), "old".getBytes(StandardCharsets.UTF_8));
        Files.write(target.toPath(), "new".getBytes(StandardCharsets.UTF_8));

        assertFalse(ConfigMigration.copyLegacyIfNeeded(target, legacy));
        assertEquals("new", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void missingLegacyFileIsANoOp() throws Exception {
        File target = this.dir.resolve("ffs.cfg")
            .toFile();
        File legacy = this.dir.resolve("FFS.cfg")
            .toFile();

        assertFalse(ConfigMigration.copyLegacyIfNeeded(target, legacy));
        assertFalse(target.exists());
    }
}
