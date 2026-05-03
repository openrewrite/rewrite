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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Regenerate a JavaScript project's lock file by running the package manager
 * in a temp directory seeded with the package.json and (optionally) the existing
 * lock file plus config files such as {@code .npmrc}.
 *
 * <p>Pre-configured instances are provided for each
 * {@link PackageManager}; {@link #forPackageManager(PackageManager)} dispatches
 * to the right one. The install args are preserved verbatim from the TypeScript
 * implementation in {@code rewrite-javascript/rewrite/src/javascript/package-manager.ts}.
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration NPM = new LockFileRegeneration(
            PackageManagerExecutor.NPM, "package-lock.json",
            new String[]{"install", "--package-lock-only", "--ignore-scripts", "--legacy-peer-deps"});

    public static final LockFileRegeneration YARN_CLASSIC = new LockFileRegeneration(
            PackageManagerExecutor.YARN, "yarn.lock",
            new String[]{"install", "--ignore-scripts"});

    public static final LockFileRegeneration YARN_BERRY = new LockFileRegeneration(
            PackageManagerExecutor.YARN, "yarn.lock",
            new String[]{"install", "--mode", "skip-build"});

    public static final LockFileRegeneration PNPM = new LockFileRegeneration(
            PackageManagerExecutor.PNPM, "pnpm-lock.yaml",
            new String[]{"install", "--lockfile-only", "--ignore-scripts", "--no-strict-peer-dependencies"});

    public static final LockFileRegeneration BUN = new LockFileRegeneration(
            PackageManagerExecutor.BUN, "bun.lock",
            new String[]{"install", "--ignore-scripts"});

    public static @Nullable LockFileRegeneration forPackageManager(@Nullable PackageManager pm) {
        if (pm == null) {
            return null;
        }
        switch (pm) {
            case Npm:         return NPM;
            case YarnClassic: return YARN_CLASSIC;
            case YarnBerry:   return YARN_BERRY;
            case Pnpm:        return PNPM;
            case Bun:         return BUN;
            default:          return null;
        }
    }

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
    private final String lockFile;
    private final String[] installArgs;

    private LockFileRegeneration(PackageManagerExecutor executor, String lockFile, String[] installArgs) {
        this.executor = executor;
        this.lockFile = lockFile;
        this.installArgs = installArgs;
    }

    public Result regenerate(String packageJsonContent) {
        return regenerate(packageJsonContent, null, null);
    }

    public Result regenerate(String packageJsonContent, @Nullable String existingLockContent) {
        return regenerate(packageJsonContent, existingLockContent, null);
    }

    /**
     * Regenerate the lock file. Optional inputs:
     * <ul>
     *   <li>{@code existingLockContent} — when present, seeded into the temp dir so
     *       the package manager performs a minimal update rather than a full
     *       re-resolve.</li>
     *   <li>{@code configFiles} — extra files to seed into the temp dir
     *       (typically {@code {".npmrc": "..."}}).</li>
     * </ul>
     */
    public Result regenerate(String packageJsonContent,
                             @Nullable String existingLockContent,
                             @Nullable Map<String, String> configFiles) {
        String executablePath = executor.find();
        if (executablePath == null) {
            return Result.failure(executor.getName() + " is not installed or not on PATH");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("openrewrite-pm-lock-");

            Files.write(tempDir.resolve("package.json"),
                    packageJsonContent.getBytes(StandardCharsets.UTF_8));

            if (existingLockContent != null) {
                Files.write(tempDir.resolve(lockFile),
                        existingLockContent.getBytes(StandardCharsets.UTF_8));
            }
            if (configFiles != null) {
                for (Map.Entry<String, String> entry : configFiles.entrySet()) {
                    Files.write(tempDir.resolve(entry.getKey()),
                            entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }

            PackageManagerExecutor.RunResult runResult = executor.run(tempDir, executablePath,
                    Collections.<String, String>emptyMap(), installArgs);
            if (!runResult.isSuccess()) {
                String stderr = runResult.getStderr();
                if (stderr != null && stderr.length() > 2000) {
                    stderr = stderr.substring(0, 2000) + "...";
                }
                return Result.failure(executor.getName() + " install failed (exit "
                        + runResult.getExitCode() + "): " + stderr);
            }

            Path lockPath = tempDir.resolve(lockFile);
            if (!Files.exists(lockPath)) {
                return Result.failure(executor.getName() + " install did not produce a "
                        + lockFile + " file");
            }
            return Result.success(new String(Files.readAllBytes(lockPath), StandardCharsets.UTF_8));

        } catch (IOException e) {
            return Result.failure("IO error during " + executor.getName() + " install: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(executor.getName() + " install was interrupted");
        } finally {
            if (tempDir != null) {
                cleanupDirectory(tempDir);
            }
        }
    }

    private static void cleanupDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        } catch (IOException e) {
            // Ignore
        }
    }
}
