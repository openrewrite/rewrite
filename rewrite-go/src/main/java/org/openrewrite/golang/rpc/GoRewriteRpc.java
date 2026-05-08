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
package org.openrewrite.golang.rpc;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.GolangParser;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;
import org.openrewrite.rpc.request.Parse;
import org.openrewrite.rpc.request.ParseResponse;
import org.openrewrite.tree.ParseError;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * RPC client that communicates with a Go process for parsing and printing Go source code.
 */
@Getter
public class GoRewriteRpc extends RewriteRpc {

    private static final RewriteRpcProcessManager<GoRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    private final String command;
    private final Map<String, String> commandEnv;
    private final RewriteRpcProcess process;

    GoRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers,
                 String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace, resolvers);
        this.command = command;
        this.commandEnv = commandEnv;
        this.process = process;
    }

    public static @Nullable GoRewriteRpc get() {
        return MANAGER.get();
    }

    public static GoRewriteRpc getOrStart() {
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

    public static void resetCurrent() {
        MANAGER.reset();
    }

    /**
     * Parse a batch of Go source inputs with project (module) context.
     * The Go server constructs a {@code ProjectImporter} from the module
     * path + go.mod content, registers every input as a sibling, and uses
     * it for type attribution. Files in the same directory are parsed
     * together so cross-file references inside a package resolve.
     * <p>
     * Use the regular {@link #parse} path when no project context is
     * available — that falls back to per-file, stdlib-only type
     * attribution (today's behavior).
     */
    public java.util.stream.Stream<SourceFile> parseWithProject(
            Iterable<Parser.Input> inputs,
            @Nullable Path relativeTo,
            Parser parser,
            String sourceFileType,
            ExecutionContext ctx,
            String module,
            @Nullable String goModContent) {
        java.util.List<Parser.Input> inputList = new java.util.ArrayList<>();
        java.util.List<Parse.Input> mappedInputs = new java.util.ArrayList<>();
        for (Parser.Input input : inputs) {
            inputList.add(input);
            if (input.isSynthetic() || !java.nio.file.Files.isRegularFile(input.getPath())) {
                mappedInputs.add(new Parse.Input(input.getSource(ctx).readFully(), input.getPath()));
            } else {
                mappedInputs.add(new Parse.Input(null, input.getPath()));
            }
        }
        if (inputList.isEmpty()) {
            return java.util.stream.Stream.empty();
        }

        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        parsingListener.intermediateMessage(String.format("Starting parsing of %,d files (module=%s)", inputList.size(), module));

        java.util.List<String> ids = send("Parse", new GoParseRequest(
                mappedInputs,
                relativeTo != null ? relativeTo.toString() : null,
                module,
                goModContent
        ), ParseResponse.class);
        if (ids.size() != inputList.size()) {
            throw new IllegalStateException("Parse response size " + ids.size() + " != input size " + inputList.size());
        }

        java.util.List<SourceFile> result = new java.util.ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            Parser.Input input = inputList.get(i);
            parsingListener.startedParsing(input);
            SourceFile sf;
            try {
                sf = getObject(ids.get(i), sourceFileType);
            } catch (Exception e) {
                sf = ParseError.build(parser, input, relativeTo, ctx, e);
            }
            result.add(sf);
            parsingListener.parsed(input, sf);
        }
        return result.stream();
    }

    /**
     * Install recipes from a local file path (e.g., a local Go module).
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
     * @param packageName The package name to install (e.g., a Git repository URL)
     * @return Response with installation details
     */
    public InstallRecipesResponse installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    /**
     * Install recipes from a package name with a specific version.
     *
     * @param packageName The package name to install (e.g., a Git repository URL)
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
     * Parses an entire Go project directory.
     *
     * @param projectPath Path to the project directory to parse
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parseProject(Path projectPath, ExecutionContext ctx) {
        return parseProject(projectPath, null, null, ctx);
    }

    /**
     * Parses an entire Go project directory.
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
     * Parses an entire Go project directory.
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

                SourceFile sourceFile;
                try {
                    sourceFile = getObject(item.getId(), item.getSourceFileType());
                    parsingListener.startedParsing(Parser.Input.fromFile(sourceFile.getSourcePath()));
                } catch (Exception e) {
                    // A single file's RPC deserialization failed. Convert it to a ParseError
                    // pointing at the offending file and keep the stream going.
                    //
                    // `item.sourcePath` may be null when talking to an older Go peer — fall
                    // back to the RPC object id so we still produce a usable ParseError.
                    String relativePath = item.getSourcePath() != null ? item.getSourcePath() : item.getId();
                    Path sourcePath = Paths.get(relativePath);
                    Path absoluteSourcePath = projectPath.resolve(sourcePath);
                    try {
                        sourceFile = ParseError.build(
                                GolangParser.builder().build(),
                                Parser.Input.fromFile(absoluteSourcePath),
                                projectPath,
                                ctx,
                                e);
                    } catch (Exception readFailure) {
                        // If the file can't be read (e.g. the fallback path from id doesn't
                        // exist on disk), wrap the original exception in a runtime so the
                        // stream continues. Without the source text the error is less useful
                        // but the alternative — aborting the whole stream — is worse.
                        throw new RuntimeException("ParseProject item " + item.getId() + " failed; readback also failed", e);
                    }
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
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements Supplier<GoRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private List<RecipeBundleResolver> resolvers = Collections.emptyList();
        private final Map<String, String> environment = new HashMap<>();
        private Supplier<@Nullable Path> goBinaryPathSupplier = () -> null;
        private Duration timeout = Duration.ofSeconds(60);
        private @Nullable Path log;
        private @Nullable Path metricsCsv;
        private @Nullable Path recipeInstallDir;
        private @Nullable Path dataTablesCsvDir;
        private @Nullable Path workingDirectory;
        private boolean traceRpcMessages;

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        public Builder resolvers(List<RecipeBundleResolver> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        public Builder goBinaryPath(Path path) {
            return goBinaryPath(() -> path);
        }

        /**
         * Supplies the path to the Go RPC binary. The supplier is invoked at most
         * once, when the RPC is first started. Returning {@code null} uses the built-in
         * fallback discovery (same as not configuring the path at all). Exceptions
         * thrown by the supplier propagate out of the RPC-start call.
         *
         * @param goBinaryPathSupplier Supplier for the path to the Go RPC binary
         * @return This builder
         */
        public Builder goBinaryPath(Supplier<@Nullable Path> goBinaryPathSupplier) {
            this.goBinaryPathSupplier = goBinaryPathSupplier;
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

        public Builder recipeInstallDir(@Nullable Path recipeInstallDir) {
            this.recipeInstallDir = recipeInstallDir;
            return this;
        }

        public Builder dataTablesCsvDir(@Nullable Path dataTablesCsvDir) {
            this.dataTablesCsvDir = dataTablesCsvDir;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment.putAll(environment);
            return this;
        }

        public Builder workingDirectory(@Nullable Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder traceRpcMessages(boolean verboseLogging) {
            this.traceRpcMessages = verboseLogging;
            return this;
        }

        public Builder traceRpcMessages() {
            return traceRpcMessages(true);
        }

        @Override
        public GoRewriteRpc get() {
            @Nullable Path goBinaryPath = goBinaryPathSupplier.get();
            String binaryPath;
            if (goBinaryPath != null) {
                binaryPath = goBinaryPath.toString();
            } else {
                // Check for a custom binary with installed recipes
                Path customBin = Paths.get(
                        System.getProperty("user.home"), ".rewrite", "go-recipes", "rewrite-go-rpc");
                if (Files.isExecutable(customBin)) {
                    binaryPath = customBin.toString();
                } else {
                    binaryPath = "rewrite-go-rpc";
                }
            }

            Stream<@Nullable String> cmd = Stream.of(
                    binaryPath,
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    metricsCsv == null ? null : "--metrics-csv=" + metricsCsv.toAbsolutePath().normalize(),
                    recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize(),
                    dataTablesCsvDir == null ? null : "--data-tables-csv-dir=" + dataTablesCsvDir.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );

            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }
            process.setStderrRedirect(log);
            process.environment().putAll(environment);
            process.start();

            try {
                return (GoRewriteRpc) new GoRewriteRpc(process, marketplace, resolvers,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
