// Package declaration for root privilege utility execution
package com.grubhelper.util;

/**
 * Top-level class representing the execution result of a shell or root command.
 */
public class CommandResult {
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
