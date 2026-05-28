/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Locates and runs a Node.js package manager CLI (npm, yarn, pnpm, bun).
 * Pre-configured instances are provided for each; {@link #find()} caches the
 * resolved executable path on first lookup. On Windows the executable is
 * suffixed with {@code .cmd} (matching {@code shell-utils.ts} behaviour).
 */
public final class PackageManagerExecutor {

    public static final PackageManagerExecutor NPM  = new PackageManagerExecutor("npm",  120);
    public static final PackageManagerExecutor YARN = new PackageManagerExecutor("yarn", 120);
    public static final PackageManagerExecutor PNPM = new PackageManagerExecutor("pnpm", 120);
    public static final PackageManagerExecutor BUN  = new PackageManagerExecutor("bun",  120);

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("windows");

    @Value
    public static class RunResult {
        boolean success;
        int exitCode;
        String stdout;
        String stderr;
    }

    @Getter
    private final String name;

    private final long timeoutSeconds;

    private @Nullable String cachedPath;

    private PackageManagerExecutor(String name, long timeoutSeconds) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Test-only factory for constructing arbitrary instances (e.g. with names that don't resolve). */
    static PackageManagerExecutor forTesting(String name, long timeoutSeconds) {
        return new PackageManagerExecutor(name, timeoutSeconds);
    }

    /**
     * Run the package manager in the given directory.
     */
    public RunResult run(Path workDir, String executablePath, Map<String, String> environment, String... args)
            throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = executablePath;
        System.arraycopy(args, 0, command, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.environment().putAll(environment);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread stdoutReader = new Thread(() -> drain(process.getInputStream(), stdout));
        Thread stderrReader = new Thread(() -> drain(process.getErrorStream(), stderr));
        stdoutReader.start();
        stderrReader.start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new RunResult(false, -1, stdout.toString(),
                    "Process timed out after " + timeoutSeconds + "s");
        }
        stdoutReader.join(5000);
        stderrReader.join(5000);

        int exitCode = process.exitValue();
        return new RunResult(exitCode == 0, exitCode, stdout.toString(), stderr.toString());
    }

    /**
     * Find the executable on the system, returning {@code null} if not installed.
     * Searches standard install locations first, then falls back to {@code which <name>}.
     */
    public @Nullable String find() {
        if (cachedPath != null) {
            return cachedPath;
        }
        String exeName = IS_WINDOWS ? name + ".cmd" : name;
        String[] locations = {
                System.getProperty("user.home") + "/.local/bin/" + exeName,
                "/opt/homebrew/bin/" + exeName,
                "/usr/local/bin/" + exeName,
                "/usr/bin/" + exeName
        };
        for (String location : locations) {
            Path path = Paths.get(location);
            if (Files.isExecutable(path)) {
                cachedPath = path.toAbsolutePath().toString();
                return cachedPath;
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(IS_WINDOWS ? "where" : "which", exeName);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() == 0) {
                        sb.append(line);
                    }
                }
                String output = sb.toString().trim();
                if (process.waitFor() == 0 && !output.isEmpty()) {
                    cachedPath = output;
                    return cachedPath;
                }
            }
        } catch (IOException | InterruptedException e) {
            // Ignore and return null below.
        }
        return null;
    }

    private static void drain(InputStream in, StringBuilder out) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
