/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.python;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.UvExecutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

/**
 * Manages workspace directories for Python projects with dependencies.
 * Creates cached workspaces with pyproject.toml and installed virtual environments
 * to enable proper type attribution via ty LSP.
 */
@UtilityClass
public class DependencyWorkspace {
    private static final Path WORKSPACE_BASE = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "openrewrite-python-workspaces"
    );
    /**
     * Bump this when the workspace layout changes (e.g. new files expected)
     * so that stale cached workspaces are automatically recreated.
     */
    private static final String WORKSPACE_VERSION = "2";
    private static final int MAX_CACHE_SIZE = 100;
    private static final Map<String, Path> cache = synchronizedMap(
            new LinkedHashMap<String, Path>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Path> eldest) {
                    if (size() > MAX_CACHE_SIZE) {
                        cleanupDirectory(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            }
    );

    static {
        initializeCacheFromDisk();
    }

    /**
     * Gets or creates a workspace directory for the given pyproject.toml content.
     * Workspaces are cached by content hash to avoid repeated installations.
     *
     * @param pyprojectContent The complete pyproject.toml file content
     * @return Path to the workspace directory containing .venv
     */
    static Path getOrCreateWorkspace(String pyprojectContent) {
        String hash = hashContent(pyprojectContent);

        // Check in-memory cache
        Path cached = cache.get(hash);
        if (cached != null && isWorkspaceValid(cached)) {
            return cached;
        }

        // Check disk cache
        Path workspaceDir = WORKSPACE_BASE.resolve(hash);
        if (isWorkspaceValid(workspaceDir)) {
            cache.put(hash, workspaceDir);
            return workspaceDir;
        }

        // Create new workspace
        try {
            Files.createDirectories(WORKSPACE_BASE);

            // Use temp directory for atomic creation
            Path tempDir = Files.createTempDirectory(WORKSPACE_BASE, hash + ".tmp-");

            try {
                // Write pyproject.toml
                Files.write(
                        tempDir.resolve("pyproject.toml"),
                        pyprojectContent.getBytes(StandardCharsets.UTF_8)
                );

                // Sync: creates .venv, generates uv.lock, and installs dependencies
                runCommand(tempDir, "uv", "sync");

                // Install ty for type stubs
                runCommand(tempDir, "uv", "pip", "install", "ty");

                // Write workspace version for cache invalidation
                Files.write(tempDir.resolve("version.txt"),
                        WORKSPACE_VERSION.getBytes(StandardCharsets.UTF_8));

                // Move to final location
                try {
                    Files.move(tempDir, workspaceDir);
                } catch (IOException e) {
                    if (isWorkspaceValid(workspaceDir)) {
                        cleanupDirectory(tempDir);
                    } else {
                        throw e;
                    }
                }

                cache.put(hash, workspaceDir);
                return workspaceDir;

            } catch (Exception e) {
                cleanupDirectory(tempDir);
                throw e;
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create dependency workspace", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("uv install was interrupted", e);
        }
    }

    /**
     * Gets or creates a workspace directory for a requirements.txt file.
     * Returns null (graceful degradation) when uv is unavailable.
     *
     * @param requirementsContent The complete requirements.txt content
     * @param originalFilePath    The original file path on disk (supports -r includes), or null
     * @return Path to the workspace directory, or null if uv is unavailable
     */
    static @Nullable Path getOrCreateRequirementsWorkspace(String requirementsContent,
                                                            @Nullable Path originalFilePath) {
        String uvPath = UvExecutor.findUvExecutable();
        if (uvPath == null) {
            return null;
        }

        String hash = "req-" + hashContent(requirementsContent);

        // Check in-memory cache
        Path cached = cache.get(hash);
        if (cached != null && isRequirementsWorkspaceValid(cached)) {
            return cached;
        }

        // Check disk cache
        Path workspaceDir = WORKSPACE_BASE.resolve(hash);
        if (isRequirementsWorkspaceValid(workspaceDir)) {
            cache.put(hash, workspaceDir);
            return workspaceDir;
        }

        // Create new workspace
        try {
            Files.createDirectories(WORKSPACE_BASE);

            Path tempDir = Files.createTempDirectory(WORKSPACE_BASE, hash + ".tmp-");

            try {
                // Create virtualenv
                runCommandWithPath(tempDir, uvPath, "venv");

                // Install dependencies from requirements file
                Path reqFile;
                if (originalFilePath != null && Files.exists(originalFilePath)) {
                    reqFile = originalFilePath;
                } else {
                    reqFile = tempDir.resolve("requirements.txt");
                    Files.write(reqFile, requirementsContent.getBytes(StandardCharsets.UTF_8));
                }
                runCommandWithPath(tempDir, uvPath, "pip", "install", "-r", reqFile.toString());

                // Capture freeze output BEFORE installing ty
                UvExecutor.RunResult freezeResult = UvExecutor.run(tempDir, uvPath, "pip", "freeze");
                if (freezeResult.isSuccess()) {
                    Files.write(
                            tempDir.resolve("freeze.txt"),
                            freezeResult.getStdout().getBytes(StandardCharsets.UTF_8)
                    );
                }

                // Install ty for type stubs (after freeze so it's not in the dep model)
                runCommandWithPath(tempDir, uvPath, "pip", "install", "ty");

                // Write workspace version for cache invalidation
                Files.write(tempDir.resolve("version.txt"),
                        WORKSPACE_VERSION.getBytes(StandardCharsets.UTF_8));

                // Move to final location
                try {
                    Files.move(tempDir, workspaceDir);
                } catch (IOException e) {
                    if (isRequirementsWorkspaceValid(workspaceDir)) {
                        cleanupDirectory(tempDir);
                    } else {
                        throw e;
                    }
                }

                cache.put(hash, workspaceDir);
                return workspaceDir;

            } catch (Exception e) {
                cleanupDirectory(tempDir);
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Gets or creates a workspace directory for a setuptools project (setup.cfg / setup.py).
     * Uses {@code uv pip install <projectDir>} to install the project and its dependencies.
     * Returns null (graceful degradation) when uv is unavailable.
     *
     * @param manifestContent The setup.cfg (or setup.py) content for hashing
     * @param projectDir      The project directory to install from, or null
     * @return Path to the workspace directory, or null if uv is unavailable
     */
    public static @Nullable Path getOrCreateSetuptoolsWorkspace(String manifestContent,
                                                                @Nullable Path projectDir) {
        String uvPath = UvExecutor.findUvExecutable();
        if (uvPath == null) {
            return null;
        }

        String hash = "setup-" + hashContent(manifestContent);

        // Check in-memory cache
        Path cached = cache.get(hash);
        if (cached != null && isRequirementsWorkspaceValid(cached)) {
            return cached;
        }

        // Check disk cache
        Path workspaceDir = WORKSPACE_BASE.resolve(hash);
        if (isRequirementsWorkspaceValid(workspaceDir)) {
            cache.put(hash, workspaceDir);
            return workspaceDir;
        }

        if (projectDir == null || !Files.isDirectory(projectDir)) {
            return null;
        }

        // Create new workspace
        try {
            Files.createDirectories(WORKSPACE_BASE);

            Path tempDir = Files.createTempDirectory(WORKSPACE_BASE, hash + ".tmp-");

            try {
                // Create virtualenv
                runCommandWithPath(tempDir, uvPath, "venv");

                // Install from the project directory
                runCommandWithPath(tempDir, uvPath, "pip", "install", projectDir.toString());

                // Capture freeze output BEFORE installing ty
                UvExecutor.RunResult freezeResult = UvExecutor.run(tempDir, uvPath, "pip", "freeze");
                if (freezeResult.isSuccess()) {
                    Files.write(
                            tempDir.resolve("freeze.txt"),
                            freezeResult.getStdout().getBytes(StandardCharsets.UTF_8)
                    );
                }

                // Install ty for type stubs (after freeze so it's not in the dep model)
                runCommandWithPath(tempDir, uvPath, "pip", "install", "ty");

                // Write workspace version for cache invalidation
                Files.write(tempDir.resolve("version.txt"),
                        WORKSPACE_VERSION.getBytes(StandardCharsets.UTF_8));

                // Move to final location
                try {
                    Files.move(tempDir, workspaceDir);
                } catch (IOException e) {
                    if (isRequirementsWorkspaceValid(workspaceDir)) {
                        cleanupDirectory(tempDir);
                    } else {
                        throw e;
                    }
                }

                cache.put(hash, workspaceDir);
                return workspaceDir;

            } catch (Exception e) {
                cleanupDirectory(tempDir);
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    static String readFreezeOutput(Path workspaceDir) {
        try {
            return new String(Files.readAllBytes(workspaceDir.resolve("freeze.txt")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static boolean isRequirementsWorkspaceValid(Path workspaceDir) {
        return Files.exists(workspaceDir) &&
                Files.isDirectory(workspaceDir.resolve(".venv")) &&
                Files.exists(workspaceDir.resolve("freeze.txt")) &&
                hasCurrentVersion(workspaceDir);
    }

    private static void runCommandWithPath(Path dir, String uvPath, String... args) throws IOException, InterruptedException {
        UvExecutor.RunResult result = UvExecutor.run(dir, uvPath, args);
        if (!result.isSuccess()) {
            throw new RuntimeException("uv " + String.join(" ", args) + " failed with exit code: " + result.getExitCode());
        }
    }

    private static void runCommand(Path dir, String... command) throws IOException, InterruptedException {
        String uvPath = UvExecutor.findUvExecutable();
        if (uvPath == null) {
            throw new RuntimeException("uv is not installed. Install it with: pip install uv");
        }

        // The first element of command is "uv", strip it and pass the rest as args
        String[] args = new String[command.length - 1];
        System.arraycopy(command, 1, args, 0, args.length);

        UvExecutor.RunResult result = UvExecutor.run(dir, uvPath, args);
        if (!result.isSuccess()) {
            throw new RuntimeException(String.join(" ", command) + " failed with exit code: " + result.getExitCode());
        }
    }

    private static String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash)
                    .substring(0, 16)
                    .replace('/', '_')
                    .replace('+', '-');
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static boolean isWorkspaceValid(Path workspaceDir) {
        return Files.exists(workspaceDir) &&
                Files.isDirectory(workspaceDir.resolve(".venv")) &&
                Files.exists(workspaceDir.resolve("pyproject.toml")) &&
                hasCurrentVersion(workspaceDir);
    }

    private static boolean hasCurrentVersion(Path workspaceDir) {
        try {
            Path versionFile = workspaceDir.resolve("version.txt");
            if (!Files.exists(versionFile)) {
                return false;
            }
            String version = new String(Files.readAllBytes(versionFile), StandardCharsets.UTF_8).trim();
            return WORKSPACE_VERSION.equals(version);
        } catch (IOException e) {
            return false;
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

    static void clearCache() {
        cache.clear();
    }

    private static void initializeCacheFromDisk() {
        try {
            if (!Files.exists(WORKSPACE_BASE)) {
                return;
            }

            Files.list(WORKSPACE_BASE)
                    .filter(Files::isDirectory)
                    .filter(dir -> !dir.getFileName().toString().contains(".tmp-"))
                    .filter(dir -> {
                        String name = dir.getFileName().toString();
                        if (name.startsWith("req-") || name.startsWith("setup-")) {
                            return isRequirementsWorkspaceValid(dir);
                        }
                        return isWorkspaceValid(dir);
                    })
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .forEach(workspaceDir -> {
                        String hash = workspaceDir.getFileName().toString();
                        cache.put(hash, workspaceDir);
                    });
        } catch (IOException e) {
            // Ignore - cache will be populated as needed
        }
    }
}
