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
package org.openrewrite.python.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.*;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import org.openrewrite.Parser;
import org.openrewrite.python.PyProjectTomlParser;
import org.openrewrite.toml.TomlParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
public class PythonRewriteRpc extends RewriteRpc {
    private static final RewriteRpcProcessManager<PythonRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    /**
     * The command used to start the RPC process. Useful for logging and diagnostics.
     */
    private final String command;
    private final Map<String, String> commandEnv;
    private final RewriteRpcProcess process;

    PythonRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers, String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace, resolvers);
        this.command = command;
        this.commandEnv = commandEnv;
        this.process = process;
    }

    public static @Nullable PythonRewriteRpc get() {
        return MANAGER.get();
    }

    public static PythonRewriteRpc getOrStart() {
        return MANAGER.getOrStart();
    }

    public static void setFactory(Builder builder) {
        MANAGER.setFactory(builder);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        process.shutdown();
    }

    public static void shutdownCurrent() {
        MANAGER.shutdown();
    }

    /**
     * Resets the cached state of the current Python RPC instance.
     * This clears all parsed objects and references on both the Java and Python sides,
     * preventing memory accumulation across multiple parse operations.
     * <p>
     * Call this between tests or after batch operations that don't need to share state.
     */
    public static void resetCurrent() {
        PythonRewriteRpc current = MANAGER.get();
        if (current != null) {
            current.reset();
        }
    }

    /**
     * Install recipes from a local file path (e.g., a local pip package).
     *
     * @param recipes Path to the local package directory
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes.getAbsoluteFile().toPath()),
                InstallRecipesResponse.class
        );
    }

    /**
     * Install recipes from a package name.
     *
     * @param packageName The package name to install
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    /**
     * Install recipes from a package name with a specific version.
     *
     * @param packageName The package name to install
     * @param version     Optional version specification
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        );
    }

    /**
     * Parses an entire Python project directory.
     * Discovers and parses all relevant source files.
     *
     * @param projectPath Path to the project directory to parse
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, ExecutionContext ctx) {
        return parseProject(projectPath, null, null, ctx);
    }

    /**
     * Parses an entire Python project directory.
     * Discovers and parses all relevant source files.
     *
     * @param projectPath Path to the project directory to parse
     * @param exclusions  Optional glob patterns to exclude from parsing
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, @Nullable List<String> exclusions, ExecutionContext ctx) {
        return parseProject(projectPath, exclusions, null, ctx);
    }

    /**
     * Parses an entire Python project directory.
     * Discovers and parses all relevant source files.
     *
     * @param projectPath Path to the project directory to parse
     * @param exclusions  Optional glob patterns to exclude from parsing
     * @param relativeTo  Optional path to make source file paths relative to
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        Stream<SourceFile> rpcStream = StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;
            private @Nullable ParseProjectResponse response;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (response == null) {
                    parsingListener.intermediateMessage("Starting project parsing: " + projectPath);
                    response = send("ParseProject", new ParseProject(projectPath, exclusions, relativeTo), ParseProjectResponse.class);
                    parsingListener.intermediateMessage(String.format("Discovered %,d files to parse", response.size()));
                }

                if (index >= response.size()) {
                    return false;
                }

                ParseProjectResponse.Item item = response.get(index);
                index++;

                SourceFile sourceFile;
                try {
                    sourceFile = getObject(item.getId(), item.getSourceFileType());
                    parsingListener.startedParsing(Parser.Input.fromFile(sourceFile.getSourcePath()));
                } catch (Exception e) {
                    sourceFile = new ParseError(
                            Tree.randomId(),
                            new Markers(Tree.randomId(), Collections.singletonList(
                                    ParseExceptionResult.build(PythonParser.class, e, null))),
                            Paths.get(item.getSourcePath()),
                            null,
                            StandardCharsets.UTF_8.name(),
                            false,
                            null,
                            e.getMessage(),
                            null
                    );
                }
                action.accept(sourceFile);
                return true;
            }

            @Override
            public @Nullable Spliterator<SourceFile> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return response == null ? Long.MAX_VALUE : response.size() - index;
            }

            @Override
            public int characteristics() {
                return response == null ? ORDERED : ORDERED | SIZED | SUBSIZED;
            }
        }, false);

        // For setup.py-only projects (no pyproject.toml, no setup.cfg),
        // attach the marker to the Py.CompilationUnit already in the RPC stream
        boolean hasPyproject = Files.exists(projectPath.resolve("pyproject.toml"));
        boolean hasSetupCfg = Files.exists(projectPath.resolve("setup.cfg"));
        boolean hasSetupPy = Files.exists(projectPath.resolve("setup.py"));

        if (!hasPyproject && !hasSetupCfg && hasSetupPy) {
            PythonResolutionResult marker = createSetupPyMarker(projectPath, relativeTo, ctx);
            if (marker != null) {
                final PythonResolutionResult finalMarker = marker;
                rpcStream = rpcStream.map(sf -> {
                    if (sf instanceof Py.CompilationUnit &&
                            sf.getSourcePath().getFileName().toString().equals("setup.py")) {
                        return sf.withMarkers(sf.getMarkers().addIfAbsent(finalMarker));
                    }
                    return sf;
                });
            }
        }

        Stream<SourceFile> manifestStream = parseManifest(projectPath, relativeTo, ctx);
        return Stream.concat(rpcStream, manifestStream);
    }

    private @Nullable PythonResolutionResult createSetupPyMarker(Path projectPath, @Nullable Path relativeTo, ExecutionContext ctx) {
        Path setupPyPath = projectPath.resolve("setup.py");
        if (!Files.exists(setupPyPath)) {
            return null;
        }

        String source;
        try {
            source = new String(Files.readAllBytes(setupPyPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }

        Path workspace = DependencyWorkspace.getOrCreateSetuptoolsWorkspace(source, projectPath);
        if (workspace == null) {
            return null;
        }

        List<ResolvedDependency> resolvedDeps = RequirementsTxtParser.parseFreezeOutput(workspace);
        if (resolvedDeps.isEmpty()) {
            return null;
        }

        List<Dependency> deps = RequirementsTxtParser.dependenciesFromResolved(resolvedDeps);

        Path effectiveRelativeTo = relativeTo != null ? relativeTo : projectPath;
        String path = effectiveRelativeTo.relativize(setupPyPath).toString();

        return new PythonResolutionResult(
                org.openrewrite.Tree.randomId(),
                null,
                null,
                null,
                null,
                path,
                null,
                null,
                Collections.emptyList(),
                deps,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                resolvedDeps,
                PythonResolutionResult.PackageManager.Uv,
                null
        );
    }

    private Stream<SourceFile> parseManifest(Path projectPath, @Nullable Path relativeTo, ExecutionContext ctx) {
        Path effectiveRelativeTo = relativeTo != null ? relativeTo : projectPath;

        // Priority: pyproject.toml > setup.cfg > requirements.txt
        // Note: setup.py is NOT handled here — it's already in the RPC stream as Py.CompilationUnit

        Path pyprojectPath = projectPath.resolve("pyproject.toml");
        if (Files.exists(pyprojectPath)) {
            Parser.Input pyprojectInput = Parser.Input.fromFile(pyprojectPath);
            Stream<SourceFile> result = new PyProjectTomlParser().parseInputs(
                    Collections.singletonList(pyprojectInput), effectiveRelativeTo, ctx);

            Path uvLockPath = projectPath.resolve("uv.lock");
            if (Files.exists(uvLockPath)) {
                Parser.Input uvLockInput = Parser.Input.fromFile(uvLockPath);
                Stream<SourceFile> uvLockStream = new TomlParser().parseInputs(
                        Collections.singletonList(uvLockInput), effectiveRelativeTo, ctx);
                result = Stream.concat(result, uvLockStream);
            }
            return result;
        }

        Path setupCfgPath = projectPath.resolve("setup.cfg");
        if (Files.exists(setupCfgPath)) {
            Parser.Input input = Parser.Input.fromFile(setupCfgPath);
            return new SetupCfgParser().parseInputs(
                    Collections.singletonList(input), effectiveRelativeTo, ctx);
        }

        RequirementsTxtParser reqsParser = new RequirementsTxtParser();
        try (Stream<Path> entries = Files.list(projectPath)) {
            Path reqsPath = entries
                    .filter(p -> reqsParser.accept(p.getFileName()))
                    .findFirst()
                    .orElse(null);
            if (reqsPath != null) {
                Parser.Input input = Parser.Input.fromFile(reqsPath);
                return reqsParser.parseInputs(
                        Collections.singletonList(input), effectiveRelativeTo, ctx);
            }
        } catch (IOException e) {
            // Silently skip manifest parsing if we can't list the directory
        }

        return Stream.empty();
    }

    public static Builder builder() {
        return new Builder();
    }

    @RequiredArgsConstructor
    public static class Builder implements Supplier<PythonRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private List<RecipeBundleResolver> resolvers = Collections.emptyList();
        private final Map<String, String> environment = new HashMap<>();
        // Default to looking for a venv python, falling back to system python
        private Path pythonPath = findDefaultPythonPath();
        private @Nullable Path log;
        private @Nullable Path pipPackagesPath;
        private @Nullable Path recipeInstallDir;

        private static Path findDefaultPythonPath() {
            // Try to find a venv in the project structure
            Path basePath = Paths.get(System.getProperty("user.dir"));
            Path[] searchPaths = {
                // From rewrite root dir
                basePath.resolve("rewrite-python/rewrite/.venv/bin/python"),
                // From rewrite-python dir
                basePath.resolve("rewrite/.venv/bin/python"),
                // From rewrite-python/rewrite dir
                basePath.resolve(".venv/bin/python")
            };

            for (Path path : searchPaths) {
                if (Files.exists(path)) {
                    return path;
                }
            }
            return Paths.get("python3");
        }
        private @Nullable Path metricsCsv;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean traceRpcMessages;

        private @Nullable Integer debugPort;
        private @Nullable Path debugRewriteSourcePath;

        private @Nullable Path workingDirectory;

        /**
         * The Python language version to parse. Defaults to "3" (Python 3).
         * Set to "2" or "2.7" to parse Python 2 code.
         */
        private String pythonVersion = "3";

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder resolvers(List<RecipeBundleResolver> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        /**
         * Path to the Python executable.
         *
         * @param pythonPath The path to the Python executable (e.g., "python3", "/usr/bin/python3")
         * @return This builder
         */
        public Builder pythonPath(Path pythonPath) {
            this.pythonPath = pythonPath;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder log(@Nullable Path log) {
            this.log = log;
            return this;
        }

        public Builder metricsCsv(@Nullable Path metricsCsv) {
            this.metricsCsv = metricsCsv;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment.putAll(environment);
            return this;
        }

        public Builder traceRpcMessages(boolean verboseLogging) {
            this.traceRpcMessages = verboseLogging;
            return this;
        }

        public Builder traceRpcMessages() {
            return traceRpcMessages(true);
        }

        /**
         * Set the port for Python debugger to listen on.
         *
         * @param rewriteSourcePath The path to the Python Rewrite source code
         * @param debugPort         The port for the debugger to listen on
         * @return This builder
         */
        public Builder debugPort(Path rewriteSourcePath, int debugPort) {
            this.debugPort = debugPort;
            this.debugRewriteSourcePath = rewriteSourcePath;
            return this;
        }

        /**
         * Set the working directory for the Python process.
         *
         * @param workingDirectory The working directory for the Python process
         * @return This builder
         */
        public Builder workingDirectory(@Nullable Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        /**
         * Set the base pip packages directory.
         * When set and the required release version is not already available in the
         * Python interpreter, a version-specific subdirectory (e.g.,
         * {@code <pipPackagesPath>/8.74.1/}) is resolved and the openrewrite package is
         * automatically installed there via pip. Dev builds ({@code .dev0}) are not
         * installed this way and require the interpreter to already have the package.
         *
         * @param pipPackagesPath The base directory under which version-specific pip packages are installed
         * @return This builder
         */
        public Builder pipPackagesPath(@Nullable Path pipPackagesPath) {
            this.pipPackagesPath = pipPackagesPath;
            return this;
        }

        /**
         * Set the directory where user-installed recipe packages live.
         * This directory is added to PYTHONPATH so the RPC server can
         * find recipe packages installed via pip.
         *
         * @param recipeInstallDir The directory containing recipe pip packages
         * @return This builder
         */
        public Builder recipeInstallDir(@Nullable Path recipeInstallDir) {
            this.recipeInstallDir = recipeInstallDir;
            return this;
        }

        /**
         * Set the Python language version to parse.
         * <p>
         * Supported values:
         * <ul>
         *   <li>"2" or "2.7" - Parse Python 2.7 code using parso</li>
         *   <li>"3" (default) - Parse Python 3 code using the standard ast module</li>
         * </ul>
         *
         * @param pythonVersion The Python version to parse (e.g., "2", "2.7", "3")
         * @return This builder
         */
        public Builder pythonVersion(String pythonVersion) {
            this.pythonVersion = pythonVersion;
            return this;
        }

        @Override
        public PythonRewriteRpc get() {
            String version = StringUtils.readFully(
                    PythonRewriteRpc.class.getResourceAsStream("/META-INF/rewrite-python-version.txt")).trim();
            boolean isDevBuild = version.isEmpty() || version.endsWith(".dev0") || "unspecified".equals(version);

            Path resolvedPipPackagesPath = null;
            if (!isDevBuild) {
                // Known version (release or published pre-release) — try to find or
                // install the pinned version, falling back to any available install.
                if (pipPackagesPath != null && isVersionInstalled(pipPackagesPath.resolve(version), version)) {
                    resolvedPipPackagesPath = pipPackagesPath.resolve(version);
                } else if (hasRewriteVersion(pythonPath, version)) {
                    // Interpreter already has the right version; nothing to do
                } else if (pipPackagesPath != null) {
                    resolvedPipPackagesPath = pipPackagesPath.resolve(version);
                    bootstrapOpenrewrite(resolvedPipPackagesPath, version);
                } else if (canImportRewrite(pythonPath)) {
                    // Interpreter has a different version, but no pipPackagesPath to
                    // install the right one — proceed with what's available (e.g., CI
                    // running tests against a venv with an editable install)
                } else {
                    throw new IllegalStateException(
                            "The Python interpreter at " + pythonPath + " does not have openrewrite " + version + ". " +
                            "Either set pipPackagesPath to allow automatic installation, " +
                            "or install the package manually: pip install openrewrite==" + version);
                }
            } else {
                // Local dev build — require the interpreter to already have the rewrite
                // package (e.g., from a venv with an editable install).
                if (!canImportRewrite(pythonPath)) {
                    throw new IllegalStateException(
                            "The Python interpreter at " + pythonPath + " cannot import the 'rewrite' package. " +
                            "For development builds, run 'uv sync --extra dev' in the rewrite-python/rewrite/ " +
                            "directory to set up an editable install, then configure pythonPath to point to " +
                            "the venv's Python executable.");
                }
            }

            Stream<@Nullable String> cmd;

            // Use python -m rewrite.rpc.server to start the RPC server
            cmd = Stream.of(
                    pythonPath.toString(),
                    "-m", "rewrite.rpc.server",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    metricsCsv == null ? null : "--metrics-csv=" + metricsCsv.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );

            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }

            process.environment().putAll(environment);

            // Set the Python version for the parser
            process.environment().put("REWRITE_PYTHON_VERSION", pythonVersion);

            // Set up PYTHONPATH for the rewrite package
            List<String> pythonPathParts = new ArrayList<>();

            // Add pip packages path if the interpreter doesn't already have rewrite
            if (resolvedPipPackagesPath != null) {
                pythonPathParts.add(resolvedPipPackagesPath.toAbsolutePath().normalize().toString());
            }

            // Add recipe install directory to PYTHONPATH
            if (recipeInstallDir != null) {
                pythonPathParts.add(recipeInstallDir.toAbsolutePath().normalize().toString());
            }

            // If debug source path is set, use it
            if (debugRewriteSourcePath != null) {
                pythonPathParts.add(debugRewriteSourcePath.toAbsolutePath().normalize().toString());
            } else if (isDevBuild) {
                // For local dev builds, try to find the Python source in the project
                // structure (as a fallback for PYTHONPATH)
                Path basePath = workingDirectory != null ? workingDirectory : Paths.get(System.getProperty("user.dir"));

                // Check common locations
                Path[] searchPaths = {
                    basePath.resolve("rewrite-python/rewrite/src"),
                    basePath.resolve("rewrite/src"),
                    basePath.getParent() != null ? basePath.getParent().resolve("rewrite-python/rewrite/src") : null
                };

                for (Path searchPath : searchPaths) {
                    if (searchPath != null && Files.exists(searchPath.resolve("rewrite"))) {
                        pythonPathParts.add(searchPath.toAbsolutePath().normalize().toString());
                        break;
                    }
                }
            }

            // Add existing PYTHONPATH
            String existingPath = System.getenv("PYTHONPATH");
            if (existingPath != null && !existingPath.isEmpty()) {
                pythonPathParts.add(existingPath);
            }

            if (!pythonPathParts.isEmpty()) {
                process.environment().put("PYTHONPATH", String.join(File.pathSeparator, pythonPathParts));
            }

            process.start();

            try {
                return (PythonRewriteRpc) new PythonRewriteRpc(process, marketplace, resolvers,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Checks whether the given Python interpreter can import the rewrite package
         * (any version) without PYTHONPATH modifications.
         */
        private static boolean canImportRewrite(Path pythonPath) {
            try {
                Process probe = new ProcessBuilder(
                        pythonPath.toString(), "-c", "import rewrite"
                ).redirectErrorStream(true).start();
                try (InputStream is = probe.getInputStream()) {
                    //noinspection StatementWithEmptyBody
                    while (is.read() != -1) {
                    }
                }
                return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0;
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }

        /**
         * Checks whether the given Python interpreter has a specific version of the
         * openrewrite package installed.
         */
        private static boolean hasRewriteVersion(Path pythonPath, String version) {
            try {
                Process probe = new ProcessBuilder(
                        pythonPath.toString(), "-c",
                        "from importlib.metadata import version; print(version('openrewrite'))"
                ).redirectErrorStream(true).start();
                String output;
                try (InputStream is = probe.getInputStream()) {
                    output = StringUtils.readFully(is).trim();
                }
                return probe.waitFor(10, TimeUnit.SECONDS) && probe.exitValue() == 0
                        && version.equals(output);
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }

        /**
         * Checks whether the version-specific pip packages directory already has the
         * correct version installed, based on a marker file.
         */
        private static boolean isVersionInstalled(Path pipPackagesPath, String version) {
            if (!Files.exists(pipPackagesPath.resolve("rewrite"))) {
                return false;
            }
            Path versionMarker = pipPackagesPath.resolve(".openrewrite-version");
            try {
                return Files.exists(versionMarker) &&
                        version.equals(new String(Files.readAllBytes(versionMarker), StandardCharsets.UTF_8).trim());
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * Installs the pinned openrewrite package into the given pip packages directory.
         */
        private void bootstrapOpenrewrite(Path pipPackagesPath, String version) {
            try {
                Files.createDirectories(pipPackagesPath);

                ProcessBuilder pb = new ProcessBuilder(
                        pythonPath.toString(),
                        "-m", "pip", "install",
                        "--upgrade",
                        "--target=" + pipPackagesPath.toAbsolutePath().normalize(),
                        "openrewrite==" + version
                );
                pb.redirectErrorStream(true);
                if (log != null) {
                    File logFile = log.toAbsolutePath().normalize().toFile();
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
                }
                Process process = pb.start();
                String pipOutput = "";
                if (log == null) {
                    // Capture stdout+stderr so we can include it in error messages
                    try (InputStream is = process.getInputStream()) {
                        pipOutput = StringUtils.readFully(is);
                    }
                }
                boolean completed = process.waitFor(2, TimeUnit.MINUTES);

                if (!completed) {
                    process.destroyForcibly();
                    throw new RuntimeException("Timed out bootstrapping openrewrite==" + version);
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String message = "Failed to install openrewrite==" + version +
                            " (pip exited with code " + exitCode + ")";
                    if (!pipOutput.isEmpty()) {
                        message += ":\n" + pipOutput.trim();
                    } else if (log != null) {
                        message += ". See " + log.toAbsolutePath().normalize() + " for details";
                    }
                    throw new RuntimeException(message);
                }

                Files.write(pipPackagesPath.resolve(".openrewrite-version"), version.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to bootstrap openrewrite package", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while bootstrapping openrewrite package", e);
            }
        }
    }
}
