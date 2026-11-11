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
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Locates and runs a Python package manager CLI for {@link org.openrewrite.python.DependencyWorkspace}.
 * A pre-configured instance is provided for {@code uv} ({@link #UV}); it caches the
 * resolved executable path on first {@link #find()}.
 * <p>
 * Callers that just need the executable path can call {@link #find()} and skip
 * workspace creation when it returns {@code null}. Callers that already have the
 * path can run subcommands via {@link #run(Path, String, Map, String...)}.
 */
public final class PackageManagerExecutor {

    public static final PackageManagerExecutor UV = new PackageManagerExecutor(
            "uv", 120, PackageManagerExecutor::uvExtraLocations);

    @Value
    public static class RunResult {
        boolean success;
        int exitCode;
        String stdout;
        String stderr;
    }

    private final String name;
    private final long timeoutSeconds;
    private final Supplier<List<String>> extraLocations;

    private @Nullable String cachedPath;

    private PackageManagerExecutor(String name, long timeoutSeconds, Supplier<List<String>> extraLocations) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        this.extraLocations = extraLocations;
    }

    /**
     * Run the package manager in the given directory with additional environment variables.
     */
    public RunResult run(Path workDir, String executablePath, Map<String, String> environment, String... args) throws IOException, InterruptedException {
        return run(workDir, executablePath, environment, null, null, args);
    }

    /**
     * Run the package manager until {@code deadline}, or for this manager's default timeout when there
     * is none, appending the command and its output to {@code log}. The environment is not logged, so
     * credentials belong there rather than in {@code args}.
     */
    public RunResult run(Path workDir, String executablePath, Map<String, String> environment,
                         @Nullable Instant deadline, @Nullable Path log, String... args)
            throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = executablePath;
        System.arraycopy(args, 0, command, 1, args.length);

        long timeoutMillis = deadline == null ? TimeUnit.SECONDS.toMillis(timeoutSeconds) :
                Duration.between(Instant.now(), deadline).toMillis();
        if (timeoutMillis <= 0) {
            RunResult result = new RunResult(false, -1, "", "The deadline passed before " + name + " could start");
            appendToLog(log, command, result);
            return result;
        }

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

        boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        RunResult result;
        if (!finished) {
            process.destroyForcibly();
            result = new RunResult(false, -1, stdout.toString(), "Process timed out after " +
                                                                 TimeUnit.MILLISECONDS.toSeconds(timeoutMillis) + "s");
        } else {
            stdoutReader.join(5000);
            stderrReader.join(5000);
            int exitCode = process.exitValue();
            result = new RunResult(exitCode == 0, exitCode, stdout.toString(), stderr.toString());
        }
        appendToLog(log, command, result);
        return result;
    }

    private static void appendToLog(@Nullable Path log, String[] command, RunResult result) {
        appendToLog(log, "$ " + String.join(" ", command) + "\n" + result.getStdout() + result.getStderr() +
                         (result.isSuccess() ? "" : "(exit code " + result.getExitCode() + ")\n"));
    }

    public static void appendToLog(@Nullable Path log, String text) {
        if (log == null) {
            return;
        }
        try {
            Files.write(log, text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // An unwritable log loses the text, not the command's result
        }
    }

    /**
     * Find the executable on the system. Returns {@code null} when the tool is
     * not installed or not on {@code PATH} — callers should warn-and-skip in
     * that case.
     */
    public @Nullable String find() {
        if (cachedPath != null) {
            return cachedPath;
        }

        List<String> locations = new ArrayList<>(extraLocations.get());
        locations.add(System.getProperty("user.home") + "/.local/bin/" + name);
        locations.add("/opt/homebrew/bin/" + name);
        locations.add("/usr/local/bin/" + name);
        locations.add("/usr/bin/" + name);

        for (String location : locations) {
            Path path = Paths.get(location);
            if (Files.isExecutable(path)) {
                cachedPath = path.toAbsolutePath().toString();
                return cachedPath;
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("which", name);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String output = sb.toString().trim();
                if (process.waitFor() == 0 && !output.isEmpty()) {
                    cachedPath = output;
                    return cachedPath;
                }
            }
        } catch (IOException | InterruptedException e) {
            // Ignore
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

    private static List<String> uvExtraLocations() {
        List<String> locations = new ArrayList<>();
        Path projectRoot = findProjectRoot();
        if (projectRoot != null) {
            locations.add(projectRoot.resolve("rewrite-python/rewrite/.venv/bin/uv").toString());
        }
        locations.add(".venv/bin/uv");
        return locations;
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
