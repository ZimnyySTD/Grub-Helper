package com.grubhelper.model;

import java.io.File;

/**
 * Detects system GRUB directories, theme paths, and update commands across various Linux distributions.
 */
public class GrubEnvironment {

    private String grubConfigPath = "/etc/default/grub";
    private String grubThemesDir = "/boot/grub/themes";
    private String grubBootDir = "/boot/grub";
    private String updateGrubCommand = "update-grub";

    public GrubEnvironment() {
        detectEnvironment();
    }

    public void detectEnvironment() {
        // Detect GRUB directory (/boot/grub vs /boot/grub2)
        File grub2Themes = new File("/boot/grub2/themes");
        File grub2Dir = new File("/boot/grub2");
        File grubThemes = new File("/boot/grub/themes");

        if (grub2Themes.exists() || grub2Dir.exists()) {
            grubThemesDir = "/boot/grub2/themes";
            grubBootDir = "/boot/grub2";
        } else if (grubThemes.exists()) {
            grubThemesDir = "/boot/grub/themes";
            grubBootDir = "/boot/grub";
        } else {
            // Default fallback based on /boot/grub2 vs /boot/grub
            if (new File("/boot/grub2").exists()) {
                grubThemesDir = "/boot/grub2/themes";
                grubBootDir = "/boot/grub2";
            } else {
                grubThemesDir = "/boot/grub/themes";
                grubBootDir = "/boot/grub";
            }
        }

        // Detect update-grub vs grub-mkconfig / grub2-mkconfig
        if (isExecutableInPath("update-grub")) {
            updateGrubCommand = "update-grub";
        } else if (isExecutableInPath("grub-mkconfig")) {
            String targetCfg = grubBootDir + "/grub.cfg";
            if (!new File(targetCfg).exists() && new File("/boot/efi/EFI/fedora/grub.cfg").exists()) {
                targetCfg = "/boot/efi/EFI/fedora/grub.cfg";
            }
            updateGrubCommand = "grub-mkconfig -o " + targetCfg;
        } else if (isExecutableInPath("grub2-mkconfig")) {
            String targetCfg = grubBootDir + "/grub.cfg";
            if (!new File(targetCfg).exists() && new File("/boot/efi/EFI/fedora/grub.cfg").exists()) {
                targetCfg = "/boot/efi/EFI/fedora/grub.cfg";
            }
            updateGrubCommand = "grub2-mkconfig -o " + targetCfg;
        }
    }

    private boolean isExecutableInPath(String cmd) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;
        String[] paths = pathEnv.split(File.pathSeparator);
        for (String path : paths) {
            File file = new File(path, cmd);
            if (file.exists() && file.canExecute()) {
                return true;
            }
        }
        return false;
    }

    public String getGrubConfigPath() {
        return grubConfigPath;
    }

    public String getGrubThemesDir() {
        return grubThemesDir;
    }

    public String getGrubBootDir() {
        return grubBootDir;
    }

    public String getUpdateGrubCommand() {
        return updateGrubCommand;
    }
}
