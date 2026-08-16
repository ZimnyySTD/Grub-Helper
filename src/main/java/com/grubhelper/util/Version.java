// Package location for version tracking utility
package com.grubhelper.util;

/**
 * Utility class holding application version constants, repository URL, and semantic versioning rules.
 * Semantic Versioning (vX.Y.Z):
 * - X (Major): Significant structural changes, full GUI redesigns, or major platform updates.
 * - Y (Minor): New features, new capabilities, or new integrations.
 * - Z (Patch): Small tweaks, bug fixes, performance improvements, or minor style changes.
 */
public class Version {
    // Current release version string adhering to vX.Y.Z semantic versioning
    public static final String CURRENT_VERSION = "1.1.3";

    // Official GitHub repository clone URL
    public static final String REPO_URL = "https://github.com/ZimnyySTD/Grub-Helper.git";

    // Primary GitHub API endpoint for checking the latest release
    public static final String RELEASES_API_URL = "https://api.github.com/repos/ZimnyySTD/Grub-Helper/releases/latest";

    // Fallback GitHub raw version.json manifest URL
    public static final String UPDATE_URL_FALLBACK = "https://raw.githubusercontent.com/ZimnyySTD/Grub-Helper/master/version.json";
}
