package com.grubhelper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    public static class UpdateInfo {
        public final boolean updateAvailable;
        public final String latestVersion;
        public final String changelog;
        public final String downloadUrl;

        public UpdateInfo(boolean updateAvailable, String latestVersion, String changelog, String downloadUrl) {
            this.updateAvailable = updateAvailable;
            this.latestVersion = latestVersion;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
        }
    }

    public static UpdateInfo checkForUpdates() {
        try {
            // Check via git if cloned repo
            File gitDir = new File(".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                ProcessBuilder pb = new ProcessBuilder("git", "fetch", "--dry-run");
                Process p = pb.start();
                p.waitFor();
            }

            URL url = URI.create(Version.UPDATE_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String json = sb.toString();
                String latestVer = extractJsonValue(json, "version");
                String changelog = extractJsonValue(json, "changelog");
                String downloadUrl = extractJsonValue(json, "downloadUrl");

                boolean available = isNewerVersion(Version.CURRENT_VERSION, latestVer);
                return new UpdateInfo(available, latestVer, changelog, downloadUrl);
            }
        } catch (Exception ignored) {
            // Offline or repository manifest not reachable
        }

        return new UpdateInfo(false, Version.CURRENT_VERSION, "No updates found.", "");
    }

    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + pattern.length());
        if (start == -1) return "";
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }

    public static boolean isNewerVersion(String current, String latest) {
        if (latest == null || latest.trim().isEmpty()) return false;
        String[] cParts = current.split("\\.");
        String[] lParts = latest.split("\\.");
        int length = Math.max(cParts.length, lParts.length);

        for (int i = 0; i < length; i++) {
            int cVal = i < cParts.length ? Integer.parseInt(cParts[i].replaceAll("[^0-9]", "")) : 0;
            int lVal = i < lParts.length ? Integer.parseInt(lParts[i].replaceAll("[^0-9]", "")) : 0;
            if (lVal > cVal) return true;
            if (lVal < cVal) return false;
        }
        return false;
    }

    public static RootExecutor.CommandResult performAutoUpdate() throws Exception {
        String updateScript = "git pull origin main || git pull; chmod +x install.sh; ./install.sh";
        return RootExecutor.runAsRoot(updateScript);
    }
}
