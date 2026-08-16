// Declare the package location for this class file within the com.grubhelper.model hierarchy
package com.grubhelper.model;

// Import the File class for representing file paths on disk
import java.io.File;

/**
 * Represents a GRUB theme found on the system.
 */
public class GrubTheme {

    // Final string field storing the display name of the theme
    private final String name;
    // Final File object storing the directory location of the theme folder
    private final File folder;
    // Final File object storing the path to the main theme.txt configuration file
    private final File themeTxtFile;
    // Final File object storing the path to the preview image if found
    private final File previewImage;

    /**
     * Constructor for creating a new GrubTheme object.
     * @param name The name of the theme
     * @param folder The directory containing the theme
     * @param themeTxtFile The theme.txt configuration file
     * @param previewImage The preview image file for the theme
     */
    public GrubTheme(String name, File folder, File themeTxtFile, File previewImage) {
        // Assign the theme name to the instance variable
        this.name = name;
        // Assign the folder File object to the instance variable
        this.folder = folder;
        // Assign the themeTxtFile object to the instance variable
        this.themeTxtFile = themeTxtFile;
        // Assign the previewImage File object to the instance variable
        this.previewImage = previewImage;
    }

    /**
     * Returns the name of the theme.
     * @return Theme name string
     */
    public String getName() {
        // Return the stored theme name
        return name;
    }

    /**
     * Returns the folder directory of the theme.
     * @return Theme folder File object
     */
    public File getFolder() {
        // Return the theme directory File object
        return folder;
    }

    /**
     * Returns the theme.txt configuration file.
     * @return theme.txt File object
     */
    public File getThemeTxtFile() {
        // Return the theme.txt File object
        return themeTxtFile;
    }

    /**
     * Returns the preview image file for the theme.
     * @return Preview image File object
     */
    public File getPreviewImage() {
        // Return the preview image File object
        return previewImage;
    }

    /**
     * Returns the absolute path string to theme.txt or null if themeTxtFile is null.
     * @return Absolute path string
     */
    public String getThemeTxtPath() {
        // Check if themeTxtFile is not null before retrieving its absolute path
        return themeTxtFile != null ? themeTxtFile.getAbsolutePath() : null;
    }

    /**
     * Overrides toString to display the theme name in UI components like JList.
     * @return Theme name string
     */
    @Override
    public String toString() {
        // Return the theme name for display
        return name;
    }
}
