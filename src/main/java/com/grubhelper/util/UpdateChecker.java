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
 * Utility class for checking remote updates and triggering auto-reinstallations.
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
     * Checks remote GitHub version manifest to see if a newer version exists.
     * Checks both master and main branches, and includes cache-busting headers.
     * @return UpdateInfo result object
     */
    public static UpdateInfo checkForUpdates() {
        // Try master branch URL first, then fallback to main branch URL
        String[] urls = new String[]{
            Version.UPDATE_URL + "?t=" + System.currentTimeMillis(),
            Version.UPDATE_URL_FALLBACK + "?t=" + System.currentTimeMillis()
        };

        for (String urlStr : urls) {
            UpdateInfo info = fetchManifestFromUrl(urlStr);
            if (info != null) {
                return info;
            }
        }

        // Return fallback UpdateInfo indicating no updates found
        return new UpdateInfo(false, Version.CURRENT_VERSION, "No updates found.", "");
    }

    /**
     * Helper method to fetch and parse JSON manifest from a single URL.
     */
    private static UpdateInfo fetchManifestFromUrl(String urlStr) {
        try {
            // Create URL instance from string
            URL url = URI.create(urlStr).toURL();
            // Open HttpURLConnection to remote URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // Set connection timeout to 4000ms
            conn.setConnectTimeout(4000);
            // Set read timeout to 4000ms
            conn.setReadTimeout(4000);
            // Set HTTP method to GET
            conn.setRequestMethod("GET");
            // Set User-Agent header
            conn.setRequestProperty("User-Agent", "Grub-Helper/" + Version.CURRENT_VERSION);
            // Set Cache-Control header to prevent raw.githubusercontent CDN caching stale version.json
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            // Set Pragma header for HTTP 1.0 proxies
            conn.setRequestProperty("Pragma", "no-cache");

            // Check if server returned 200 OK response
            if (conn.getResponseCode() == 200) {
                // Open BufferedReader on connection input stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                // Create StringBuilder to collect JSON response string
                StringBuilder sb = new StringBuilder();
                // Declare line variable
                String line;
                // Read lines from reader until EOF
                while ((line = reader.readLine()) != null) {
                    // Append line to StringBuilder
                    sb.append(line);
                }
                // Close reader
                reader.close();

                // Convert StringBuilder to json String
                String json = sb.toString();
                // Extract "version" value from json string
                String latestVer = extractJsonValue(json, "version");
                // Extract "changelog" value from json string
                String changelog = extractJsonValue(json, "changelog");
                // Extract "downloadUrl" value from json string
                String downloadUrl = extractJsonValue(json, "downloadUrl");

                if (!latestVer.isEmpty()) {
                    // Compare current version against remote version
                    boolean available = isNewerVersion(Version.CURRENT_VERSION, latestVer);
                    // Return new UpdateInfo instance
                    return new UpdateInfo(available, latestVer, changelog, downloadUrl);
                }
            }
        } catch (Exception ignored) {
            // Catch connection or parsing errors gracefully
        }
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
     * Executes git pull and install.sh script as root to perform auto update.
     */
    public static RootExecutor.CommandResult performAutoUpdate() throws Exception {
        // Formulate update shell script string
        String updateScript = "git pull origin master || git pull origin main || git pull; chmod +x install.sh; ./install.sh";
        // Execute update script as root
        return RootExecutor.runAsRoot(updateScript);
    }
}
