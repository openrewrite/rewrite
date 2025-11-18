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
package org.openrewrite.javascript;

import lombok.experimental.UtilityClass;

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
 * Manages workspace directories for JavaScript/TypeScript compilation with dependencies.
 * Creates cached workspaces with package.json and installed node_modules to enable
 * proper type attribution and dependency resolution.
 */
@UtilityClass
class DependencyWorkspace {
    private static final Path WORKSPACE_BASE = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "openrewrite-js-workspaces"
    );
    private static final int MAX_CACHE_SIZE = 100;
    private static final Map<String, Path> cache = synchronizedMap(
            new LinkedHashMap<String, Path>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Path> eldest) {
                    if (size() > MAX_CACHE_SIZE) {
                        // Clean up the evicted workspace directory
                        cleanupDirectory(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            }
    );

    static {
        // Pre-populate cache with existing workspaces from disk
        initializeCacheFromDisk();
    }

    /**
     * Gets or creates a workspace directory for the given package.json content.
     * Workspaces are cached by content hash to avoid repeated npm installs.
     *
     * @param packageJsonContent The complete package.json file content
     * @return Path to the workspace directory containing node_modules
     */
    static Path getOrCreateWorkspace(String packageJsonContent) {
        String hash = hashContent(packageJsonContent);

        // Check in-memory cache
        Path cached = cache.get(hash);
        if (cached != null && isWorkspaceValid(cached)) {
            return cached;
        }

        // Check disk cache (for cross-JVM reuse)
        Path workspaceDir = WORKSPACE_BASE.resolve(hash);
        if (isWorkspaceValid(workspaceDir)) {
            cache.put(hash, workspaceDir);
            return workspaceDir;
        }

        // Create new workspace
        try {
            // Ensure workspace base directory exists
            // createDirectories is idempotent and safe to call even if directory exists
            Files.createDirectories(WORKSPACE_BASE);

            // Use temp directory for atomic creation
            Path tempDir = Files.createTempDirectory(WORKSPACE_BASE, hash + ".tmp-");

            try {
                // Write package.json
                Files.write(
                        tempDir.resolve("package.json"),
                        packageJsonContent.getBytes(StandardCharsets.UTF_8)
                );

                // Run npm install
                ProcessBuilder pb = new ProcessBuilder("npm", "install", "--silent");
                pb.directory(tempDir.toFile());
                pb.inheritIO();
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new RuntimeException("npm install failed with exit code: " + exitCode);
                }

                // Move to final location (atomic on POSIX systems)
                try {
                    Files.move(tempDir, workspaceDir);
                } catch (IOException e) {
                    // If move fails, another thread might have created it
                    if (isWorkspaceValid(workspaceDir)) {
                        // Use the other thread's workspace
                        cleanupDirectory(tempDir);
                    } else {
                        throw e;
                    }
                }

                cache.put(hash, workspaceDir);
                return workspaceDir;

            } catch (Exception e) {
                // Clean up temp directory on failure
                cleanupDirectory(tempDir);
                throw e;
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create dependency workspace", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("npm install was interrupted", e);
        }
    }

    /**
     * Generates a hash from package.json content for caching.
     */
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

    /**
     * Checks if a workspace is valid (has node_modules directory).
     */
    private static boolean isWorkspaceValid(Path workspaceDir) {
        return Files.exists(workspaceDir) &&
                Files.isDirectory(workspaceDir.resolve("node_modules")) &&
                Files.exists(workspaceDir.resolve("package.json"));
    }

    /**
     * Cleans up a directory, ignoring errors.
     */
    private static void cleanupDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder()) // Delete files before directories
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

    /**
     * Clears the in-memory cache. Useful for testing.
     */
    static void clearCache() {
        cache.clear();
    }

    /**
     * Initializes the cache by discovering existing valid workspaces from disk.
     * This allows reuse of workspaces across JVM restarts and ensures proper
     * LRU eviction even for pre-existing workspaces.
     */
    private static void initializeCacheFromDisk() {
        try {
            if (!Files.exists(WORKSPACE_BASE)) {
                return;
            }

            Files.list(WORKSPACE_BASE)
                    .filter(Files::isDirectory)
                    .filter(dir -> !dir.getFileName().toString().contains(".tmp-")) // Skip temp dirs
                    .filter(DependencyWorkspace::isWorkspaceValid)
                    .sorted((a, b) -> {
                        // Sort by last modified time (oldest first)
                        // This way oldest workspaces will be evicted first when we hit the limit
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
            // Ignore - cache will be empty and workspaces will be created as needed
        }
    }
}
