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
 * Preserves existing comments and file formatting when editing key-value pairs.
 */
public class GrubConfigParser {

    // List storing raw lines from file to preserve comments and layout
    private final List<String> lines = new ArrayList<>();
    // Ordered LinkedHashMap storing parsed configuration key-value pairs
    private final LinkedHashMap<String, String> configMap = new LinkedHashMap<>();

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
        // Clear previous raw lines list
        lines.clear();
        // Clear previous key-value map entries
        configMap.clear();

        // Read all lines from file into String list
        List<String> fileLines = Files.readAllLines(file.toPath());
        // Parse lines into internal structures
        parseLines(fileLines);
    }

    /**
     * Loads GRUB configuration lines from a string content buffer.
     * @param content Raw configuration file text
     */
    public void loadFromString(String content) {
        // Clear previous raw lines list
        lines.clear();
        // Clear previous key-value map entries
        configMap.clear();

        // Split string into lines array and convert to list
        List<String> fileLines = Arrays.asList(content.split("\\r?\\n"));
        // Parse lines into internal structures
        parseLines(fileLines);
    }

    /**
     * Internal method to parse lines into configMap while preserving raw lines.
     */
    private void parseLines(List<String> fileLines) {
        // Store raw lines in lines collection
        this.lines.addAll(fileLines);
        // Loop through each line in file
        for (String line : fileLines) {
            // Trim leading and trailing whitespace
            String trimmed = line.trim();
            // Skip empty lines or comment lines starting with #
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                // Continue to next line
                continue;
            }
            // Locate index of equals sign delimiter
            int eqIndex = trimmed.indexOf('=');
            // If equals sign is found after first character
            if (eqIndex > 0) {
                // Extract setting key name
                String key = trimmed.substring(0, eqIndex).trim();
                // Extract setting value string
                String value = trimmed.substring(eqIndex + 1).trim();
                // Strip surrounding quotes for table display
                value = unquote(value);
                // Put key and unquoted value into configMap
                configMap.put(key, value);
            }
        }
    }

    /**
     * Helper method to strip outer double or single quotes from value string.
     */
    private String unquote(String val) {
        // Check if value length is at least 2 characters
        if (val.length() >= 2) {
            // Check if string starts and ends with matching quotes
            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                // Return inner string substring without outer quotes
                return val.substring(1, val.length() - 1);
            }
        }
        // Return original value if unquoted
        return val;
    }

    /**
     * Returns an unmodifiable map of parsed key-value settings.
     * @return Map of setting keys and values
     */
    public Map<String, String> getConfigMap() {
        // Return unmodifiable map wrapper around configMap
        return Collections.unmodifiableMap(configMap);
    }

    /**
     * Gets value string for a setting key or null if absent.
     * @param key Setting key name
     * @return Value string or null
     */
    public String getValue(String key) {
        // Return value from configMap
        return configMap.get(key);
    }

    /**
     * Sets or updates value for a key in both map and raw lines list.
     * @param key Setting key name
     * @param value Setting value string
     */
    public void setValue(String key, String value) {
        // Update or insert key-value pair in configMap
        configMap.put(key, value);
        // Synchronize change into lines list
        updateLinesKey(key, value);
    }

    /**
     * Removes a setting key from both map and raw lines list.
     * @param key Setting key name
     */
    public void removeKey(String key) {
        // Remove key from configMap
        configMap.remove(key);
        // Remove matching line from raw lines list
        removeLineKey(key);
    }

    /**
     * Synchronizes new key-value map changes while keeping comments and line ordering intact.
     * @param newMap Map of new key-value pairs
     */
    public void updateFromMap(Map<String, String> newMap) {
        // List to hold keys present in configMap but missing in newMap
        List<String> keysToRemove = new ArrayList<>();
        // Find deleted keys
        for (String k : configMap.keySet()) {
            // Check if key is absent in new map
            if (!newMap.containsKey(k)) {
                // Add missing key to removal list
                keysToRemove.add(k);
            }
        }
        // Iterate through keys to remove
        for (String k : keysToRemove) {
            // Remove key
            removeKey(k);
        }

        // Add or update entries from newMap
        for (Map.Entry<String, String> entry : newMap.entrySet()) {
            // Set value for key
            setValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates matching line in raw lines list or appends new line if key does not exist.
     */
    private void updateLinesKey(String key, String value) {
        // Format value string with double quotes
        String formattedValue = formatValue(value);
        // Boolean flag tracking whether line was found and updated
        boolean updated = false;

        // Iterate through raw lines list
        for (int i = 0; i < lines.size(); i++) {
            // Trim current line
            String line = lines.get(i).trim();
            // Check if line is a setting line (not a comment and contains '=')
            if (!line.startsWith("#") && line.contains("=")) {
                // Find equals sign index
                int eqIndex = line.indexOf('=');
                // Extract key from line
                String lineKey = line.substring(0, eqIndex).trim();
                // Check if line key matches target key
                if (lineKey.equals(key)) {
                    // Update raw line in list with key=formattedValue
                    lines.set(i, key + "=" + formattedValue);
                    // Set updated flag to true
                    updated = true;
                    // Break loop
                    break;
                }
            }
        }

        // If key line was not found in existing lines
        if (!updated) {
            // Append new line key=formattedValue to end of list
            lines.add(key + "=" + formattedValue);
        }
    }

    /**
     * Removes matching key line from raw lines list.
     */
    private void removeLineKey(String key) {
        // Create iterator over raw lines list
        for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
            // Trim line string
            String line = it.next().trim();
            // Check if line is a setting line
            if (!line.startsWith("#") && line.contains("=")) {
                // Find equals sign index
                int eqIndex = line.indexOf('=');
                // Extract key from line
                String lineKey = line.substring(0, eqIndex).trim();
                // Check if key matches target key
                if (lineKey.equals(key)) {
                    // Remove current line from list
                    it.remove();
                    // Break loop
                    break;
                }
            }
        }
    }

    /**
     * Formats value with double quotes for GRUB config consistency.
     */
    private String formatValue(String val) {
        // Return double quotes if value is null
        if (val == null) return "\"\"";
        // Return as-is if value is already wrapped in quotes
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            // Value is already quoted
            return val;
        }
        // Wrap value in double quotes escaping nested double quotes
        return "\"" + val.replace("\"", "\\\"") + "\"";
    }

    /**
     * Recombines raw lines list into single output configuration string.
     * @return Complete configuration text
     */
    public String generateConfigString() {
        // Create StringBuilder for output string
        StringBuilder sb = new StringBuilder();
        // Append each line followed by platform line separator
        for (String line : lines) {
            // Append line and line separator
            sb.append(line).append(System.lineSeparator());
        }
        // Return built configuration string
        return sb.toString();
    }

    /**
     * Saves generated configuration string to a file on disk.
     * @param file Target File object
     * @throws IOException If writing fails
     */
    public void saveToFile(File file) throws IOException {
        // Write byte contents of config string to file path
        Files.write(file.toPath(), generateConfigString().getBytes());
    }

    /**
     * Returns a copy of raw lines list.
     * @return List of raw line strings
     */
    public List<String> getRawLines() {
        // Return new ArrayList holding raw lines copy
        return new ArrayList<>(lines);
    }
}
