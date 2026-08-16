// Package declaration for GRUB configuration parser model
package com.grubhelper.model;

// Import File class for file operations
import java.io.File;
// Import IOException for input/output exceptions
import java.io.IOException;
// Import Files class for NIO file reading/writing
import java.nio.file.Files;
// Import ArrayList for raw lines storage
import java.util.ArrayList;
// Import Arrays utility class
import java.util.Arrays;
// Import Collections utility class
import java.util.Collections;
// Import Iterator for list iteration and removal
import java.util.Iterator;
// Import LinkedHashMap for ordered key-value map
import java.util.LinkedHashMap;
// Import List interface
import java.util.List;
// Import Map interface
import java.util.Map;

/**
 * Handles reading, parsing, and modifying /etc/default/grub configuration file.
 * Preserves existing comments and file formatting while tracking enabled/disabled (# commented) state for every option.
 */
public class GrubConfigParser {

    /**
     * Inner class representing a single GRUB configuration entry, including its enablement state.
     */
    public static class ConfigItem {
        private String key;
        private String value;
        private boolean enabled;

        public ConfigItem(String key, String value, boolean enabled) {
            this.key = key;
            this.value = value;
            this.enabled = enabled;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // List storing raw lines from file to preserve comments and layout
    private final List<String> lines = new ArrayList<>();
    // Ordered LinkedHashMap storing parsed configuration items (including commented ones)
    private final LinkedHashMap<String, ConfigItem> configItems = new LinkedHashMap<>();

    /**
     * Default constructor for GrubConfigParser.
     */
    public GrubConfigParser() {}

    /**
     * Loads GRUB configuration lines from a File object on disk.
     * @param file File object representing /etc/default/grub
     * @throws IOException If reading fails
     */
    public void loadFromFile(File file) throws IOException {
        lines.clear();
        configItems.clear();

        List<String> fileLines = Files.readAllLines(file.toPath());
        parseLines(fileLines);
    }

    /**
     * Loads GRUB configuration lines from a string content buffer.
     * @param content Raw configuration file text
     */
    public void loadFromString(String content) {
        lines.clear();
        configItems.clear();

        List<String> fileLines = Arrays.asList(content.split("\\r?\\n"));
        parseLines(fileLines);
    }

    /**
     * Internal method to parse lines into configItems while preserving raw lines.
     */
    private void parseLines(List<String> fileLines) {
        this.lines.addAll(fileLines);
        for (String line : fileLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            boolean enabled = !trimmed.startsWith("#");
            String lineToParse = enabled ? trimmed : trimmed.substring(1).trim();

            int eqIndex = lineToParse.indexOf('=');
            if (eqIndex > 0) {
                String key = lineToParse.substring(0, eqIndex).trim();
                String value = lineToParse.substring(eqIndex + 1).trim();
                value = unquote(value);

                // Only store valid GRUB_... or alphanumeric setting key names
                if (key.startsWith("GRUB_") || key.matches("^[A-Za-z0-9_]+$")) {
                    configItems.put(key, new ConfigItem(key, value, enabled));
                }
            }
        }
    }

    /**
     * Helper method to strip outer double or single quotes from value string.
     */
    private String unquote(String val) {
        if (val.length() >= 2) {
            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                return val.substring(1, val.length() - 1);
            }
        }
        return val;
    }

    /**
     * Returns an unmodifiable map of enabled key-value settings.
     * @return Map of setting keys and values
     */
    public Map<String, String> getConfigMap() {
        LinkedHashMap<String, String> enabledMap = new LinkedHashMap<>();
        for (Map.Entry<String, ConfigItem> entry : configItems.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabledMap.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        return Collections.unmodifiableMap(enabledMap);
    }

    /**
     * Returns all configuration items including commented-out ones.
     */
    public Map<String, ConfigItem> getAllConfigItems() {
        return Collections.unmodifiableMap(configItems);
    }

    /**
     * Gets value string for an enabled setting key or null if absent/disabled.
     * @param key Setting key name
     * @return Value string or null
     */
    public String getValue(String key) {
        ConfigItem item = configItems.get(key);
        return (item != null && item.isEnabled()) ? item.getValue() : null;
    }

    /**
     * Sets or updates value for a key in both map and raw lines list, ensuring it is enabled.
     * @param key Setting key name
     * @param value Setting value string
     */
    public void setValue(String key, String value) {
        setItem(key, value, true);
    }

    /**
     * Sets key, value, and enabled state in both item map and raw lines list.
     */
    public void setItem(String key, String value, boolean enabled) {
        ConfigItem item = configItems.get(key);
        if (item == null) {
            item = new ConfigItem(key, value, enabled);
            configItems.put(key, item);
        } else {
            item.setValue(value);
            item.setEnabled(enabled);
        }
        updateLinesKey(key, value, enabled);
    }

    /**
     * Removes a setting key from both map and raw lines list.
     * @param key Setting key name
     */
    public void removeKey(String key) {
        configItems.remove(key);
        removeLineKey(key);
    }

    /**
     * Updates config items from a list of ConfigItems, updating line enablement state.
     */
    public void updateFromItems(List<ConfigItem> items) {
        LinkedHashMap<String, ConfigItem> newMap = new LinkedHashMap<>();
        for (ConfigItem item : items) {
            newMap.put(item.getKey(), item);
        }

        List<String> keysToRemove = new ArrayList<>();
        for (String k : configItems.keySet()) {
            if (!newMap.containsKey(k)) {
                keysToRemove.add(k);
            }
        }
        for (String k : keysToRemove) {
            removeKey(k);
        }

        for (ConfigItem item : items) {
            setItem(item.getKey(), item.getValue(), item.isEnabled());
        }
    }

    /**
     * Legacy helper updating map from plain key-value map.
     */
    public void updateFromMap(Map<String, String> newMap) {
        List<String> keysToRemove = new ArrayList<>();
        for (String k : configItems.keySet()) {
            if (!newMap.containsKey(k)) {
                keysToRemove.add(k);
            }
        }
        for (String k : keysToRemove) {
            removeKey(k);
        }

        for (Map.Entry<String, String> entry : newMap.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates matching line in raw lines list (adding/removing leading #) or appends new line if key does not exist.
     */
    private void updateLinesKey(String key, String value, boolean enabled) {
        String formattedValue = formatValue(value);
        String newLineString = (enabled ? "" : "# ") + key + "=" + formattedValue;
        boolean updated = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            String lineToParse = line.startsWith("#") ? line.substring(1).trim() : line;

            if (lineToParse.contains("=")) {
                int eqIndex = lineToParse.indexOf('=');
                String lineKey = lineToParse.substring(0, eqIndex).trim();
                if (lineKey.equals(key)) {
                    lines.set(i, newLineString);
                    updated = true;
                    break;
                }
            }
        }

        if (!updated) {
            lines.add(newLineString);
        }
    }

    /**
     * Removes matching key line from raw lines list.
     */
    private void removeLineKey(String key) {
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
            String line = it.next().trim();
            String lineToParse = line.startsWith("#") ? line.substring(1).trim() : line;
            if (lineToParse.contains("=")) {
                int eqIndex = lineToParse.indexOf('=');
                String lineKey = lineToParse.substring(0, eqIndex).trim();
                if (lineKey.equals(key)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Formats value with double quotes for GRUB config consistency.
     */
    private String formatValue(String val) {
        if (val == null) return "\"\"";
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            return val;
        }
        return "\"" + val.replace("\"", "\\\"") + "\"";
    }

    /**
     * Recombines raw lines list into single output configuration string.
     * @return Complete configuration text
     */
    public String generateConfigString() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Saves generated configuration string to a file on disk.
     * @param file Target File object
     * @throws IOException If writing fails
     */
    public void saveToFile(File file) throws IOException {
        Files.write(file.toPath(), generateConfigString().getBytes());
    }

    /**
     * Returns a copy of raw lines list.
     * @return List of raw line strings
     */
    public List<String> getRawLines() {
        return new ArrayList<>(lines);
    }
}
