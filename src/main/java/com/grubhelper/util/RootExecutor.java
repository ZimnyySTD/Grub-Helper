package com.grubhelper.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles executing commands with root privileges via pkexec or sudo.
 */
public class RootExecutor {

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static CommandResult runAsRoot(String command) throws Exception {
        String[] cmdArray;
        if (isCommandAvailable("pkexec")) {
            cmdArray = new String[]{"pkexec", "sh", "-c", command};
        } else if (isCommandAvailable("sudo")) {
            cmdArray = new String[]{"sudo", "sh", "-c", command};
        } else {
            cmdArray = new String[]{"sh", "-c", command};
        }

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        Process process = pb.start();

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
    }

    private static boolean isCommandAvailable(String cmd) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;
        for (String path : pathEnv.split(File.pathSeparator)) {
            File file = new File(path, cmd);
            if (file.exists() && file.canExecute()) {
                return true;
            }
        }
        return false;
    }

    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
