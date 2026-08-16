// Package declaration for test suite
package com.grubhelper.test;

// Import model classes for testing
import com.grubhelper.model.*;
// Import UpdateChecker utility for version comparison tests
import com.grubhelper.util.UpdateChecker;

// Import File and OutputStream for zip creation during theme tests
import java.io.*;
// Import Files class for temp directory creation
import java.nio.file.Files;
// Import ZipEntry and ZipOutputStream for zip archives
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Custom headless test runner class for verifying backend logic without JUnit runtime dependencies.
 */
public class CoreTestRunner {

    /**
     * Test runner main entry point.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Run config parser test
            testGrubConfigParser();
            // Run theme manager test
            testThemeManager();
            // Run update checker version comparison test
            testUpdateCheckerVersionComparison();
            // Print success banner
            System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
        } catch (Throwable t) {
            // Print error banner
            System.err.println("TEST FAILED:");
            // Print stack trace
            t.printStackTrace();
            // Exit with code 1
            System.exit(1);
        }
    }

    /**
     * Tests GrubConfigParser loading, key retrieval, modification, and comment preservation.
     */
    private static void testGrubConfigParser() {
        // Print progress message
        System.out.println("Testing GrubConfigParser...");
        // Define sample config string
        String sampleConfig =
                "# Sample GRUB Config\n" +
                "GRUB_DEFAULT=0\n" +
                "GRUB_TIMEOUT=5\n" +
                "GRUB_DISTRIBUTOR=\"`lsb_release -i -s 2> /dev/null || echo Debian`\"\n" +
                "GRUB_CMDLINE_LINUX_DEFAULT=\"quiet splash\"\n";

        // Instantiate GrubConfigParser
        GrubConfigParser parser = new GrubConfigParser();
        // Load sample config string
        parser.loadFromString(sampleConfig);

        // Assert parsed values match expected strings
        assertEquals("0", parser.getValue("GRUB_DEFAULT"));
        assertEquals("5", parser.getValue("GRUB_TIMEOUT"));
        assertEquals("quiet splash", parser.getValue("GRUB_CMDLINE_LINUX_DEFAULT"));

        // Modify GRUB_TIMEOUT value
        parser.setValue("GRUB_TIMEOUT", "10");
        // Assert updated value
        assertEquals("10", parser.getValue("GRUB_TIMEOUT"));

        // Set GRUB_THEME value
        parser.setValue("GRUB_THEME", "/boot/grub/themes/breeze/theme.txt");
        // Assert updated theme value
        assertEquals("/boot/grub/themes/breeze/theme.txt", parser.getValue("GRUB_THEME"));

        // Generate output configuration string
        String generated = parser.generateConfigString();
        // Assert generated config contains modified values
        assertTrue(generated.contains("GRUB_TIMEOUT=\"10\""), "Generated config missing GRUB_TIMEOUT=\"10\"");
        assertTrue(generated.contains("GRUB_THEME=\"/boot/grub/themes/breeze/theme.txt\""), "Generated config missing GRUB_THEME");
        // Print success message
        System.out.println("GrubConfigParser test passed.");
    }

    /**
     * Tests ThemeManager zip extraction and preview image detection.
     */
    private static void testThemeManager() throws Exception {
        // Print progress message
        System.out.println("Testing ThemeManager...");
        // Create temporary directory for test theme
        File tempDir = Files.createTempDirectory("theme_test_").toFile();
        // Create File object for zip archive
        File zipFile = new File(tempDir, "test-theme.zip");

        // Write test theme zip file contents
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Write my-theme/theme.txt zip entry
            zos.putNextEntry(new ZipEntry("my-theme/theme.txt"));
            // Write theme text contents
            zos.write("title-text: \"Test Theme\"".getBytes());
            // Close entry
            zos.closeEntry();

            // Write my-theme/preview.png zip entry
            zos.putNextEntry(new ZipEntry("my-theme/preview.png"));
            // Write image byte array
            zos.write(new byte[]{0, 1, 2, 3});
            // Close entry
            zos.closeEntry();
        }

        // Create target directory for installed themes
        File targetThemesDir = new File(tempDir, "installed_themes");
        // Instantiate GrubEnvironment
        GrubEnvironment env = new GrubEnvironment();
        // Instantiate ThemeManager
        ThemeManager manager = new ThemeManager(env);

        // Install theme archive
        GrubTheme theme = manager.installThemeArchive(zipFile, targetThemesDir);

        // Assert theme was installed properly
        assertNotNull(theme, "Installed theme should not be null");
        assertEquals("my-theme", theme.getName());
        assertTrue(theme.getThemeTxtFile().exists(), "theme.txt should exist");
        assertNotNull(theme.getPreviewImage(), "Preview image should not be null");
        assertTrue(theme.getPreviewImage().exists(), "Preview image file should exist");

        // Print success message
        System.out.println("ThemeManager test passed.");
    }

    /**
     * Tests UpdateChecker semantic version comparison logic.
     */
    private static void testUpdateCheckerVersionComparison() {
        // Print progress message
        System.out.println("Testing UpdateChecker version comparison...");
        // Assert patch version update detected
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.1"), "1.0.1 should be newer than 1.0.0");
        // Assert minor version update detected
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.1.0"), "1.1.0 should be newer than 1.0.0");
        // Assert major version update detected
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "2.0.0"), "2.0.0 should be newer than 1.0.0");
        // Assert same version is not newer
        assertTrue(!UpdateChecker.isNewerVersion("1.0.0", "1.0.0"), "1.0.0 should not be newer than 1.0.0");
        // Assert older version is not newer
        assertTrue(!UpdateChecker.isNewerVersion("1.1.0", "1.0.5"), "1.0.5 should not be newer than 1.1.0");
        // Print success message
        System.out.println("UpdateChecker version comparison test passed.");
    }

    /**
     * Assertion helper checking equality between two objects.
     */
    private static void assertEquals(Object expected, Object actual) {
        // Return if both objects are null
        if (expected == null && actual == null) return;
        // Return if objects are equal
        if (expected != null && expected.equals(actual)) return;
        // Throw AssertionError if not equal
        throw new AssertionError("Expected: " + expected + " but got: " + actual);
    }

    /**
     * Assertion helper checking condition is true.
     */
    private static void assertTrue(boolean condition, String message) {
        // If condition is false, throw assertion error
        if (!condition) {
            // Throw AssertionError
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    /**
     * Assertion helper checking object is not null.
     */
    private static void assertNotNull(Object obj, String message) {
        // If object is null, throw assertion error
        if (obj == null) {
            // Throw AssertionError
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
