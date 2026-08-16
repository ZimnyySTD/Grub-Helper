package com.grubhelper.model;

import java.io.File;

/**
 * Represents a GRUB theme found on the system.
 */
public class GrubTheme {

    private final String name;
    private final File folder;
    private final File themeTxtFile;
    private final File previewImage;

    public GrubTheme(String name, File folder, File themeTxtFile, File previewImage) {
        this.name = name;
        this.folder = folder;
        this.themeTxtFile = themeTxtFile;
        this.previewImage = previewImage;
    }

    public String getName() {
        return name;
    }

    public File getFolder() {
        return folder;
    }

    public File getThemeTxtFile() {
        return themeTxtFile;
    }

    public File getPreviewImage() {
        return previewImage;
    }

    public String getThemeTxtPath() {
        return themeTxtFile != null ? themeTxtFile.getAbsolutePath() : null;
    }

    @Override
    public String toString() {
        return name;
    }
}
