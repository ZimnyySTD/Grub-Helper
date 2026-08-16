// Package declaration for GRUB environment detector
package com.grubhelper.model;

// Import File class for filesystem operations
import java.io.File;

/**
 * Detects system GRUB directories, theme paths, and update commands across various Linux distributions.
 */
public class GrubEnvironment {

    // Default configuration file path for GRUB
    private String grubConfigPath = "/etc/default/grub";
    // Default theme directory location
    private String grubThemesDir = "/boot/grub/themes";
    // Default GRUB boot directory location
    private String grubBootDir = "/boot/grub";
    // Default command name to update GRUB configuration
    private String updateGrubCommand = "update-grub";

    /**
     * Constructor initializes environment detection on instantiation.
     */
    public GrubEnvironment() {
        // Execute environment detection logic
        detectEnvironment();
    }

    /**
     * Inspects system directories and PATH binaries to determine GRUB configuration paths and update commands.
     */
    public void detectEnvironment() {
        // Create File reference to check for /boot/grub2/themes (used on Fedora/RHEL/SUSE)
        File grub2Themes = new File("/boot/grub2/themes");
        // Create File reference to check for /boot/grub2 directory
        File grub2Dir = new File("/boot/grub2");
        // Create File reference to check for /boot/grub/themes directory
        File grubThemes = new File("/boot/grub/themes");

        // If grub2 themes or boot directory exists, update theme and boot directories to /boot/grub2
        if (grub2Themes.exists() || grub2Dir.exists()) {
            // Set themes directory to /boot/grub2/themes
            grubThemesDir = "/boot/grub2/themes";
            // Set boot directory to /boot/grub2
            grubBootDir = "/boot/grub2";
        // Else if /boot/grub/themes exists, use standard /boot/grub paths
        } else if (grubThemes.exists()) {
            // Set themes directory to /boot/grub/themes
            grubThemesDir = "/boot/grub/themes";
            // Set boot directory to /boot/grub
            grubBootDir = "/boot/grub";
        } else {
            // Check fallback for /boot/grub2 vs /boot/grub
            if (new File("/boot/grub2").exists()) {
                // Set themes directory to /boot/grub2/themes
                grubThemesDir = "/boot/grub2/themes";
                // Set boot directory to /boot/grub2
                grubBootDir = "/boot/grub2";
            } else {
                // Set default themes directory to /boot/grub/themes
                grubThemesDir = "/boot/grub/themes";
                // Set default boot directory to /boot/grub
                grubBootDir = "/boot/grub";
            }
        }

        // Check if update-grub executable is in PATH (Debian/Ubuntu)
        if (isExecutableInPath("update-grub")) {
            // Assign updateGrubCommand to "update-grub"
            updateGrubCommand = "update-grub";
        // Check if grub-mkconfig executable is in PATH (Arch Linux)
        } else if (isExecutableInPath("grub-mkconfig")) {
            // Construct target grub.cfg path
            String targetCfg = grubBootDir + "/grub.cfg";
            // If target does not exist and Fedora EFI path exists, update target file path
            if (!new File(targetCfg).exists() && new File("/boot/efi/EFI/fedora/grub.cfg").exists()) {
                // Set target config to Fedora EFI path
                targetCfg = "/boot/efi/EFI/fedora/grub.cfg";
            }
            // Set update command to grub-mkconfig -o <path>
            updateGrubCommand = "grub-mkconfig -o " + targetCfg;
        // Check if grub2-mkconfig executable is in PATH (Fedora/RHEL/CentOS)
        } else if (isExecutableInPath("grub2-mkconfig")) {
            // Construct target grub.cfg path
            String targetCfg = grubBootDir + "/grub.cfg";
            // If target does not exist and Fedora EFI path exists, update target file path
            if (!new File(targetCfg).exists() && new File("/boot/efi/EFI/fedora/grub.cfg").exists()) {
                // Set target config to Fedora EFI path
                targetCfg = "/boot/efi/EFI/fedora/grub.cfg";
            }
            // Set update command to grub2-mkconfig -o <path>
            updateGrubCommand = "grub2-mkconfig -o " + targetCfg;
        }
    }

    /**
     * Helper method to check if a command exists and is executable in System PATH directories.
     */
    private boolean isExecutableInPath(String cmd) {
        // Get the system PATH environment variable string
        String pathEnv = System.getenv("PATH");
        // If PATH is null, return false
        if (pathEnv == null) return false;
        // Split PATH string into individual directory paths using the path separator
        String[] paths = pathEnv.split(File.pathSeparator);
        // Loop through each directory in PATH
        for (String path : paths) {
            // Create File object for command within current path directory
            File file = new File(path, cmd);
            // If file exists and can be executed, return true
            if (file.exists() && file.canExecute()) {
                // Command found in PATH
                return true;
            }
        }
        // Command not found in PATH
        return false;
    }

    /**
     * Gets the path to /etc/default/grub file.
     */
    public String getGrubConfigPath() {
        // Return grubConfigPath string
        return grubConfigPath;
    }

    /**
     * Gets the path to GRUB themes directory.
     */
    public String getGrubThemesDir() {
        // Return grubThemesDir string
        return grubThemesDir;
    }

    /**
     * Gets the GRUB boot directory path.
     */
    public String getGrubBootDir() {
        // Return grubBootDir string
        return grubBootDir;
    }

    /**
     * Gets the update GRUB shell command.
     */
    public String getUpdateGrubCommand() {
        // Return updateGrubCommand string
        return updateGrubCommand;
    }
}
