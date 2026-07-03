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
package org.openrewrite.golang.internal;

import lombok.Getter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Locates and runs the {@code go} toolchain. Mirrors the Python/JavaScript
 * {@code PackageManagerExecutor}, specialized to the single {@code go} binary.
 * The resolved path is cached on first {@link #find()}; callers should
 * warn-and-skip when it returns {@code null}.
 */
public final class GoExecutor {

    public static final GoExecutor GO = new GoExecutor("go", 300);

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

    private GoExecutor(String name, long timeoutSeconds) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
    }

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
     * Find the {@code go} executable. Returns {@code null} when it is not
     * installed or not on {@code PATH}.
     */
    public @Nullable String find() {
        if (cachedPath != null) {
            return cachedPath;
        }

        List<String> locations = new ArrayList<>();
        String goRoot = System.getenv("GOROOT");
        if (goRoot != null && !goRoot.trim().isEmpty()) {
            locations.add(goRoot + "/bin/" + name);
        }
        locations.add("/usr/local/go/bin/" + name);
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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
