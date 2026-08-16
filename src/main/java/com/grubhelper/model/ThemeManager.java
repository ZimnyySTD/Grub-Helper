// Package declaration for GRUB theme manager backend
package com.grubhelper.model;

// Import File class for filesystem operations
import java.io.File;
// Import FileInputStream for reading zip archive input streams
import java.io.FileInputStream;
// Import FileOutputStream for writing extracted files
import java.io.FileOutputStream;
// Import IOException for handling file system exceptions
import java.io.IOException;
// Import OutputStream for binary file writing
import java.io.OutputStream;
// Import Path for java.nio path operations
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
// Import ArrayList for dynamic lists
import java.util.ArrayList;
// Import List interface
import java.util.List;
// Import ZipEntry for zip entries
import java.util.zip.ZipEntry;
// Import ZipInputStream for unzipping zip archives
import java.util.zip.ZipInputStream;

/**
 * Manages finding, installing, and managing GRUB themes in /boot/grub/themes.
 */
public class ThemeManager {

    // Instance variable storing reference to system GRUB environment detector
    private final GrubEnvironment environment;

    /**
     * Constructor for ThemeManager.
     * @param environment GrubEnvironment reference
     */
    public ThemeManager(GrubEnvironment environment) {
        // Store environment instance variable
        this.environment = environment;
    }

    /**
     * Returns a list of all valid installed GRUB themes in themes directory.
     * @return List of GrubTheme objects
     */
    public List<GrubTheme> getInstalledThemes() {
        // Initialize list to collect discovered themes
        List<GrubTheme> themes = new ArrayList<>();
        // Create File object for system themes directory
        File themesDir = new File(environment.getGrubThemesDir());

        // Return empty list if themes directory does not exist or is not a directory
        if (!themesDir.exists() || !themesDir.isDirectory()) {
            // Return empty list
            return themes;
        }

        // List all subdirectories inside themes directory
        File[] subDirs = themesDir.listFiles(File::isDirectory);
        // Return empty list if subDirs array is null
        if (subDirs == null) return themes;

        // Loop through each subfolder in themes directory
        for (File dir : subDirs) {
            // Attempt to parse subfolder into a GrubTheme object
            GrubTheme theme = parseThemeDir(dir);
            // If theme object is valid, add to themes list
            if (theme != null) {
                // Add theme to list
                themes.add(theme);
            }
        }

        // Return list of discovered themes
        return themes;
    }

    /**
     * Parses a theme directory looking for theme.txt and preview images.
     * @param dir Directory File object
     * @return GrubTheme instance or null if invalid
     */
    public GrubTheme parseThemeDir(File dir) {
        // Create File object for theme.txt inside dir
        File themeTxt = new File(dir, "theme.txt");
        // If theme.txt does not exist
        if (!themeTxt.exists()) {
            // Check if there is a single subfolder containing theme.txt
            File[] files = dir.listFiles(File::isDirectory);
            // Check if files array contains exactly 1 subfolder
            if (files != null && files.length == 1 && new File(files[0], "theme.txt").exists()) {
                // Redirect dir to subfolder
                dir = files[0];
                // Redirect themeTxt reference to subfolder theme.txt
                themeTxt = new File(dir, "theme.txt");
            } else {
                // Return null if theme.txt is missing
                return null;
            }
        }

        // Find preview image inside theme folder
        File preview = findPreviewImage(dir);

        // Return new GrubTheme instance
        return new GrubTheme(dir.getName(), dir, themeTxt, preview);
    }

    /**
     * Helper method to find preview/screenshot images inside a theme directory.
     */
    private File findPreviewImage(File dir) {
        // Define list of preferred preview image file names
        String[] preferredNames = {
            "preview.png", "preview.jpg", "preview.jpeg",
            "screenshot.png", "screenshot.jpg", "screenshot.jpeg",
            "background.png", "background.jpg"
        };

        // Loop through preferred file names
        for (String name : preferredNames) {
            // Create File instance for candidate image
            File f = new File(dir, name);
            // If candidate image exists, return it
            if (f.exists()) return f;
        }

        // Fallback: list all png or jpg files in directory
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        // Return first png or jpg file if available
        if (files != null && files.length > 0) {
            // Return first image file
            return files[0];
        }

        // Return null if no preview image found
        return null;
    }

    /**
     * Installs a theme archive (.zip, .tar.gz, .tar.xz, .tar) to target themes directory.
     */
    public GrubTheme installThemeArchive(File archiveFile, File targetThemesDir) throws Exception {
        // Ensure target directory exists
        if (!targetThemesDir.exists()) {
            // Create target directories
            targetThemesDir.mkdirs();
        }

        // Get lower-case archive file name
        String fileName = archiveFile.getName().toLowerCase();
        // Create temporary directory for staging extraction
        File tempDir = Files.createTempDirectory("grub_theme_extract_").toFile();

        try {
            // Check if archive is .zip
            if (fileName.endsWith(".zip")) {
                // Extract zip archive
                unzip(archiveFile, tempDir);
            // Else check if archive is .tar, .tar.gz, .tgz, or .tar.xz
            } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar.xz") || fileName.endsWith(".tar")) {
                // Extract tar archive
                untar(archiveFile, tempDir);
            } else {
                // Throw error for unsupported archive format
                throw new IllegalArgumentException("Unsupported archive format: " + archiveFile.getName());
            }

            // Find theme directory containing theme.txt inside extracted contents
            File themeFolder = findThemeFolderInExtracted(tempDir);
            // Check if theme folder was found
            if (themeFolder == null) {
                // Throw error if theme.txt is missing
                throw new IllegalArgumentException("No theme.txt found inside the archive: " + archiveFile.getName());
            }

            // Get theme folder name
            String themeFolderName = themeFolder.getName();
            // Create destination folder File object inside targetThemesDir
            File destFolder = new File(targetThemesDir, themeFolderName);

            // Copy contents recursively to target directory
            copyDirectory(themeFolder.toPath(), destFolder.toPath());

            // Parse and return installed theme object
            return parseThemeDir(destFolder);

        } finally {
            // Delete temporary extraction directory in finally block
            deleteDirectory(tempDir);
        }
    }

    /**
     * Recursively searches for directory containing theme.txt in extracted tree.
     */
    private File findThemeFolderInExtracted(File dir) {
        // Create File object for theme.txt
        File themeTxt = new File(dir, "theme.txt");
        // Return dir if theme.txt exists directly
        if (themeTxt.exists()) {
            // Theme folder found
            return dir;
        }

        // List files in directory
        File[] files = dir.listFiles();
        // Loop through files if non-null
        if (files != null) {
            // Loop through entries
            for (File f : files) {
                // If entry is a directory, search recursively
                if (f.isDirectory()) {
                    // Recursively search subfolder
                    File found = findThemeFolderInExtracted(f);
                    // Return found folder if non-null
                    if (found != null) return found;
                }
            }
        }
        // Return null if not found
        return null;
    }

    /**
     * Unzips a zip archive safely preventing Zip-Slip directory traversal attacks.
     */
    private void unzip(File zipFile, File destDir) throws IOException {
        // Create ZipInputStream from FileInputStream
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            // Declare ZipEntry variable
            ZipEntry entry;
            // Iterate through zip entries
            while ((entry = zis.getNextEntry()) != null) {
                // Create target file object for zip entry
                File target = new File(destDir, entry.getName());
                // Check against Zip-Slip path traversal attack
                if (!target.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    // Throw security exception
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                }
                // Check if entry is directory
                if (entry.isDirectory()) {
                    // Make directory
                    target.mkdirs();
                } else {
                    // Make parent directories
                    target.getParentFile().mkdirs();
                    // Write file bytes from zip stream
                    try (OutputStream os = new FileOutputStream(target)) {
                        // Create byte buffer
                        byte[] buffer = new byte[8192];
                        // Declare len variable
                        int len;
                        // Read bytes into buffer
                        while ((len = zis.read(buffer)) > 0) {
                            // Write bytes to output stream
                            os.write(buffer, 0, len);
                        }
                    }
                }
                // Close current zip entry
                zis.closeEntry();
            }
        }
    }

    /**
     * Extracts tar archives using system tar command via ProcessBuilder.
     */
    private void untar(File tarFile, File destDir) throws Exception {
        // Build tar extraction command process
        ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getAbsolutePath(), "-C", destDir.getAbsolutePath());
        // Start tar process
        Process process = pb.start();
        // Wait for process completion and get exit code
        int exitCode = process.waitFor();
        // Check exit code
        if (exitCode != 0) {
            // Throw exception if tar command failed
            throw new IOException("Failed to extract tar archive: " + tarFile.getName());
        }
    }

    /**
     * Recursively copies files and subdirectories from source path to target path.
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        // Walk file tree from source path
        Files.walk(source).forEach(sourcePath -> {
            try {
                // Resolve target path relative to source
                Path targetPath = target.resolve(source.relativize(sourcePath));
                // Check if source path is directory
                if (Files.isDirectory(sourcePath)) {
                    // Create target directory if it does not exist
                    if (!Files.exists(targetPath)) {
                        // Create directories
                        Files.createDirectories(targetPath);
                    }
                } else {
                    // Copy file replacing existing file
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                // Wrap in RuntimeException for stream lambda
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Recursively deletes directory and its contents.
     */
    private void deleteDirectory(File dir) {
        // Check if dir is directory
        if (dir.isDirectory()) {
            // List files in directory
            File[] files = dir.listFiles();
            // Loop through files if non-null
            if (files != null) {
                // Loop through files
                for (File f : files) {
                    // Recursively delete sub-files
                    deleteDirectory(f);
                }
            }
        }
        // Delete file or directory
        dir.delete();
    }
}
