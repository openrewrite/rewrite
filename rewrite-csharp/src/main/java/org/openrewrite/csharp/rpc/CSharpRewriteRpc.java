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
package org.openrewrite.csharp.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RewriteRpcProcess;
import org.openrewrite.rpc.RewriteRpcProcessManager;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

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

@Getter
public class CSharpRewriteRpc extends RewriteRpc {
    private static final RewriteRpcProcessManager<CSharpRewriteRpc> MANAGER = new RewriteRpcProcessManager<>(builder());

    /**
     * The command used to start the RPC process. Useful for logging and diagnostics.
     */
    private final String command;
    private final Map<String, String> commandEnv;
    private final RewriteRpcProcess process;

    CSharpRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, List<RecipeBundleResolver> resolvers, String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace, resolvers);
        this.command = command;
        this.commandEnv = commandEnv;
        this.process = process;
    }

    public static @Nullable CSharpRewriteRpc get() {
        return MANAGER.get();
    }

    public static CSharpRewriteRpc getOrStart() {
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

    public InstallRecipesResponse installRecipes(java.io.File recipes) {
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

    public static void resetCurrent() {
        MANAGER.reset();
    }

    /**
     * Parses a .sln or .csproj file via MSBuildWorkspace on the C# side.
     * The C# side loads the solution/project, resolves all references,
     * discovers source files, and parses with correct type attribution.
     *
     * @param path    Path to the .sln or .csproj file
     * @param rootDir Repository root directory for computing relative source paths
     * @param ctx     Execution context for parsing
     * @return Stream of parsed source files
     */
    public ParseSolutionResult parseSolution(Path path, Path rootDir, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        Map<String, Object> options = new HashMap<>();
        options.put(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT,
                ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, true));

        // Phase 1: Eager RPC call to get lightweight response (IDs + metadata)
        parsingListener.intermediateMessage("Starting C# solution parsing: " + path);
        ParseSolutionResponse response = send("ParseSolution", new ParseSolution(path, rootDir, options), ParseSolutionResponse.class);
        parsingListener.intermediateMessage(String.format("Discovered %,d files to parse", response.itemCount()));

        // Phase 2: Lazy stream that retrieves full ASTs one at a time via getObject()
        Stream<SourceFile> sourceFiles = StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (index >= response.itemCount()) {
                    return false;
                }

                ParseSolutionResponse.Item item = response.getItem(index);
                index++;

                SourceFile sourceFile = getObject(item.getId(), item.getSourceFileType());

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
                return response.itemCount() - index;
            }

            @Override
            public int characteristics() {
                return ORDERED | SIZED | SUBSIZED;
            }
        }, false);

        return new ParseSolutionResult(sourceFiles, response.projects);
    }

    public static Builder builder() {
        return new Builder();
    }

    @RequiredArgsConstructor
    public static class Builder implements Supplier<CSharpRewriteRpc> {
        private static final String TOOL_COMMAND = "rewrite-csharp";
        private static final String NUGET_PACKAGE_ID = "OpenRewrite.CSharp.Tool";
        private static final String REWRITE_SOURCE_PATH_ENV = "REWRITE_SOURCE_PATH";

        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private List<RecipeBundleResolver> resolvers = new ArrayList<>();
        private final Map<String, String> environment = new HashMap<>();
        private Path dotnetPath = Paths.get("dotnet");
        private @Nullable Path csharpServerEntry;
        private @Nullable Path log;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean traceRpcMessages;
        private @Nullable Path workingDirectory;
        private @Nullable Path profileOutputPath;

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
            return this;
        }

        /**
         * A mutable list of resolvers that will be used by the RPC's PrepareRecipe handler.
         * Since the list is captured by reference, resolvers added after construction will
         * be visible to the handler.
         */
        public Builder resolvers(List<RecipeBundleResolver> resolvers) {
            this.resolvers = resolvers;
            return this;
        }

        /**
         * Path to the dotnet executable.
         *
         * @param dotnetPath The path to the dotnet executable (e.g., "dotnet", "/usr/local/share/dotnet/dotnet")
         * @return This builder
         */
        public Builder dotnetPath(Path dotnetPath) {
            this.dotnetPath = dotnetPath;
            return this;
        }

        /**
         * Explicit entry point for the C# RPC server, primarily for tests.
         * When set, the server is launched via {@code dotnet run --project <path>}
         * (for {@code .csproj}) or {@code dotnet <path>} (for DLLs), bypassing
         * both {@code REWRITE_SOURCE_PATH} and global tool auto-install.
         */
        public Builder csharpServerEntry(Path csharpServerEntry) {
            this.csharpServerEntry = csharpServerEntry;
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
         * Set the working directory for the dotnet process.
         *
         * @param workingDirectory The working directory for the dotnet process
         * @return This builder
         */
        public Builder workingDirectory(@Nullable Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        /**
         * Enable .NET EventPipe profiling on the C# process. Captures CPU sampling,
         * GC events, sampled allocations, and runtime counters into a {@code .nettrace}
         * file that can be analyzed with {@code dotnet-trace convert}, PerfView, or
         * Visual Studio.
         *
         * @param outputPath Path for the {@code .nettrace} output file
         * @return This builder
         */
        public Builder profile(Path outputPath) {
            this.profileOutputPath = outputPath;
            return this;
        }

        @Override
        public CSharpRewriteRpc get() {
            Stream<@Nullable String> cmd;

            if (csharpServerEntry != null) {
                // Explicit override (used by tests)
                if (csharpServerEntry.toString().endsWith(".csproj")) {
                    cmd = buildCsprojCommand(csharpServerEntry);
                } else {
                    cmd = Stream.of(
                            dotnetPath.toString(),
                            csharpServerEntry.toAbsolutePath().normalize().toString(),
                            log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                            traceRpcMessages ? "--trace-rpc-messages" : null
                    );
                }
            } else {
                // If REWRITE_SOURCE_PATH is set, launch from source via dotnet run
                String rewriteSourcePath = System.getenv(REWRITE_SOURCE_PATH_ENV);
                if (rewriteSourcePath != null && !rewriteSourcePath.isEmpty()) {
                    Path csproj = Paths.get(rewriteSourcePath)
                            .resolve("rewrite-csharp/csharp/OpenRewrite.Tool/OpenRewrite.Tool.csproj");
                    if (!Files.exists(csproj)) {
                        throw new IllegalStateException(
                                REWRITE_SOURCE_PATH_ENV + " is set to " + rewriteSourcePath +
                                " but " + csproj + " does not exist");
                    }
                    cmd = buildCsprojCommand(csproj);
                } else {
                    // Run via dotnet tool exec with the pinned version from the build
                    String version = StringUtils.readFully(
                            CSharpRewriteRpc.class.getResourceAsStream("/META-INF/rewrite-csharp-version.txt")).trim();
                    cmd = buildToolExecCommand(version);
                }
            }

            return startProcess(cmd);
        }

        private CSharpRewriteRpc startProcess(Stream<@Nullable String> cmd) {
            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }
            process.setStderrRedirect(log);

            process.environment().putAll(environment);

            if (profileOutputPath != null) {
                Map<String, String> env = process.environment();
                env.put("DOTNET_EnableEventPipe", "1");
                env.put("DOTNET_EventPipeOutputPath", profileOutputPath.toAbsolutePath().normalize().toString());
                env.put("DOTNET_EventPipeOutputStreaming", "1");
                env.put("DOTNET_EventPipeCircularMB", "256");
                env.put("DOTNET_EventPipeConfig",
                        "Microsoft-DotNETCore-SampleProfiler:0:5;" +
                        "Microsoft-Windows-DotNETRuntime:2088009:4;" +
                        "System.Runtime:0:4");
            }

            process.start();

            try {
                return (CSharpRewriteRpc) new CSharpRewriteRpc(process, marketplace, resolvers,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Stream<@Nullable String> buildCsprojCommand(Path csproj) {
            return Stream.of(
                    dotnetPath.toString(),
                    "run",
                    "--project", csproj.toAbsolutePath().normalize().toString(),
                    "--framework", "net10.0",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );
        }

        private Stream<@Nullable String> buildToolExecCommand(String version) {
            // When the tool package exists in the NuGet global cache (e.g. from pTML),
            // add it as a source so dotnet tool exec can resolve it without remote feeds
            Path globalCachePath = Paths.get(System.getProperty("user.home"),
                    ".nuget", "packages", NUGET_PACKAGE_ID.toLowerCase(), version);
            String addSource = Files.isDirectory(globalCachePath) ? globalCachePath.toString() : null;

            return Stream.of(
                    dotnetPath.toString(),
                    "tool", "exec",
                    NUGET_PACKAGE_ID + "@" + version,
                    "-y",
                    "--allow-roll-forward",
                    addSource != null ? "--add-source" : null,
                    addSource,
                    "--ignore-failed-sources",
                    // Suppress NuGet informational messages (e.g. "Skipping NuGet package
                    // signature verification") that would corrupt the RPC stdout channel.
                    "-v", "q",
                    "--",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );
        }
    }
}
