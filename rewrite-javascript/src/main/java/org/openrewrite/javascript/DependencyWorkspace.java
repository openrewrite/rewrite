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
import org.openrewrite.javascript.internal.PackageManagerExecutor;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

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
     * Gets or creates a workspace directory for the given package.json content,
     * using npm as the package manager. Delegates to
     * {@link #getOrCreateWorkspace(String, PackageManager)}.
     *
     * @param packageJsonContent The complete package.json file content
     * @return Path to the workspace directory containing node_modules
     */
    static Path getOrCreateWorkspace(String packageJsonContent) {
        return getOrCreateWorkspace(packageJsonContent, PackageManager.Npm);
    }

    /**
     * Gets or creates a workspace directory for the given package.json content.
     * Workspaces are cached by a key combining the content hash and package manager
     * name, so the same package.json installed with different PMs yields separate
     * workspaces.
     *
     * @param packageJsonContent The complete package.json file content
     * @param pm                 The package manager to use for installation
     * @return Path to the workspace directory containing node_modules
     */
    static Path getOrCreateWorkspace(String packageJsonContent, PackageManager pm) {
        String key = hashContent(packageJsonContent) + "_" + pm.name();

        // Check in-memory cache
        Path cached = cache.get(key);
        if (cached != null && isWorkspaceValid(cached)) {
            return cached;
        }

        // Check disk cache (for cross-JVM reuse)
        Path workspaceDir = WORKSPACE_BASE.resolve(key);
        if (isWorkspaceValid(workspaceDir)) {
            cache.put(key, workspaceDir);
            return workspaceDir;
        }

        // Create new workspace
        try {
            // Ensure workspace base directory exists
            // createDirectories is idempotent and safe to call even if directory exists
            Files.createDirectories(WORKSPACE_BASE);

            // Use temp directory for atomic creation
            Path tempDir = Files.createTempDirectory(WORKSPACE_BASE, key + ".tmp-");

            try {
                // Write package.json
                Files.write(
                        tempDir.resolve("package.json"),
                        packageJsonContent.getBytes(StandardCharsets.UTF_8)
                );

                // Yarn berry needs a yarnrc telling it to use node_modules instead of PnP,
                // so the symlinking pattern in Assertions.nodePackageManager() works.
                if (pm == PackageManager.YarnBerry) {
                    Files.write(
                            tempDir.resolve(".yarnrc.yml"),
                            "nodeLinker: node-modules\n".getBytes(StandardCharsets.UTF_8)
                    );
                }

                runInstall(tempDir, pm);

                // Move to final location (atomic on POSIX systems)
                try {
                    Files.move(tempDir, workspaceDir);
                } catch (IOException e) {
                    // The move can fail because another thread already created the workspace, or
                    // because a stale/corrupt directory (e.g. one missing package.json from an
                    // interrupted prior run) is occupying the target.
                    if (isWorkspaceValid(workspaceDir)) {
                        // Use the other thread's workspace
                        cleanupDirectory(tempDir);
                    } else {
                        // Target is stale/invalid; replace it with our freshly built workspace so a
                        // corrupt directory can't permanently wedge workspace creation.
                        cleanupDirectory(workspaceDir);
                        Files.move(tempDir, workspaceDir);
                    }
                }

                cache.put(key, workspaceDir);
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
            throw new RuntimeException("install was interrupted", e);
        }
    }

    /**
     * Per-PM full install (produces both lockfile and node_modules). The args
     * differ from {@link org.openrewrite.javascript.internal.LockFileRegeneration},
     * which uses lock-only flags ({@code --package-lock-only}, {@code --lockfile-only});
     * here we want a full install so that downstream tests have node_modules
     * available for type attribution and recipe round-trips.
     */
    private static void runInstall(Path workingDir, PackageManager pm)
            throws IOException, InterruptedException {
        PackageManagerExecutor executor;
        String[] args;
        switch (pm) {
            case Npm:
                executor = PackageManagerExecutor.NPM;
                args = new String[]{"install", "--silent"};
                break;
            case YarnClassic:
                executor = PackageManagerExecutor.YARN;
                args = new String[]{"install", "--ignore-scripts"};
                break;
            case YarnBerry:
                // Yarn Berry projects pin their version via the package.json "packageManager"
                // field and rely on Corepack to provision it. A global Yarn Classic refuses to
                // run such a project, so prefer Corepack when it is available and fall back to a
                // plain yarn otherwise (e.g. when the yarn on PATH is already a Corepack shim).
                String corepackExe = PackageManagerExecutor.COREPACK.find();
                if (corepackExe != null) {
                    executor = PackageManagerExecutor.COREPACK;
                    args = new String[]{"yarn", "install", "--mode", "skip-build"};
                } else {
                    executor = PackageManagerExecutor.YARN;
                    args = new String[]{"install", "--mode", "skip-build"};
                }
                break;
            case Pnpm:
                executor = PackageManagerExecutor.PNPM;
                args = new String[]{"install", "--ignore-scripts", "--no-strict-peer-dependencies"};
                break;
            case Bun:
                executor = PackageManagerExecutor.BUN;
                args = new String[]{"install", "--ignore-scripts"};
                break;
            default:
                throw new IllegalArgumentException("Unsupported package manager: " + pm);
        }
        String exe = executor.find();
        if (exe == null) {
            throw new RuntimeException(executor.getName() + " is not installed or not on PATH");
        }
        PackageManagerExecutor.RunResult result = executor.run(
                workingDir, exe, Collections.<String, String>emptyMap(), args);
        if (!result.isSuccess()) {
            throw new RuntimeException(executor.getName() + " install failed (exit "
                    + result.getExitCode() + "): " + result.getStderr());
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
                    .substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Checks if a workspace is valid (package.json present; node_modules may be absent for empty deps).
     */
    private static boolean isWorkspaceValid(Path workspaceDir) {
        return Files.isDirectory(workspaceDir) &&
                Files.exists(workspaceDir.resolve("package.json"));
    }

    /**
     * Cleans up a directory, ignoring errors.
     */
    private static void cleanupDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()) // Delete files before directories
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
        if (!Files.exists(WORKSPACE_BASE)) {
            return;
        }
        try (Stream<Path> entries = Files.list(WORKSPACE_BASE)) {
            entries.filter(Files::isDirectory)
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
                        String key = workspaceDir.getFileName().toString();
                        cache.put(key, workspaceDir);
                    });
        } catch (IOException e) {
            // Ignore - cache will be empty and workspaces will be created as needed
        }
    }
}
