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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Locates and runs a Python package manager CLI. Pre-configured instances are
 * provided for {@code uv} ({@link #UV}) and {@code pipenv} ({@link #PIPENV});
 * each caches the resolved executable path on first {@link #find()}.
 * <p>
 * Callers that just need the executable path can call {@link #find()} and skip
 * lock regeneration when it returns {@code null}. Callers that already have the
 * path can run subcommands via {@link #run(Path, String, Map, String...)}.
 */
public final class PackageManagerExecutor {

    public static final PackageManagerExecutor UV = new PackageManagerExecutor(
            "uv", 120, PackageManagerExecutor::uvExtraLocations, Collections.emptyMap());

    public static final PackageManagerExecutor PIPENV = new PackageManagerExecutor(
            "pipenv", 300, Collections::emptyList, pipenvEnvDefaults());

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
    private final Supplier<List<String>> extraLocations;

    /**
     * Environment variables that should be merged into every invocation of this
     * tool (caller-supplied values win on key collision).
     */
    @Getter
    private final Map<String, String> envDefaults;

    private @Nullable String cachedPath;

    private PackageManagerExecutor(String name, long timeoutSeconds,
                                   Supplier<List<String>> extraLocations, Map<String, String> envDefaults) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        this.extraLocations = extraLocations;
        this.envDefaults = envDefaults;
    }

    /**
     * Run the package manager in the given directory with additional environment variables.
     */
    public RunResult run(Path workDir, String executablePath, Map<String, String> environment, String... args) throws IOException, InterruptedException {
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
            return new RunResult(false, -1, stdout.toString(), "Process timed out after " + timeoutSeconds + "s");
        }

        stdoutReader.join(5000);
        stderrReader.join(5000);

        int exitCode = process.exitValue();
        return new RunResult(exitCode == 0, exitCode, stdout.toString(), stderr.toString());
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

    private static Map<String, String> pipenvEnvDefaults() {
        // PIPENV_VENV_IN_PROJECT keeps the virtualenv inside the temp dir so it's
        // torn down with the rest of the work area; NOSPIN/QUIET keep stderr clean.
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("PIPENV_VENV_IN_PROJECT", "1");
        defaults.put("PIPENV_NOSPIN", "1");
        defaults.put("PIPENV_QUIET", "1");
        return Collections.unmodifiableMap(defaults);
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
