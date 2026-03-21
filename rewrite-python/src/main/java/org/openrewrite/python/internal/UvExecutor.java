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
package org.openrewrite.python.internal;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared utility for finding and executing the uv Python package manager.
 */
@UtilityClass
public class UvExecutor {

    private static final long DEFAULT_TIMEOUT_SECONDS = 120;
    private static @Nullable String cachedUvPath;

    @Value
    public static class RunResult {
        boolean success;
        int exitCode;
        String stdout;
        String stderr;
    }

    /**
     * Run a uv command in the given directory.
     *
     * @param workDir   the working directory
     * @param uvPath    the path to the uv executable
     * @param args      the arguments to pass to uv
     * @return the run result
     */
    public static RunResult run(Path workDir, String uvPath, String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = uvPath;
        System.arraycopy(args, 0, command, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Read stdout and stderr in parallel to avoid deadlocks
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append('\n');
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append('\n');
                }
            } catch (IOException e) {
                // Ignore
            }
        });

        stdoutReader.start();
        stderrReader.start();

        boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new RunResult(false, -1, stdout.toString(), "Process timed out after " + DEFAULT_TIMEOUT_SECONDS + "s");
        }

        stdoutReader.join(5000);
        stderrReader.join(5000);

        int exitCode = process.exitValue();
        return new RunResult(exitCode == 0, exitCode, stdout.toString(), stderr.toString());
    }

    /**
     * Find the uv executable on the system.
     *
     * @return the absolute path to uv, or null if not found
     */
    public static @Nullable String findUvExecutable() {
        if (cachedUvPath != null) {
            return cachedUvPath;
        }

        Path projectRoot = findProjectRoot();

        List<String> locations = new ArrayList<>();

        if (projectRoot != null) {
            locations.add(projectRoot.resolve("rewrite-python/rewrite/.venv/bin/uv").toString());
        }

        locations.add(".venv/bin/uv");
        locations.add(System.getProperty("user.home") + "/.local/bin/uv");
        locations.add("/opt/homebrew/bin/uv");
        locations.add("/usr/local/bin/uv");
        locations.add("/usr/bin/uv");

        for (String location : locations) {
            Path path = Paths.get(location);
            if (Files.isExecutable(path)) {
                cachedUvPath = path.toAbsolutePath().toString();
                return cachedUvPath;
            }
        }

        // Try PATH as last resort
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "uv");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String output = sb.toString().trim();
            if (process.waitFor() == 0 && !output.isEmpty()) {
                cachedUvPath = output;
                return cachedUvPath;
            }
        } catch (IOException | InterruptedException e) {
            // Ignore
        }

        return null;
    }

    private static @Nullable Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 20; i++) {
            if (Files.exists(current.resolve("settings.gradle.kts")) ||
                    Files.exists(current.resolve("settings.gradle"))) {
                return current;
            }
            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return null;
    }
}
