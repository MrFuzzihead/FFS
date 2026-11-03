package com.lordmau5.ffs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class LicenseNoticeTest {

    @Test
    void noticeKeepsAttributionAndLicenseTerms() throws Exception {
        String notice = read("NOTICE.md");

        assertTrue(notice.contains("Lordmau5"));
        assertTrue(notice.contains("https://github.com/Lordmau5/FFS"));
        assertTrue(notice.contains("Creative Commons Attribution-NonCommercial-ShareAlike 3.0"));
        assertTrue(notice.contains("https://creativecommons.org/licenses/by-nc-sa/3.0/"));
        assertTrue(notice.contains("modified"));
        assertTrue(
            notice.toLowerCase()
                .contains("not mit"));
    }

    @Test
    void licenseIsTheUpstreamShareAlikeText() throws Exception {
        String license = read("LICENSE.md");

        assertTrue(license.startsWith("THE WORK"));
        assertTrue(license.contains("Attribution, Noncommercial, ShareAlike"));
        assertFalse(license.contains("MIT License"));
    }

    @Test
    void buildIncludesTheLicenseAndNoticeInJars() throws Exception {
        String build = read("build.gradle.kts");
        assertTrue(build.contains("LICENSE.md"));
        assertTrue(build.contains("NOTICE.md"));
    }

    private static String read(String file) throws Exception {
        Path path = Paths.get(file);
        assertTrue(Files.isRegularFile(path), "missing " + file);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
