package com.grubhelper.model;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Handles reading, parsing, and modifying /etc/default/grub configuration file.
 * Preserves existing comments and file formatting when editing key-value pairs.
 */
public class GrubConfigParser {

    private final List<String> lines = new ArrayList<>();
    private final LinkedHashMap<String, String> configMap = new LinkedHashMap<>();

    public GrubConfigParser() {}

    public void loadFromFile(File file) throws IOException {
        lines.clear();
        configMap.clear();

        List<String> fileLines = Files.readAllLines(file.toPath());
        parseLines(fileLines);
    }

    public void loadFromString(String content) {
        lines.clear();
        configMap.clear();

        List<String> fileLines = Arrays.asList(content.split("\\r?\\n"));
        parseLines(fileLines);
    }

    private void parseLines(List<String> fileLines) {
        this.lines.addAll(fileLines);
        for (String line : fileLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex > 0) {
                String key = trimmed.substring(0, eqIndex).trim();
                String value = trimmed.substring(eqIndex + 1).trim();
                // strip leading/trailing quotes if present for display in table
                value = unquote(value);
                configMap.put(key, value);
            }
        }
    }

    private String unquote(String val) {
        if (val.length() >= 2) {
            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                return val.substring(1, val.length() - 1);
            }
        }
        return val;
    }

    public Map<String, String> getConfigMap() {
        return Collections.unmodifiableMap(configMap);
    }

    public String getValue(String key) {
        return configMap.get(key);
    }

    public void setValue(String key, String value) {
        configMap.put(key, value);
        updateLinesKey(key, value);
    }

    public void removeKey(String key) {
        configMap.remove(key);
        removeLineKey(key);
    }

    /**
     * Updates the config map and syncs lines from a table key-value map while preserving original non-key comments/formatting.
     */
    public void updateFromMap(Map<String, String> newMap) {
        // Find keys removed in newMap
        List<String> keysToRemove = new ArrayList<>();
        for (String k : configMap.keySet()) {
            if (!newMap.containsKey(k)) {
                keysToRemove.add(k);
            }
        }
        for (String k : keysToRemove) {
            removeKey(k);
        }

        // Add or update keys from newMap
        for (Map.Entry<String, String> entry : newMap.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    private void updateLinesKey(String key, String value) {
        String formattedValue = formatValue(value);
        boolean updated = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.startsWith("#") && line.contains("=")) {
                int eqIndex = line.indexOf('=');
                String lineKey = line.substring(0, eqIndex).trim();
                if (lineKey.equals(key)) {
                    lines.set(i, key + "=" + formattedValue);
                    updated = true;
                    break;
                }
            }
        }

        if (!updated) {
            lines.add(key + "=" + formattedValue);
        }
    }

    private void removeLineKey(String key) {
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
            String line = it.next().trim();
            if (!line.startsWith("#") && line.contains("=")) {
                int eqIndex = line.indexOf('=');
                String lineKey = line.substring(0, eqIndex).trim();
                if (lineKey.equals(key)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    private String formatValue(String val) {
        if (val == null) return "\"\"";
        // If already quoted properly, keep as is
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            return val;
        }
        // Wrap values in double quotes for GRUB config consistency
        return "\"" + val.replace("\"", "\\\"") + "\"";
    }

    public String generateConfigString() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void saveToFile(File file) throws IOException {
        Files.write(file.toPath(), generateConfigString().getBytes());
    }

    public List<String> getRawLines() {
        return new ArrayList<>(lines);
    }
}
