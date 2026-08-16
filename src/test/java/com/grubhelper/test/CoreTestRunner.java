package com.grubhelper.test;

import com.grubhelper.model.*;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CoreTestRunner {

    public static void main(String[] args) {
        try {
            testGrubConfigParser();
            testThemeManager();
            System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
        } catch (Throwable t) {
            System.err.println("TEST FAILED:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testGrubConfigParser() {
        System.out.println("Testing GrubConfigParser...");
        String sampleConfig =
                "# Sample GRUB Config\n" +
                "GRUB_DEFAULT=0\n" +
                "GRUB_TIMEOUT=5\n" +
                "GRUB_DISTRIBUTOR=\"`lsb_release -i -s 2> /dev/null || echo Debian`\"\n" +
                "GRUB_CMDLINE_LINUX_DEFAULT=\"quiet splash\"\n";

        GrubConfigParser parser = new GrubConfigParser();
        parser.loadFromString(sampleConfig);

        assertEquals("0", parser.getValue("GRUB_DEFAULT"));
        assertEquals("5", parser.getValue("GRUB_TIMEOUT"));
        assertEquals("quiet splash", parser.getValue("GRUB_CMDLINE_LINUX_DEFAULT"));

        parser.setValue("GRUB_TIMEOUT", "10");
        assertEquals("10", parser.getValue("GRUB_TIMEOUT"));

        parser.setValue("GRUB_THEME", "/boot/grub/themes/breeze/theme.txt");
        assertEquals("/boot/grub/themes/breeze/theme.txt", parser.getValue("GRUB_THEME"));

        String generated = parser.generateConfigString();
        assertTrue(generated.contains("GRUB_TIMEOUT=\"10\""), "Generated config missing GRUB_TIMEOUT=\"10\"");
        assertTrue(generated.contains("GRUB_THEME=\"/boot/grub/themes/breeze/theme.txt\""), "Generated config missing GRUB_THEME");
        System.out.println("GrubConfigParser test passed.");
    }

    private static void testThemeManager() throws Exception {
        System.out.println("Testing ThemeManager...");
        File tempDir = Files.createTempDirectory("theme_test_").toFile();
        File zipFile = new File(tempDir, "test-theme.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("my-theme/theme.txt"));
            zos.write("title-text: \"Test Theme\"".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("my-theme/preview.png"));
            zos.write(new byte[]{0, 1, 2, 3});
            zos.closeEntry();
        }

        File targetThemesDir = new File(tempDir, "installed_themes");
        GrubEnvironment env = new GrubEnvironment();
        ThemeManager manager = new ThemeManager(env);

        GrubTheme theme = manager.installThemeArchive(zipFile, targetThemesDir);

        assertNotNull(theme, "Installed theme should not be null");
        assertEquals("my-theme", theme.getName());
        assertTrue(theme.getThemeTxtFile().exists(), "theme.txt should exist");
        assertNotNull(theme.getPreviewImage(), "Preview image should not be null");
        assertTrue(theme.getPreviewImage().exists(), "Preview image file should exist");

        System.out.println("ThemeManager test passed.");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Expected: " + expected + " but got: " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
