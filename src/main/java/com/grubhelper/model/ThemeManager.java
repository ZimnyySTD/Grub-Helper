package com.grubhelper.model;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages finding, installing, and managing GRUB themes in /boot/grub/themes.
 */
public class ThemeManager {

    private final GrubEnvironment environment;

    public ThemeManager(GrubEnvironment environment) {
        this.environment = environment;
    }

    public List<GrubTheme> getInstalledThemes() {
        List<GrubTheme> themes = new ArrayList<>();
        File themesDir = new File(environment.getGrubThemesDir());

        if (!themesDir.exists() || !themesDir.isDirectory()) {
            return themes;
        }

        File[] subDirs = themesDir.listFiles(File::isDirectory);
        if (subDirs == null) return themes;

        for (File dir : subDirs) {
            GrubTheme theme = parseThemeDir(dir);
            if (theme != null) {
                themes.add(theme);
            }
        }

        return themes;
    }

    public GrubTheme parseThemeDir(File dir) {
        File themeTxt = new File(dir, "theme.txt");
        if (!themeTxt.exists()) {
            // Check if there is a single subfolder containing theme.txt
            File[] files = dir.listFiles(File::isDirectory);
            if (files != null && files.length == 1 && new File(files[0], "theme.txt").exists()) {
                dir = files[0];
                themeTxt = new File(dir, "theme.txt");
            } else {
                return null;
            }
        }

        // Search for preview image
        File preview = findPreviewImage(dir);

        return new GrubTheme(dir.getName(), dir, themeTxt, preview);
    }

    private File findPreviewImage(File dir) {
        String[] preferredNames = {
            "preview.png", "preview.jpg", "preview.jpeg",
            "screenshot.png", "screenshot.jpg", "screenshot.jpeg",
            "background.png", "background.jpg"
        };

        for (String name : preferredNames) {
            File f = new File(dir, name);
            if (f.exists()) return f;
        }

        // Fallback: first png or jpg in directory
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        if (files != null && files.length > 0) {
            return files[0];
        }

        return null;
    }

    /**
     * Installs a theme archive (.zip, .tar.gz, .tar.xz, .tar) to temporary staging or direct target directory.
     * Returns the installed GrubTheme.
     */
    public GrubTheme installThemeArchive(File archiveFile, File targetThemesDir) throws Exception {
        if (!targetThemesDir.exists()) {
            targetThemesDir.mkdirs();
        }

        String fileName = archiveFile.getName().toLowerCase();
        File tempDir = Files.createTempDirectory("grub_theme_extract_").toFile();

        try {
            if (fileName.endsWith(".zip")) {
                unzip(archiveFile, tempDir);
            } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar.xz") || fileName.endsWith(".tar")) {
                untar(archiveFile, tempDir);
            } else {
                throw new IllegalArgumentException("Unsupported archive format: " + archiveFile.getName());
            }

            // Find theme directory in tempDir
            File themeFolder = findThemeFolderInExtracted(tempDir);
            if (themeFolder == null) {
                throw new IllegalArgumentException("No theme.txt found inside the archive: " + archiveFile.getName());
            }

            String themeFolderName = themeFolder.getName();
            File destFolder = new File(targetThemesDir, themeFolderName);

            // Copy contents from themeFolder to destFolder
            copyDirectory(themeFolder.toPath(), destFolder.toPath());

            return parseThemeDir(destFolder);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private File findThemeFolderInExtracted(File dir) {
        File themeTxt = new File(dir, "theme.txt");
        if (themeTxt.exists()) {
            return dir;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findThemeFolderInExtracted(f);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File target = new File(destDir, entry.getName());
                if (!target.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void untar(File tarFile, File destDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xf", tarFile.getAbsolutePath(), "-C", destDir.getAbsolutePath());
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to extract tar archive: " + tarFile.getName());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }
}
