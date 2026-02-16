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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Utility for regenerating uv.lock files by running {@code uv lock} in a temporary directory.
 */
@UtilityClass
public class UvLockRegeneration {

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

    /**
     * Regenerate a uv.lock file from the given pyproject.toml content.
     *
     * @param pyprojectContent the pyproject.toml content to lock
     * @return a result containing the new lock file content, or an error message
     */
    public static Result regenerate(String pyprojectContent) {
        String uvPath = UvExecutor.findUvExecutable();
        if (uvPath == null) {
            return Result.failure("uv is not installed. Install it with: pip install uv");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("openrewrite-uv-lock-");

            Files.write(
                    tempDir.resolve("pyproject.toml"),
                    pyprojectContent.getBytes(StandardCharsets.UTF_8)
            );

            UvExecutor.RunResult runResult = UvExecutor.run(tempDir, uvPath, "lock");
            if (!runResult.isSuccess()) {
                return Result.failure("uv lock failed (exit code " + runResult.getExitCode() + "): " + runResult.getStderr());
            }

            Path lockFile = tempDir.resolve("uv.lock");
            if (!Files.exists(lockFile)) {
                return Result.failure("uv lock did not produce a uv.lock file");
            }

            String lockContent = new String(Files.readAllBytes(lockFile), StandardCharsets.UTF_8);
            return Result.success(lockContent);

        } catch (IOException e) {
            return Result.failure("IO error during uv lock: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure("uv lock was interrupted");
        } finally {
            if (tempDir != null) {
                cleanupDirectory(tempDir);
            }
        }
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
