package com.lordmau5.ffs.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The mod id changed from {@code FFS} to {@code ffs} for resource-domain safety.
 * FML derives the config filename from the id, so migrate the legacy {@code FFS.cfg}
 * once instead of silently resetting every setting.
 */
public final class ConfigMigration {

    private ConfigMigration() {}

    public static boolean copyLegacyIfNeeded(File target, File legacy) throws IOException {
        if (target == null || legacy == null || target.exists() || !legacy.exists() || legacy.isDirectory()) {
            return false;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }

        Files.copy(legacy.toPath(), target.toPath());
        return true;
    }
}
