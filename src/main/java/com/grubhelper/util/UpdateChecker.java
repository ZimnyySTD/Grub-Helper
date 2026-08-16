// Package declaration for update utility classes
package com.grubhelper.util;

// Import BufferedReader for reading HTTP network stream lines
import java.io.BufferedReader;
// Import File class for checking local git folder status
import java.io.File;
// Import InputStreamReader for reading network bytes into characters
import java.io.InputStreamReader;
// Import HttpURLConnection for opening HTTP connections
import java.net.HttpURLConnection;
// Import URI for safe URL creation
import java.net.URI;
// Import URL class for network endpoints
import java.net.URL;

/**
 * Utility class for checking remote updates via GitHub Releases API and triggering auto-reinstallations.
 */
public class UpdateChecker {

    /**
     * Data holder class containing update status details.
     */
    public static class UpdateInfo {
        // Boolean indicating if a newer version is available
        public final boolean updateAvailable;
        // String holding latest version string
        public final String latestVersion;
        // String holding changelog text
        public final String changelog;
        // String holding download URL string
        public final String downloadUrl;

        /**
         * Constructor for UpdateInfo.
         */
        public UpdateInfo(boolean updateAvailable, String latestVersion, String changelog, String downloadUrl) {
            // Store update availability boolean
            this.updateAvailable = updateAvailable;
            // Store latest version string
            this.latestVersion = latestVersion;
            // Store changelog string
            this.changelog = changelog;
            // Store download URL string
            this.downloadUrl = downloadUrl;
        }
    }

    /**
     * Checks remote GitHub Releases API endpoint for the latest tag/release.
     * Falls back to raw version.json if no release is found.
     * @return UpdateInfo result object
     */
    public static UpdateInfo checkForUpdates() {
        // Try GitHub Releases API endpoint first
        UpdateInfo releaseInfo = checkGitHubReleasesAPI();
        // If release info was successfully fetched from GitHub Releases API
        if (releaseInfo != null) {
            // Return release update info
            return releaseInfo;
        }

        // Fallback: check version.json raw manifest
        UpdateInfo fallbackInfo = checkFallbackManifest();
        // If fallback info is non-null
        if (fallbackInfo != null) {
            // Return fallback update info
            return fallbackInfo;
        }

        // Return default UpdateInfo indicating no updates found
        return new UpdateInfo(false, Version.CURRENT_VERSION, "No updates found.", "");
    }

    /**
     * Fetches the latest release details from GitHub Releases API endpoint.
     */
    private static UpdateInfo checkGitHubReleasesAPI() {
        try {
            // Create URL instance from GitHub Releases API URL with cache-busting timestamp query parameter
            URL url = URI.create(Version.RELEASES_API_URL + "?t=" + System.currentTimeMillis()).toURL();
            // Open HttpURLConnection to remote endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // Set connection timeout to 4000ms
            conn.setConnectTimeout(4000);
            // Set read timeout to 4000ms
            conn.setReadTimeout(4000);
            // Set HTTP method to GET
            conn.setRequestMethod("GET");
            // Set User-Agent header required by GitHub API
            conn.setRequestProperty("User-Agent", "Grub-Helper/" + Version.CURRENT_VERSION);
            // Set Accept header for GitHub REST API v3
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            // Set Cache-Control header to bypass CDN caching
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");

            // Check if GitHub returned 200 OK
            if (conn.getResponseCode() == 200) {
                // Open BufferedReader on stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                // Create StringBuilder to collect JSON response
                StringBuilder sb = new StringBuilder();
                // Declare line string
                String line;
                // Read response lines
                while ((line = reader.readLine()) != null) {
                    // Append line
                    sb.append(line);
                }
                // Close reader
                reader.close();

                // Convert JSON string
                String json = sb.toString();
                // Extract "tag_name" property
                String tagName = extractJsonValue(json, "tag_name");
                // If tag_name is empty, try "name"
                if (tagName.isEmpty()) {
                    tagName = extractJsonValue(json, "name");
                }
                // Strip leading 'v' or 'V' character if present (e.g. v1.0.1 -> 1.0.1)
                String latestVer = tagName.replaceAll("^[vV]", "").trim();
                // Extract "body" property for changelog
                String changelog = extractJsonValue(json, "body");
                // Extract "html_url" for download link
                String htmlUrl = extractJsonValue(json, "html_url");

                // Check if extracted version string is non-empty
                if (!latestVer.isEmpty()) {
                    // Compare current version against release tag version
                    boolean available = isNewerVersion(Version.CURRENT_VERSION, latestVer);
                    // Return UpdateInfo object
                    return new UpdateInfo(available, latestVer, changelog, htmlUrl);
                }
            }
        } catch (Exception ignored) {
            // Catch network or API parsing errors gracefully
        }
        // Return null if GitHub Releases API query failed or returned no releases
        return null;
    }

    /**
     * Fallback method fetching version.json raw manifest.
     */
    private static UpdateInfo checkFallbackManifest() {
        try {
            // Create URL instance from fallback manifest URL with timestamp query parameter
            URL url = URI.create(Version.UPDATE_URL_FALLBACK + "?t=" + System.currentTimeMillis()).toURL();
            // Open HttpURLConnection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // Set connection timeout
            conn.setConnectTimeout(4000);
            // Set read timeout
            conn.setReadTimeout(4000);
            // Set HTTP method to GET
            conn.setRequestMethod("GET");
            // Set User-Agent header
            conn.setRequestProperty("User-Agent", "Grub-Helper/" + Version.CURRENT_VERSION);
            // Set Cache-Control header
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");

            // Check response status
            if (conn.getResponseCode() == 200) {
                // Open reader
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                // Create StringBuilder
                StringBuilder sb = new StringBuilder();
                // Declare line variable
                String line;
                // Read lines
                while ((line = reader.readLine()) != null) {
                    // Append line
                    sb.append(line);
                }
                // Close reader
                reader.close();

                // Convert JSON string
                String json = sb.toString();
                // Extract "version" string
                String latestVer = extractJsonValue(json, "version").replaceAll("^[vV]", "").trim();
                // Extract "changelog" string
                String changelog = extractJsonValue(json, "changelog");
                // Extract "downloadUrl" string
                String downloadUrl = extractJsonValue(json, "downloadUrl");

                // Check if version string is non-empty
                if (!latestVer.isEmpty()) {
                    // Compare version
                    boolean available = isNewerVersion(Version.CURRENT_VERSION, latestVer);
                    // Return UpdateInfo object
                    return new UpdateInfo(available, latestVer, changelog, downloadUrl);
                }
            }
        } catch (Exception ignored) {
            // Ignore exception
        }
        // Return null if fallback manifest check failed
        return null;
    }

    /**
     * Lightweight helper method to extract JSON string property values without external dependencies.
     */
    private static String extractJsonValue(String json, String key) {
        // Construct key match pattern
        String pattern = "\"" + key + "\":";
        // Find index of pattern in json string
        int idx = json.indexOf(pattern);
        // Return empty string if key not found
        if (idx == -1) return "";
        // Find opening quote index after pattern
        int start = json.indexOf("\"", idx + pattern.length());
        // Return empty string if start quote not found
        if (start == -1) return "";
        // Find closing quote index
        int end = json.indexOf("\"", start + 1);
        // Return empty string if end quote not found
        if (end == -1) return "";
        // Extract substring between quotes
        return json.substring(start + 1, end);
    }

    /**
     * Compares version strings using semantic versioning rules (vX.Y.Z).
     * @param current Current version string
     * @param latest Latest version string
     * @return true if latest is strictly newer than current
     */
    public static boolean isNewerVersion(String current, String latest) {
        // Return false if latest version string is null or empty
        if (latest == null || latest.trim().isEmpty()) return false;
        // Clean leading v characters
        current = current.replaceAll("^[vV]", "").trim();
        latest = latest.replaceAll("^[vV]", "").trim();

        // Split current version string by dot delimiter
        String[] cParts = current.split("\\.");
        // Split latest version string by dot delimiter
        String[] lParts = latest.split("\\.");
        // Get maximum segment length
        int length = Math.max(cParts.length, lParts.length);

        // Iterate through version segments
        for (int i = 0; i < length; i++) {
            // Parse current version segment integer or 0
            int cVal = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")) : 0;
            // Parse latest version segment integer or 0
            int lVal = i < lParts.length ? Integer.parseInt(lParts[i].replaceAll("[^0-9]", "")) : 0;
            // If latest value is greater, return true
            if (lVal > cVal) return true;
            // If latest value is smaller, return false
            if (lVal < cVal) return false;
        }
        // Versions are identical, return false
        return false;
    }

    /**
     * Executes auto update in a dedicated temporary staging directory to pull latest release source code and run install.sh as root.
     */
    public static RootExecutor.CommandResult performAutoUpdate() throws Exception {
        // Create script that clones or fetches latest release in temporary folder and executes install.sh as root
        String updateScript = "TEMP_DIR=$(mktemp -d /tmp/grub_helper_update_XXXXXX) && " +
                              "git clone " + Version.REPO_URL + " \"$TEMP_DIR\" && " +
                              "cd \"$TEMP_DIR\" && " +
                              "chmod +x install.sh && " +
                              "./install.sh && " +
                              "rm -rf \"$TEMP_DIR\" /tmp/grub_theme_extract_* /tmp/grub_themes_staging";

        // Execute update script as root via pkexec or sudo
        return RootExecutor.runAsRoot(updateScript);
    }

    /**
     * Relaunches the application using the installed system command 'grub-helper' or java binary and exits current JVM process.
     */
    public static void restartApplication() {
        try {
            // Check if installed grub-helper binary exists in PATH
            ProcessBuilder pb = new ProcessBuilder("grub-helper");
            // Start new process
            pb.start();
        } catch (Exception e) {
            try {
                // Fallback: run system jar directly
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", "/usr/share/java/grub-helper/grub-helper.jar");
                // Start fallback process
                pb.start();
            } catch (Exception ignored) {}
        }
        // Exit current Java virtual machine process
        System.exit(0);
    }
}
