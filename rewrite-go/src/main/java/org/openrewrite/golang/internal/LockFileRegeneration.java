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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Regenerates a Go module's {@code go.sum} by running {@code go mod download all}
 * in a temporary directory seeded with {@code go.mod}. The existing {@code go.sum}
 * is not seeded by default, so stale entries are dropped rather than left behind
 * ({@code go mod download} only appends). A module with no dependencies produces
 * no {@code go.sum}, reported as success with empty content.
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration GO_SUM = new LockFileRegeneration(
            GoExecutor.GO, "go.mod", "go.sum", "mod", "download", "all");

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

    private final GoExecutor executor;
    private final String manifestFile;
    private final String lockFile;
    private final String[] args;

    private LockFileRegeneration(GoExecutor executor, String manifestFile, String lockFile, String... args) {
        this.executor = executor;
        this.manifestFile = manifestFile;
        this.lockFile = lockFile;
        this.args = args;
    }

    public Result regenerate(String manifestContent) {
        return regenerate(manifestContent, null, Collections.emptyMap());
    }

    public Result regenerate(String manifestContent, @Nullable String existingLockContent) {
        return regenerate(manifestContent, existingLockContent, Collections.emptyMap());
    }

    /**
     * Regenerate {@code go.sum} from the given {@code go.mod} content.
     *
     * @param manifestContent     the {@code go.mod} content to lock
     * @param existingLockContent the current {@code go.sum}, seeded to enable an
     *                            incremental (append-only) update; {@code null} for a
     *                            clean regeneration
     * @param environment         additional environment variables merged into the run
     * @return the regenerated {@code go.sum} content (empty when the module has no
     * dependencies), or an error message
     */
    public Result regenerate(String manifestContent, @Nullable String existingLockContent, Map<String, String> environment) {
        String executablePath = executor.find();
        if (executablePath == null) {
            return Result.failure(executor.getName() + " is not installed or not on PATH");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("openrewrite-go-lock-");

            Files.write(tempDir.resolve(manifestFile),
                    manifestContent.getBytes(StandardCharsets.UTF_8));

            if (existingLockContent != null) {
                Files.write(tempDir.resolve(lockFile),
                        existingLockContent.getBytes(StandardCharsets.UTF_8));
            }

            GoExecutor.RunResult runResult = executor.run(tempDir, executablePath, environment, args);
            if (!runResult.isSuccess()) {
                String stderr = runResult.getStderr();
                if (stderr != null && stderr.length() > 2000) {
                    stderr = stderr.substring(0, 2000) + "...";
                }
                return Result.failure(executor.getName() + " " + String.join(" ", args) +
                        " failed (exit " + runResult.getExitCode() + "): " + stderr);
            }

            Path lockPath = tempDir.resolve(lockFile);
            if (!Files.exists(lockPath)) {
                return Result.success("");
            }
            return Result.success(new String(Files.readAllBytes(lockPath), StandardCharsets.UTF_8));

        } catch (IOException e) {
            return Result.failure("IO error during " + executor.getName() + " " + String.join(" ", args) + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure(executor.getName() + " " + String.join(" ", args) + " was interrupted");
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
            // Ignore cleanup errors
        }
    }
}
