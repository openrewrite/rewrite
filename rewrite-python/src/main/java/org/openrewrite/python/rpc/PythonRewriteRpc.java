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
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;
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

    PythonRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace);
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

                SourceFile sourceFile = getObject(item.getId(), item.getSourceFileType());
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

        // Parse pyproject.toml and uv.lock if present
        Path pyprojectPath = projectPath.resolve("pyproject.toml");
        if (Files.exists(pyprojectPath)) {
            Path effectiveRelativeTo = relativeTo != null ? relativeTo : projectPath;
            Parser.Input pyprojectInput = Parser.Input.fromFile(pyprojectPath);
            Stream<SourceFile> pyprojectStream = new PyProjectTomlParser().parseInputs(
                    Collections.singletonList(pyprojectInput), effectiveRelativeTo, ctx);
            rpcStream = Stream.concat(rpcStream, pyprojectStream);

            Path uvLockPath = projectPath.resolve("uv.lock");
            if (Files.exists(uvLockPath)) {
                Parser.Input uvLockInput = Parser.Input.fromFile(uvLockPath);
                Stream<SourceFile> uvLockStream = new TomlParser().parseInputs(
                        Collections.singletonList(uvLockInput), effectiveRelativeTo, ctx);
                rpcStream = Stream.concat(rpcStream, uvLockStream);
            }
        }
        return rpcStream;
    }

    public static Builder builder() {
        return new Builder();
    }

    @RequiredArgsConstructor
    public static class Builder implements Supplier<PythonRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private final Map<String, String> environment = new HashMap<>();
        // Default to looking for a venv python, falling back to system python
        private Path pythonPath = findDefaultPythonPath();
        private @Nullable Path log;
        private @Nullable Path pipPackagesPath;

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
         * Set the pip packages directory for recipe installations.
         * When set, this directory will be added to PYTHONPATH and the openrewrite
         * package will be automatically installed if not present.
         *
         * @param pipPackagesPath The directory where pip packages are installed
         * @return This builder
         */
        public Builder pipPackagesPath(@Nullable Path pipPackagesPath) {
            this.pipPackagesPath = pipPackagesPath;
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
            // Bootstrap openrewrite package if pip packages path is set
            if (pipPackagesPath != null) {
                bootstrapOpenrewrite(pipPackagesPath);
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

            // Add pip packages path first if set (takes priority)
            if (pipPackagesPath != null) {
                pythonPathParts.add(pipPackagesPath.toAbsolutePath().normalize().toString());
            }

            // If debug source path is set, use it
            if (debugRewriteSourcePath != null) {
                pythonPathParts.add(debugRewriteSourcePath.toAbsolutePath().normalize().toString());
            } else if (pipPackagesPath == null) {
                // Only search for development source if pip packages path is not set
                // Try to find the Python source in the project structure
                // Look for rewrite-python/rewrite/src relative to the working directory
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
                return (PythonRewriteRpc) new PythonRewriteRpc(process, marketplace,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Ensures the openrewrite Python package is installed in the pip packages directory.
         * This is required for the RPC server to start.
         */
        private void bootstrapOpenrewrite(Path pipPackagesPath) {
            Path rewriteModule = pipPackagesPath.resolve("rewrite");
            if (Files.exists(rewriteModule)) {
                return; // Already installed
            }

            try {
                Files.createDirectories(pipPackagesPath);

                ProcessBuilder pb = new ProcessBuilder(
                        pythonPath.toString(),
                        "-m", "pip", "install",
                        "--target=" + pipPackagesPath.toAbsolutePath().normalize(),
                        "openrewrite"
                );
                pb.redirectErrorStream(true);
                if (log != null) {
                    File logFile = log.toAbsolutePath().normalize().toFile();
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
                }
                Process process = pb.start();
                if (log == null) {
                    // Drain stdout+stderr to prevent pipe buffer from filling and blocking
                    Thread drainer = new Thread(() -> {
                        try (InputStream is = process.getInputStream()) {
                            byte[] buf = new byte[4096];
                            //noinspection StatementWithEmptyBody
                            while (is.read(buf) != -1) {
                            }
                        } catch (IOException ignored) {
                        }
                    });
                    drainer.setDaemon(true);
                    drainer.start();
                }
                boolean completed = process.waitFor(2, TimeUnit.MINUTES);

                if (!completed) {
                    process.destroyForcibly();
                    throw new RuntimeException("Timed out bootstrapping openrewrite package");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("Failed to bootstrap openrewrite package, pip install exited with code " + exitCode);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to bootstrap openrewrite package", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while bootstrapping openrewrite package", e);
            }
        }
    }
}
