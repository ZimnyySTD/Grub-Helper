// Package declaration for root privilege utility execution
package com.grubhelper.util;

// Import ByteArrayOutputStream for converting InputStream content to string
import java.io.ByteArrayOutputStream;
// Import File class for checking command paths
import java.io.File;
// Import IOException for handling stream read errors
import java.io.IOException;
// Import InputStream for reading process stdout/stderr
import java.io.InputStream;
// Import StandardCharsets for UTF-8 decoding
import java.nio.charset.StandardCharsets;

/**
 * Handles executing commands with root privileges via pkexec or sudo.
 */
public class RootExecutor {

    /**
     * Inner class representing the execution result of a root shell command.
     */
    public static class CommandResult {
        // Integer exit status returned by the process
        public final int exitCode;
        // String stdout output captured from the process
        public final String stdout;
        // String stderr output captured from the process
        public final String stderr;

        /**
         * Constructor for CommandResult.
         * @param exitCode Exit code integer
         * @param stdout Standard output string
         * @param stderr Standard error string
         */
        public CommandResult(int exitCode, String stdout, String stderr) {
            // Store exit code parameter
            this.exitCode = exitCode;
            // Store standard output parameter
            this.stdout = stdout;
            // Store standard error parameter
            this.stderr = stderr;
        }

        /**
         * Returns true if exit code is equal to 0 (success).
         * @return boolean indicating success
         */
        public boolean isSuccess() {
            // Check if exit code equals 0
            return exitCode == 0;
        }
    }

    /**
     * Executes a command with elevated privileges using pkexec or sudo.
     * @param command Command string to run
     * @return CommandResult containing exit code, stdout, and stderr
     */
    public static CommandResult runAsRoot(String command) throws Exception {
        // Declare string array to hold process builder arguments
        String[] cmdArray;

        // Check if pkexec is available in PATH
        if (isCommandAvailable("pkexec")) {
            // Wrap command in pkexec sh -c
            cmdArray = new String[]{"pkexec", "sh", "-c", command};
        // Else check if sudo is available in PATH
        } else if (isCommandAvailable("sudo")) {
            // Wrap command in sudo sh -c
            cmdArray = new String[]{"sudo", "sh", "-c", command};
        // Else fallback to direct sh -c
        } else {
            // Wrap command in direct sh -c
            cmdArray = new String[]{"sh", "-c", command};
        }

        // Initialize ProcessBuilder with constructed argument array
        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        // Start process
        Process process = pb.start();

        // Read standard output stream from process
        String stdout = readStream(process.getInputStream());
        // Read standard error stream from process
        String stderr = readStream(process.getErrorStream());

        // Wait for process completion and obtain exit code
        int exitCode = process.waitFor();
        // Return new CommandResult instance
        return new CommandResult(exitCode, stdout, stderr);
    }

    /**
     * Helper method to verify if a executable binary exists in system PATH.
     */
    private static boolean isCommandAvailable(String cmd) {
        // Read System PATH variable
        String pathEnv = System.getenv("PATH");
        // Return false if PATH variable is null
        if (pathEnv == null) return false;
        // Split PATH variable into directory string array
        for (String path : pathEnv.split(File.pathSeparator)) {
            // Create File instance for command within directory
            File file = new File(path, cmd);
            // Return true if file exists and can be executed
            if (file.exists() && file.canExecute()) {
                // Command exists
                return true;
            }
        }
        // Command does not exist in PATH
        return false;
    }

    /**
     * Helper method to read an InputStream into a UTF-8 String.
     */
    private static String readStream(InputStream is) throws IOException {
        // Create ByteArrayOutputStream to collect stream bytes
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // Create byte buffer array
        byte[] buffer = new byte[1024];
        // Integer variable for storing read length
        int length;
        // Loop while bytes are read from stream
        while ((length = is.read(buffer)) != -1) {
            // Write buffer bytes into output stream
            result.write(buffer, 0, length);
        }
        // Convert collected bytes into UTF-8 string
        return result.toString(StandardCharsets.UTF_8);
    }
}
