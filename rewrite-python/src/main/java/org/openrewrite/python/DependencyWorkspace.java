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
class DependencyWorkspace {
    private static final Path WORKSPACE_BASE = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "openrewrite-python-workspaces"
    );
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

    private static void runCommand(Path dir, String... command) throws IOException, InterruptedException {
        // Find the uv executable
        String uvPath = findUvExecutable();
        if (uvPath == null) {
            throw new RuntimeException("uv is not installed. Install it with: pip install uv");
        }

        // Replace "uv" with the full path
        String[] resolvedCommand = new String[command.length];
        for (int i = 0; i < command.length; i++) {
            resolvedCommand[i] = "uv".equals(command[i]) ? uvPath : command[i];
        }

        ProcessBuilder pb = new ProcessBuilder(resolvedCommand);
        pb.directory(dir.toFile());
        pb.environment().put("VIRTUAL_ENV", dir.resolve(".venv").toString());
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(String.join(" ", command) + " failed with exit code: " + exitCode);
        }
    }

    private static @Nullable String uvPath = null;

    private static @Nullable String findUvExecutable() {
        if (uvPath != null) {
            return uvPath;
        }

        // Try to find project root by looking for settings.gradle.kts
        Path projectRoot = findProjectRoot();

        // Check common locations
        java.util.List<String> locations = new java.util.ArrayList<>();

        if (projectRoot != null) {
            // Project venv (most likely for this project)
            locations.add(projectRoot.resolve("rewrite-python/rewrite/.venv/bin/uv").toString());
        }

        // Relative to cwd
        locations.add(".venv/bin/uv");
        // User-level pip install
        locations.add(System.getProperty("user.home") + "/.local/bin/uv");
        // Homebrew on macOS
        locations.add("/opt/homebrew/bin/uv");
        locations.add("/usr/local/bin/uv");
        // Linux package managers
        locations.add("/usr/bin/uv");

        for (String location : locations) {
            Path path = Paths.get(location);
            if (Files.isExecutable(path)) {
                uvPath = path.toAbsolutePath().toString();
                return uvPath;
            }
        }

        // Try PATH as last resort
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "uv");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String output = sb.toString().trim();
            if (process.waitFor() == 0 && !output.isEmpty()) {
                uvPath = output;
                return uvPath;
            }
        } catch (IOException | InterruptedException e) {
            // Ignore
        }

        return null;
    }

    private static @Nullable Path findProjectRoot() {
        // Start from the current working directory and walk up
        Path current = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 20; i++) {
            // Look for settings.gradle.kts (Gradle multi-project root)
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            // Also check for settings.gradle (Groovy DSL)
            if (Files.exists(current.resolve("settings.gradle"))) {
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
                Files.exists(workspaceDir.resolve("pyproject.toml"));
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
                    .filter(DependencyWorkspace::isWorkspaceValid)
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
