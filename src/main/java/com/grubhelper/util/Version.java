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
    public static final String CURRENT_VERSION = "1.0.0";

    // Official GitHub repository URL
    public static final String REPO_URL = "https://github.com/ZimnyySTD/Grub-Helper/";

    // Primary raw GitHub URL for checking remote version manifest updates (master branch)
    public static final String UPDATE_URL = "https://raw.githubusercontent.com/ZimnyySTD/Grub-Helper/master/version.json";

    // Fallback raw GitHub URL for checking remote version manifest updates (main branch)
    public static final String UPDATE_URL_FALLBACK = "https://raw.githubusercontent.com/ZimnyySTD/Grub-Helper/main/version.json";
}
