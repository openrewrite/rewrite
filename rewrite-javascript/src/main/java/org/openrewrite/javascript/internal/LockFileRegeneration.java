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
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.internal.npmlock.NpmLockEngine;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Regenerate a JavaScript project's lock file after a {@code package.json} edit.
 *
 * <p>For npm ({@code package-lock.json}) regeneration is native: a Java engine
 * consults the project's registry over HTTP and surgically updates the lock,
 * with no package manager execution — see {@link NpmLockEngine}. For the other
 * package managers regeneration still runs the package manager in a temp
 * directory seeded with the {@code package.json}, the existing lock file, and
 * config files such as {@code .npmrc}.
 *
 * <p>Failures are structured ({@link Failure}) so recipes can aggregate them in
 * the {@link NodeLockRegenerationFailures} data table; on any failure the old
 * lock is left untouched.
 */
public abstract class LockFileRegeneration {

    public static final LockFileRegeneration NPM = new NativeNpm();

    public static final LockFileRegeneration YARN_CLASSIC = new ShellOut(
            PackageManagerExecutor.YARN, "yarn.lock",
            new String[]{"install", "--ignore-scripts"});

    public static final LockFileRegeneration YARN_BERRY = new ShellOut(
            PackageManagerExecutor.YARN, "yarn.lock",
            new String[]{"install", "--mode", "skip-build"});

    public static final LockFileRegeneration PNPM = new ShellOut(
            PackageManagerExecutor.PNPM, "pnpm-lock.yaml",
            new String[]{"install", "--lockfile-only", "--ignore-scripts", "--no-strict-peer-dependencies"});

    public static final LockFileRegeneration BUN = new ShellOut(
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

    public enum Reason {
        REGISTRY_UNREACHABLE,
        AUTH_FAILED,
        PACKAGE_NOT_FOUND,
        VERSION_NOT_FOUND,
        INTEGRITY_UNAVAILABLE,
        RESOLUTION_REQUIRED,
        UNSUPPORTED_ENTRY_TYPE,
        UNSUPPORTED_LOCK_VERSION,
        MALFORMED_MANIFEST,
        MALFORMED_LOCK,
        PACKAGE_MANAGER_UNAVAILABLE,
        PACKAGE_MANAGER_FAILED
    }

    @Value
    public static class Failure {
        Reason reason;
        @Nullable String packageName;
        @Nullable String registryUrl;
        String detail;
    }

    @Value
    public static class Result {
        boolean success;
        @Nullable String lockFileContent;
        @Nullable String errorMessage;
        @Nullable Failure failure;

        public static Result success(String lockFileContent) {
            return new Result(true, lockFileContent, null, null);
        }

        public static Result failure(Failure failure) {
            return new Result(false, null, failure.getReason() + ": " + failure.getDetail(), failure);
        }

        public static Result failure(Reason reason, String detail) {
            return failure(new Failure(reason, null, null, detail));
        }
    }

    /**
     * Record a failed regeneration in the data table, once per project.
     */
    public static void insertFailureRow(ExecutionContext ctx, NodeLockRegenerationFailures table,
                                        Path lockPath, Result result) {
        Failure failure = result.getFailure();
        table.insertRow(ctx, new NodeLockRegenerationFailures.Row(
                lockPath.toString(),
                failure == null ? null : failure.getPackageName(),
                failure == null ? Reason.PACKAGE_MANAGER_FAILED.toString() : failure.getReason().toString(),
                failure == null ? (result.getErrorMessage() == null ? "" : result.getErrorMessage()) : failure.getDetail()));
    }

    public abstract String getLockFile();

    /**
     * Regenerate the lock file for an edited {@code package.json}.
     *
     * @param packageJsonContent          the manifest after the recipe's edit
     * @param originalPackageJsonContent  the manifest before the edit, when available;
     *                                    used to detect which {@code overrides} the
     *                                    recipe changed (dependency edits are diffed
     *                                    against the lock itself, which records them)
     * @param existingLockContent         the current lock file content
     * @param configFiles                 extra config, typically {@code {".npmrc": "..."}}
     */
    public abstract Result regenerate(String packageJsonContent,
                                      @Nullable String originalPackageJsonContent,
                                      @Nullable String existingLockContent,
                                      @Nullable Map<String, String> configFiles,
                                      ExecutionContext ctx);

    private static class NativeNpm extends LockFileRegeneration {
        @Override
        public String getLockFile() {
            return "package-lock.json";
        }

        @Override
        public Result regenerate(String packageJsonContent,
                                 @Nullable String originalPackageJsonContent,
                                 @Nullable String existingLockContent,
                                 @Nullable Map<String, String> configFiles,
                                 ExecutionContext ctx) {
            if (existingLockContent == null) {
                return Result.failure(Reason.RESOLUTION_REQUIRED,
                        "Creating a lock file from scratch requires full resolution; only existing " +
                                "package-lock.json files are regenerated natively");
            }
            String npmrc = configFiles == null ? null : configFiles.get(".npmrc");
            return NpmLockEngine.regenerate(packageJsonContent, originalPackageJsonContent,
                    existingLockContent, npmrc, ctx);
        }
    }

    /**
     * The pre-native mechanism, retained for the package managers without a native
     * engine yet. The install args are preserved verbatim from the TypeScript
     * implementation in {@code rewrite-javascript/rewrite/src/javascript/package-manager.ts}.
     */
    private static class ShellOut extends LockFileRegeneration {
        private final PackageManagerExecutor executor;
        private final String lockFile;
        private final String[] installArgs;

        private ShellOut(PackageManagerExecutor executor, String lockFile, String[] installArgs) {
            this.executor = executor;
            this.lockFile = lockFile;
            this.installArgs = installArgs;
        }

        @Override
        public String getLockFile() {
            return lockFile;
        }

        @Override
        public Result regenerate(String packageJsonContent,
                                 @Nullable String originalPackageJsonContent,
                                 @Nullable String existingLockContent,
                                 @Nullable Map<String, String> configFiles,
                                 ExecutionContext ctx) {
            String executablePath = executor.find();
            if (executablePath == null) {
                return Result.failure(Reason.PACKAGE_MANAGER_UNAVAILABLE,
                        executor.getName() + " is not installed or not on PATH");
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
                    return Result.failure(Reason.PACKAGE_MANAGER_FAILED, executor.getName() +
                            " install failed (exit " + runResult.getExitCode() + "): " + stderr);
                }

                Path lockPath = tempDir.resolve(lockFile);
                if (!Files.exists(lockPath)) {
                    return Result.failure(Reason.PACKAGE_MANAGER_FAILED,
                            executor.getName() + " install did not produce a " + lockFile + " file");
                }
                return Result.success(new String(Files.readAllBytes(lockPath), StandardCharsets.UTF_8));

            } catch (IOException e) {
                return Result.failure(Reason.PACKAGE_MANAGER_FAILED,
                        "IO error during " + executor.getName() + " install: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.failure(Reason.PACKAGE_MANAGER_FAILED,
                        executor.getName() + " install was interrupted");
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
}
