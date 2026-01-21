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
package org.openrewrite.javascript.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

@Getter
public class JavaScriptRewriteRpc extends RewriteRpc {
    private static final RewriteRpcProcessManager<JavaScriptRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    /**
     * The command used to start the RPC process. Useful for logging and diagnostics.
     */
    private final String command;
    private final Map<String, String> commandEnv;
    private final RewriteRpcProcess process;

    JavaScriptRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace);
        this.command = command;
        this.commandEnv = commandEnv;
        this.process = process;
    }

    public static @Nullable JavaScriptRewriteRpc get() {
        return MANAGER.get();
    }

    public static JavaScriptRewriteRpc getOrStart() {
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

    public InstallRecipesResponse installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes.getAbsoluteFile().toPath()),
                InstallRecipesResponse.class
        );
    }

    public InstallRecipesResponse installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    public InstallRecipesResponse installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        );
    }

    /**
     * Parses an entire JavaScript/TypeScript project directory.
     * Discovers and parses all relevant source files, package.json files, and lock files.
     *
     * @param projectPath Path to the project directory to parse
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, ExecutionContext ctx) {
        return parseProject(projectPath, null, null, ctx);
    }

    /**
     * Parses an entire JavaScript/TypeScript project directory.
     * Discovers and parses all relevant source files, package.json files, and lock files.
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
     * Parses an entire JavaScript/TypeScript project directory.
     * Discovers and parses all relevant source files, package.json files, and lock files.
     *
     * @param projectPath Path to the project directory to parse
     * @param exclusions  Optional glob patterns to exclude from parsing
     * @param relativeTo  Optional path to make source file paths relative to. If not specified,
     *                    paths are relative to projectPath. Use this when parsing a subdirectory
     *                    but wanting paths relative to the repository root.
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        return StreamSupport.stream(new Spliterator<SourceFile>() {
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
                // for status update messages
                parsingListener.startedParsing(Parser.Input.fromFile(sourceFile.getSourcePath()));
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
                // Don't report SIZED until response is available, as estimateSize()
                // returns Long.MAX_VALUE before the RPC call completes
                return response == null ? ORDERED : ORDERED | SIZED | SUBSIZED;
            }
        }, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    @RequiredArgsConstructor
    public static class Builder implements Supplier<JavaScriptRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private final Map<String, String> environment = new HashMap<>();
        private Path npxPath = Paths.get("npx");
        private @Nullable Path log;
        private @Nullable Path metricsCsv;
        private @Nullable Path recipeInstallDir;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean traceRpcMessages;

        private @Nullable Integer inspectBrk;
        private @Nullable Path inspectBrkRewriteSourcePath;

        private @Nullable Integer maxHeapSize;
        private @Nullable Path workingDirectory;

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder recipeInstallDir(@Nullable Path recipeInstallDir) {
            this.recipeInstallDir = recipeInstallDir;
            return this;
        }

        /**
         * Path to the `npx` executable, not just the directory it is installed in.
         *
         * @param npxPath The path to the `npx` executable.
         * @return This builder
         */
        public Builder npxPath(Path npxPath) {
            if (Files.notExists(npxPath) || Files.isDirectory(npxPath)) {
                throw new IllegalArgumentException("Invalid npx executable " + npxPath.toAbsolutePath().normalize());
            }
            this.npxPath = npxPath;
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

        /**
         * Enables info and debug level logging.
         *
         * @return This builder.
         */
        public Builder traceRpcMessages(boolean verboseLogging) {
            this.traceRpcMessages = verboseLogging;
            return this;
        }

        public Builder traceRpcMessages() {
            return traceRpcMessages(true);
        }

        /**
         * Set the port for the Node.js inspector to listen on. When this is set, you can use
         * an "Attach to Node.js/Chrome" run configuration in IDEA to debug the JavaScript Rewrite RPC process.
         * The Rewrite RPC process will block waiting for this connection.
         *
         * @param rewriteSourcePath The path to either OpenRewrite TypeScript source code, or inside a
         *                          node_modules folder e.g., @openrewrite/rewrite. When
         *                          running a test from within this project, the value should be
         *                          "rewrite" (since "rewrite-javascript/rewrite" contains the TypeScript
         *                          source code).
         * @param inspectBrk        The port for the Node.js inspector to listen on.
         * @return This builder
         */
        public Builder inspectBrk(Path rewriteSourcePath, int inspectBrk) {
            this.inspectBrk = inspectBrk;
            this.inspectBrkRewriteSourcePath = rewriteSourcePath;
            return this;
        }

        public Builder inspectBrk(Path rewriteDistPath) {
            return inspectBrk(rewriteDistPath, 9229);
        }

        /**
         * Set the maximum heap size for the Node.js process in megabytes.
         * Default V8 heap size is approximately 1.5-2 GB on 64-bit systems.
         * For large repositories with many source files, you may need to increase this.
         *
         * @param maxHeapSize Maximum heap size in megabytes (e.g., 4096 for 4GB)
         * @return This builder
         */
        public Builder maxHeapSize(@Nullable Integer maxHeapSize) {
            this.maxHeapSize = maxHeapSize;
            return this;
        }

        /**
         * Set the working directory for the Node.js process.
         * This affects where profile logs and other output files are generated.
         * If not set, the process inherits the current working directory.
         *
         * @param workingDirectory The working directory for the Node.js process
         * @return This builder
         */
        public Builder workingDirectory(@Nullable Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        @Override
        public JavaScriptRewriteRpc get() {
            Stream<@Nullable String> cmd;

            log = Paths.get("/tmp/output136.log");

            if (inspectBrk != null) {
                Path serverJs = requireNonNull(inspectBrkRewriteSourcePath).resolve("dist/rpc/server.js");

                // We have to use node directly here because npx spawns a child node process. The
                // IDE's debug configuration would connect to the npx process rather than the spawned
                // node process and breakpoints don't get hit.
                cmd = Stream.of(
                        "node",
                        "--enable-source-maps",
                        "--inspect-brk=" + inspectBrk,
                        maxHeapSize != null ? "--max-old-space-size=" + maxHeapSize : null,
                        serverJs.toAbsolutePath().normalize().toString(),
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        traceRpcMessages ? "--trace-rpc-messages" : null,
                        recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize()
                );
            } else {
                String version = StringUtils.readFully(getClass().getResourceAsStream("/META-INF/version.txt"));
                cmd = Stream.of(
                        npxPath.toString(),
                        // For SNAPSHOT versions, assume npm link has been run and don't use --package
                        version.endsWith("-SNAPSHOT") ? null : "--package=@openrewrite/rewrite@" + version,
                        "--loglevel verbose",
                        "rewrite-rpc",
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        metricsCsv == null ? null : "--metrics-csv=" + metricsCsv.toAbsolutePath().normalize(),
                        traceRpcMessages ? "--trace-rpc-messages" : null,
                        recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize()
                );
            }

            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            // Set working directory if specified
            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }

            // Build NODE_OPTIONS with all necessary flags
            StringBuilder nodeOptions = new StringBuilder("--enable-source-maps");
            if (inspectBrk == null) {
                // When not using inspect-brk, we need to pass Node.js flags via NODE_OPTIONS
                // since npx spawns a child process
                // Note: --prof is not allowed in NODE_OPTIONS for security reasons
                if (maxHeapSize != null) {
                    nodeOptions.append(" --max-old-space-size=").append(maxHeapSize);
                }
            }
            process.environment().putAll(environment);
            process.environment().put("NODE_OPTIONS", nodeOptions.toString());
            if (npxPath.getParent() != null) {
                // `npx` is typically a shebang script alongside the `node` executable
                process.environment().put("PATH", npxPath.getParent() + File.pathSeparator +
                        System.getenv("PATH"));
            }
            process.start();

            try {
                return (JavaScriptRewriteRpc) new JavaScriptRewriteRpc(process, marketplace,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(System.out);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
