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

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.DependencyManifest;
import org.openrewrite.python.internal.DependencyManifest.Requirements;
import org.openrewrite.python.internal.DependencyManifest.SetupPyReader;
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.internal.PackageManagerExecutor;
import org.openrewrite.python.internal.PackageManagerExecutor.RunResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;

/**
 * Installs the dependencies a Python project declares into a virtual environment with uv, so that ty
 * resolves the types that reach into them. {@link #provision} is the entry point. The project's
 * dependency markers are computed from the same {@link DependencyManifest}, so they agree with the env.
 */
@UtilityClass
public class DependencyWorkspace {
    /**
     * Installed venvs are expensive to rebuild and are meant to outlive a single run, so they live
     * beside the other {@code ~/.rewrite} caches. A {@code $TMPDIR} reaper evicts by access time,
     * which uv's installed files inherit from their wheel rather than from the install, and would
     * empty a workspace the same night it was created. Bounded by {@link #MAX_CACHE_SIZE}.
     */
    private static final Path WORKSPACE_BASE = Paths.get(
            System.getProperty("user.home"),
            ".rewrite",
            "python-workspaces"
    );
    /**
     * Bump this when the workspace layout changes (e.g. new files expected)
     * so that stale cached workspaces are automatically recreated.
     */
    private static final String WORKSPACE_VERSION = "2";
    private static final int MAX_CACHE_SIZE = 100;
    /**
     * Holds the hash of what an env was installed from and how many packages that installed. It is written
     * once the install has succeeded.
     */
    private static final String INSTALLED = "installed.txt";
    private static final Map<Path, Object> WORKSPACE_LOCKS = new ConcurrentHashMap<>();
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
     * What a host decides about an install. Every field is optional.
     */
    @Value
    @Builder
    @With
    public static class Options {
        /**
         * The uv executable, when it is not the one on {@code PATH}.
         */
        @Nullable
        Path uv;

        /**
         * Extra environment for every uv command, such as a proxy, certificates or a default index.
         */
        @Builder.Default
        Map<String, String> environment = emptyMap();

        /**
         * When the last uv command has to have finished. Without one, each command may run for two minutes.
         */
        @Nullable
        Instant deadline;

        /**
         * A directory the host keeps for this project's env, which holds it as {@code .venv} beside the files
         * it is installed from. Apart from uv's own cache, an install writes nowhere else. Without one, the
         * env lives under {@code ~/.rewrite}, in a directory named for what it is installed from.
         */
        @Nullable
        Path workspace;

        /**
         * Where the search for a lock above the project stops, the same directory the parse's source
         * paths are relative to.
         */
        @Nullable
        Path relativeTo;

        /**
         * A file that each uv command and its output are appended to.
         */
        @Nullable
        Path log;
    }

    /**
     * What one install produced, for the host to forward and record.
     */
    @Value
    public static class Result {
        /**
         * The virtual environment to parse with as {@code dependencyPath}, or null when there is none.
         */
        @Nullable
        Path env;

        /**
         * The env's Python minor version, such as {@code 3.12}.
         */
        @Nullable
        String pythonVersion;

        /**
         * The manifest the env is installed from, such as "requirements files", or null when the
         * project declares no dependencies.
         */
        @Nullable
        String manifest;

        /**
         * Why the install failed, for a host to record as a tolerated failure, or null when it did not.
         */
        @Nullable
        String failure;
    }

    /**
     * Installs the dependencies {@code projectDir} declares into a virtual environment. A project
     * without a manifest, or without uv to install it with, gets no env and no failure.
     */
    public static Result provision(Path projectDir, Options options) {
        DependencyManifest manifest = DependencyManifest.choose(projectDir);
        if (manifest == null) {
            return new Result(null, null, null, null);
        }
        return install(manifest, setupPy -> manifest.read(projectDir, options.getRelativeTo(),
                options.getEnvironment(), setupPy), options);
    }

    /**
     * The requirements an env that {@link #provision} created was installed from, or null for an env it
     * did not create.
     */
    public static @Nullable List<String> installedRequirements(Path env) {
        Path workspace = env.toAbsolutePath().normalize().getParent();
        if (workspace == null || !Files.isRegularFile(workspace.resolve(INSTALLED))) {
            return null;
        }
        try {
            return Files.readAllLines(workspace.resolve("requirements.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    static Result install(DependencyManifest manifest, Function<SetupPyReader, Requirements> read, Options options) {
        String uv = options.getUv() != null ? options.getUv().toString() : PackageManagerExecutor.UV.find();
        if (uv == null) {
            log(options, "uv is not available, so the " + manifest.getDescription() + " get no dependency environment");
            return new Result(null, null, manifest.getDescription(), null);
        }
        Options withUv = options.withUv(Paths.get(uv));
        List<Path> temporaryWorkspaces = new ArrayList<>();
        try {
            Requirements requirements = read.apply((projectDir, extras) ->
                    compileSetupPy(projectDir, extras, withUv, temporaryWorkspaces));
            return install(requirements, withUv);
        } catch (RuntimeException e) {
            return failed(options, manifest, e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            temporaryWorkspaces.forEach(DependencyWorkspace::cleanupDirectory);
        }
    }

    /**
     * A host's workspace is installed into in place. A default one is shared with other processes, so it is
     * built aside and moved into place whole.
     */
    private static Result install(Requirements requirements, Options options) {
        String python = DependencyManifest.pythonVersion(requirements.getRequiresPython());
        if (python == null) {
            return failed(options, requirements.getManifest(), "No Python from 3.8 up satisfies " +
                                                               String.join(" and ", requirements.getRequiresPython()));
        }
        String hash = hashContent(installedFrom(requirements, python));
        try {
            Path workspace = options.getWorkspace();
            if (workspace != null) {
                Object lock = WORKSPACE_LOCKS.computeIfAbsent(workspace.toAbsolutePath().normalize(), w -> new Object());
                synchronized (lock) {
                    String failure = isInstalled(workspace, hash) ? null :
                            installInto(workspace, requirements, python, hash, options);
                    if (failure != null) {
                        return failed(options, requirements.getManifest(), failure);
                    }
                }
            } else {
                workspace = WORKSPACE_BASE.resolve("env-" + hash);
                if (!isInstalled(workspace, hash)) {
                    Path built = Files.createTempDirectory(Files.createDirectories(WORKSPACE_BASE),
                            "env-" + hash + ".tmp-");
                    try {
                        String failure = installInto(built, requirements, python, hash, options);
                        if (failure != null) {
                            return failed(options, requirements.getManifest(), failure);
                        }
                        moveIntoPlace(built, workspace, hash);
                    } finally {
                        cleanupDirectory(built);
                    }
                }
                cache.put(workspace.getFileName().toString(), workspace);
            }
            Path venv = workspace.resolve(".venv");
            return new Result(venv, venvPython(venv, python), requirements.getManifest().getDescription(), null);
        } catch (IOException e) {
            return failed(options, requirements.getManifest(), e.toString());
        }
    }

    private static void moveIntoPlace(Path built, Path workspace, String hash) throws IOException {
        if (isInstalled(workspace, hash)) {
            return;
        }
        cleanupDirectory(workspace);
        try {
            Files.move(built, workspace);
        } catch (IOException e) {
            if (!isInstalled(workspace, hash)) {
                throw e;
            }
        }
    }

    /**
     * Installs into {@code workspace} and returns why that failed, or null when it did not. A lock's pins are
     * constraints, so the roots decide what is installed and the lock decides at which version. {@code --exact}
     * removes what a reused env no longer needs.
     */
    private static @Nullable String installInto(Path workspace, Requirements requirements, String python,
                                                String hash, Options options) throws IOException {
        Files.createDirectories(workspace);
        Files.deleteIfExists(workspace.resolve(INSTALLED));
        stopSettingsDiscovery(workspace);
        Files.write(workspace.resolve("requirements.txt"), requirements.getRoots(), StandardCharsets.UTF_8);
        Files.write(workspace.resolve("constraints.txt"), requirements.getConstraints(), StandardCharsets.UTF_8);
        Files.write(workspace.resolve("overrides.txt"), requirements.getOverrides(), StandardCharsets.UTF_8);

        Path venv = workspace.resolve(".venv");
        boolean empty = requirements.getRoots().isEmpty();
        String existingPython = venvPython(venv, null);
        if (existingPython != null && (empty || !existingPython.equals(python) || !InstalledEnvParser.isIntact(venv))) {
            // A venv keeps the interpreter it was created with.
            // uv cannot install over a package whose files went missing.
            cleanupDirectory(venv);
        }
        RunResult result = run(workspace, options, options.getEnvironment(),
                "venv", ".venv", "--allow-existing", "--python", python);
        if (result.isSuccess() && !empty) {
            List<String> args = new ArrayList<>(Arrays.asList("pip", "install", "--python", ".venv", "--exact",
                    "-r", "requirements.txt"));
            if (!requirements.getConstraints().isEmpty()) {
                args.addAll(Arrays.asList("-c", "constraints.txt"));
            }
            if (!requirements.getOverrides().isEmpty()) {
                args.addAll(Arrays.asList("--overrides", "overrides.txt"));
            }
            result = run(workspace, options, withIndexes(options.getEnvironment(), requirements.getIndexes()),
                    args.toArray(new String[0]));
        }
        if (!result.isSuccess()) {
            return result.getExitCode() < 0 ? result.getStderr().trim() : "uv exited with code " + result.getExitCode();
        }
        Files.write(workspace.resolve(INSTALLED), Arrays.asList(hash, String.valueOf(installedPackages(venv))),
                StandardCharsets.UTF_8);
        return null;
    }

    /**
     * Project indexes go in through {@code UV_INDEX}, so that credentials stay out of the logged command lines.
     * They come before any the host set, and uv takes each package from the first index that has it.
     */
    private static Map<String, String> withIndexes(Map<String, String> environment, List<String> indexes) {
        if (indexes.isEmpty()) {
            return environment;
        }
        List<String> all = new ArrayList<>(indexes);
        String hostIndexes = environment.containsKey("UV_INDEX") ? environment.get("UV_INDEX") :
                System.getenv("UV_INDEX");
        if (hostIndexes != null && !hostIndexes.trim().isEmpty()) {
            all.add(hostIndexes.trim());
        }
        Map<String, String> withIndexes = new HashMap<>(environment);
        withIndexes.put("UV_INDEX", String.join(" ", all));
        return withIndexes;
    }

    /**
     * Reads a setup.py's requirements with {@code uv pip compile}, which runs setuptools' {@code egg_info}.
     * setuptools writes {@code <name>.egg-info} into the project unless {@code DIST_EXTRA_CONFIG}, which it
     * reads after the project's own setup.cfg, points {@code egg_base} into the workspace. Each root comes
     * out pinned to the version uv picked for it.
     */
    private static List<String> compileSetupPy(Path projectDir, List<String> extras, Options options,
                                               List<Path> temporaryWorkspaces) {
        try {
            Path workspace = options.getWorkspace();
            if (workspace == null) {
                if (temporaryWorkspaces.isEmpty()) {
                    temporaryWorkspaces.add(Files.createTempDirectory(Files.createDirectories(WORKSPACE_BASE),
                            "read.tmp-"));
                }
                workspace = temporaryWorkspaces.get(0);
            }
            stopSettingsDiscovery(workspace);
            Path eggBase = Files.createDirectories(workspace.resolve("egg-info"));
            Path distExtraConfig = workspace.resolve("dist-extra.cfg");
            Files.write(distExtraConfig, ("[egg_info]\negg_base = " + eggBase + "\n").getBytes(StandardCharsets.UTF_8));
            Map<String, String> environment = new HashMap<>(options.getEnvironment());
            environment.put("DIST_EXTRA_CONFIG", distExtraConfig.toString());
            List<String> args = new ArrayList<>(Arrays.asList("pip", "compile", "--no-deps", "--universal",
                    "--no-strip-extras", "--no-header", "--no-annotate",
                    "--python-version", requireNonNull(DependencyManifest.pythonVersion(emptyList()))));
            for (String extra : extras) {
                args.addAll(Arrays.asList("--extra", extra));
            }
            args.add(projectDir.resolve("setup.py").toString());
            RunResult result = run(workspace, options, environment, args.toArray(new String[0]));
            if (!result.isSuccess()) {
                throw new IllegalStateException("Reading " + projectDir.resolve("setup.py") + " failed: " +
                                                (result.getExitCode() < 0 ? result.getStderr().trim() :
                                                        "uv exited with code " + result.getExitCode()));
            }
            List<String> roots = new ArrayList<>();
            for (String line : result.getStdout().split("\\r?\\n")) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    roots.add(line.trim());
                }
            }
            return roots;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RunResult run(Path workspace, Options options, Map<String, String> environment, String... args)
            throws IOException {
        try {
            return PackageManagerExecutor.UV.run(workspace, requireNonNull(options.getUv()).toString(), environment,
                    options.getDeadline(), options.getLog(), args);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(false, -1, "", "Interrupted while running uv " + args[0]);
        }
    }

    /**
     * uv runs in the workspace, and a {@code [tool.uv]} table there ends its search of the directories
     * above for a project and its settings. A repository's own {@code required-version} then cannot fail
     * the install, while user-level settings still apply. A {@code uv.toml} does not stop uv from
     * discovering an ancestor that has a {@code [project]} table.
     */
    private static void stopSettingsDiscovery(Path workspace) throws IOException {
        Files.createDirectories(workspace);
        Files.write(workspace.resolve("pyproject.toml"), "[tool.uv]\n".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Index credentials reach the disk only as part of the hash.
     */
    private static String installedFrom(Requirements requirements, String python) {
        return WORKSPACE_VERSION + "\n" + requirements.getManifest() + "\n" + python + "\n" +
               requirements.getRoots() + "\n" + requirements.getConstraints() + "\n" +
               requirements.getOverrides() + "\n" + requirements.getIndexes();
    }

    /**
     * Whether {@code workspace} holds an env installed from what {@code hash} names. An env that installed no
     * packages has none to lose, while one whose packages went missing from outside is installed again.
     */
    private static boolean isInstalled(Path workspace, String hash) {
        List<String> installed;
        try {
            installed = Files.readAllLines(workspace.resolve(INSTALLED), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return false;
        }
        Path venv = workspace.resolve(".venv");
        return installed.size() == 2 && hash.equals(installed.get(0)) &&
               Files.isRegularFile(venv.resolve("pyvenv.cfg")) &&
               ("0".equals(installed.get(1)) || InstalledEnvParser.isIntact(venv));
    }

    private static int installedPackages(Path venv) throws IOException {
        Path sitePackages = InstalledEnvParser.findSitePackages(venv);
        if (sitePackages == null) {
            return 0;
        }
        int count = 0;
        try (DirectoryStream<Path> distInfos = Files.newDirectoryStream(sitePackages, "*.dist-info")) {
            for (Path ignored : distInfos) {
                count++;
            }
        }
        return count;
    }

    /**
     * The minor version of the Python a venv was created with, as its {@code pyvenv.cfg} records it.
     */
    private static @Nullable String venvPython(Path venv, @Nullable String orElse) {
        try {
            for (String line : Files.readAllLines(venv.resolve("pyvenv.cfg"), StandardCharsets.UTF_8)) {
                String[] keyValue = line.split("=", 2);
                String key = keyValue[0].trim();
                if (keyValue.length == 2 && ("version_info".equals(key) || "version".equals(key))) {
                    String[] parts = keyValue[1].trim().split("\\.");
                    if (parts.length >= 2) {
                        return parts[0] + "." + parts[1];
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return orElse;
    }

    private static Result failed(Options options, DependencyManifest manifest, String failure) {
        log(options, "The " + manifest.getDescription() + " get no dependency environment: " + failure);
        return new Result(null, null, manifest.getDescription(), failure);
    }

    private static void log(Options options, String message) {
        PackageManagerExecutor.appendToLog(options.getLog(), message + "\n");
    }

    /**
     * Gets or creates a workspace directory for the given pyproject.toml content.
     * Workspaces are cached by content hash to avoid repeated installations.
     *
     * @param pyprojectContent The complete pyproject.toml file content
     * @return Path to the workspace directory containing .venv
     */
    static Path getOrCreateWorkspace(String pyprojectContent) {
        return getOrCreateWorkspace(pyprojectContent, emptyMap());
    }

    /**
     * Gets or creates a workspace directory for the given pyproject.toml content.
     * Workspaces are cached by content hash to avoid repeated installations.
     *
     * @param pyprojectContent The complete pyproject.toml file content
     * @param environment      additional environment variables for subprocess (e.g., SSL_CERT_FILE)
     * @return Path to the workspace directory containing .venv
     */
    static Path getOrCreateWorkspace(String pyprojectContent, Map<String, String> environment) {
        String hash = hashContent(pyprojectContent);

        // Check in-memory cache
        Path cached = cache.get(hash);
        if (cached != null) {
            if (isWorkspaceValid(cached)) {
                return cached;
            }
            cache.remove(hash);
        }

        // Check disk cache
        Path workspaceDir = WORKSPACE_BASE.resolve(hash);
        if (isWorkspaceValid(workspaceDir)) {
            cache.put(hash, workspaceDir);
            return workspaceDir;
        }
        // An invalid leftover at the target path (e.g. from an interrupted run) would
        // block the final Files.move into place; remove it before rebuilding.
        cleanupDirectory(workspaceDir);

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
                runCommand(tempDir, environment, "uv", "sync");

                // Install ty for type stubs
                runCommand(tempDir, environment, "uv", "pip", "install", "ty");

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

    private static void runCommand(Path dir, Map<String, String> environment, String... command) throws IOException, InterruptedException {
        String uvPath = PackageManagerExecutor.UV.find();
        if (uvPath == null) {
            throw new RuntimeException("uv is not installed. Install it with: pip install uv");
        }

        // The first element of command is "uv", strip it and pass the rest as args
        String[] args = new String[command.length - 1];
        System.arraycopy(command, 1, args, 0, args.length);

        PackageManagerExecutor.RunResult result = PackageManagerExecutor.UV.run(dir, uvPath, environment, args);
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
                InstalledEnvParser.isIntact(workspaceDir.resolve(".venv")) &&
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

    static void clearCache() {
        cache.clear();
    }

    private static void initializeCacheFromDisk() {
        if (!Files.exists(WORKSPACE_BASE)) {
            return;
        }
        try (Stream<Path> entries = Files.list(WORKSPACE_BASE)) {
            entries.filter(Files::isDirectory)
                    .filter(dir -> !dir.getFileName().toString().contains(".tmp-"))
                    .filter(dir -> {
                        String name = dir.getFileName().toString();
                        return name.startsWith("env-") ? Files.isRegularFile(dir.resolve(INSTALLED)) :
                                isWorkspaceValid(dir);
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
