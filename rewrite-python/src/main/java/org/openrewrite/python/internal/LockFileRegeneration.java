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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regenerates a lock file by running {@code <packageManager> lock} in a temporary
 * directory seeded with the dependencies file (and optionally an existing lock).
 * Pre-configured instances are provided for {@code uv} ({@link #UV}) and
 * {@code pipenv} ({@link #PIPENV}).
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration UV = new LockFileRegeneration(
            PackageManagerExecutor.UV, "pyproject.toml", "uv.lock");

    public static final LockFileRegeneration PIPENV = new LockFileRegeneration(
            PackageManagerExecutor.PIPENV, "Pipfile", "Pipfile.lock");

    @Value
    public static class Result {
        boolean success;
        @Nullable String lockFileContent;
        @Nullable String errorMessage;

        public static Result success(String lockFileContent) {
            return new Result(true, lockFileContent, null);
        }

        public static Result failure(String errorMessage) {
            return new Result(false, null, errorMessage);
        }
    }

    private final PackageManagerExecutor executor;
    private final String dependenciesFile;
    private final String lockFile;

    private LockFileRegeneration(PackageManagerExecutor executor, String dependenciesFile, String lockFile) {
        this.executor = executor;
        this.dependenciesFile = dependenciesFile;
        this.lockFile = lockFile;
    }

    public Result regenerate(String dependenciesContent) {
        return regenerate(dependenciesContent, null, Collections.emptyMap());
    }

    public Result regenerate(String dependenciesContent, @Nullable String existingLockContent) {
        return regenerate(dependenciesContent, existingLockContent, Collections.emptyMap());
    }

    /**
     * Regenerate the lock file from the given dependencies content.
     * When an existing lock file is provided it is seeded into the working
     * directory so the package manager performs a minimal update rather than
     * re-resolving every dependency from scratch.
     *
     * @param dependenciesContent the dependencies-file content to lock
     * @param existingLockContent the current lock file content, or {@code null}
     * @param environment         additional environment variables (e.g., SSL_CERT_FILE)
     * @return a result containing the new lock file content, or an error message
     */
    public Result regenerate(String dependenciesContent, @Nullable String existingLockContent, Map<String, String> environment) {
        String executablePath = executor.find();
        if (executablePath == null) {
            return Result.failure(executor.getName() + " is not installed. Install it with: pip install " + executor.getName());
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("openrewrite-packagemanager-lock-");

            Files.write(tempDir.resolve(dependenciesFile),
                    dependenciesContent.getBytes(StandardCharsets.UTF_8));

            if (existingLockContent != null) {
                Files.write(tempDir.resolve(lockFile),
                        existingLockContent.getBytes(StandardCharsets.UTF_8));
            }

            PackageManagerExecutor.RunResult runResult = executor.run(tempDir, executablePath,
                    mergeEnv(environment), "lock");
            if (!runResult.isSuccess()) {
                return Result.failure(executor.getName() + " lock failed (exit code " + runResult.getExitCode() + "): " + runResult.getStderr());
            }

            Path lockPath = tempDir.resolve(lockFile);
            if (!Files.exists(lockPath)) {
                return Result.failure(executor.getName() + " lock did not produce a " + lockFile + " file");
            }

            String lockContent = new String(Files.readAllBytes(lockPath), StandardCharsets.UTF_8);
            return Result.success(lockContent);

        } catch (IOException e) {
            return Result.failure("IO error during " + executor.getName() + " lock: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(executor.getName() + " lock was interrupted");
        } finally {
            if (tempDir != null) {
                cleanupDirectory(tempDir);
            }
        }
    }

    private Map<String, String> mergeEnv(Map<String, String> environment) {
        Map<String, String> defaults = executor.getEnvDefaults();
        if (defaults.isEmpty()) {
            return environment;
        }
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        merged.putAll(environment);
        return merged;
    }

    private static void cleanupDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}
