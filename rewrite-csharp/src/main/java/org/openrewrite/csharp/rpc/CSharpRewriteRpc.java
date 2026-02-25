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
import org.openrewrite.csharp.tree.Cs;
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

    CSharpRewriteRpc(RewriteRpcProcess process, RecipeMarketplace marketplace, String command, Map<String, String> commandEnv) {
        super(process.getRpcClient(), marketplace);
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

    /**
     * Resets the cached state of the current C# RPC instance.
     * This clears all parsed objects and references on both the Java and C# sides,
     * preventing memory accumulation across multiple parse operations.
     * <p>
     * Call this between tests or after batch operations that don't need to share state.
     */
    public static void resetCurrent() {
        CSharpRewriteRpc current = MANAGER.get();
        if (current != null) {
            current.reset();
        }
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
    public Stream<SourceFile> parseSolution(Path path, Path rootDir, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        return StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;
            private @Nullable ParseSolutionResponse response;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (response == null) {
                    parsingListener.intermediateMessage("Starting C# solution parsing: " + path);
                    response = send("ParseSolution", new ParseSolution(path, rootDir), ParseSolutionResponse.class);
                    parsingListener.intermediateMessage(String.format("Discovered %,d files to parse", response.size()));
                }

                if (index >= response.size()) {
                    return false;
                }

                ParseSolutionResponse.Item item = response.get(index);
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

    @RequiredArgsConstructor
    public static class Builder implements Supplier<CSharpRewriteRpc> {
        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private final Map<String, String> environment = new HashMap<>();
        private Path dotnetPath = Paths.get("dotnet");
        private @Nullable Path csharpServerEntry;
        private @Nullable Path log;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean traceRpcMessages;
        private @Nullable Path workingDirectory;

        public Builder marketplace(RecipeMarketplace marketplace) {
            this.marketplace = marketplace;
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
         * Entry point that launches the C# RPC server. When the path has a {@code .csproj}
         * extension, the server is launched from source via {@code dotnet run --project}.
         * Otherwise the path is treated as a published executable (DLL) and launched
         * via {@code dotnet <path>}.
         *
         * @param csharpServerEntry Path to either a {@code .csproj} file or a published DLL
         * @return This builder
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

        @Override
        public CSharpRewriteRpc get() {
            Path entry = findCSharpServerEntry();
            Stream<@Nullable String> cmd;
            if (entry.toString().endsWith(".csproj")) {
                cmd = Stream.of(
                        dotnetPath.toString(),
                        "run",
                        "--project", entry.toAbsolutePath().normalize().toString(),
                        "--framework", "net10.0",
                        "--no-build",
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        traceRpcMessages ? "--trace-rpc-messages" : null
                );
            } else {
                cmd = Stream.of(
                        dotnetPath.toString(),
                        entry.toAbsolutePath().normalize().toString(),
                        log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                        traceRpcMessages ? "--trace-rpc-messages" : null
                );
            }

            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }

            process.environment().putAll(environment);
            process.start();

            try {
                return (CSharpRewriteRpc) new CSharpRewriteRpc(process, marketplace,
                        String.join(" ", cmdArr), process.environment())
                        .livenessCheck(process::getLivenessCheck)
                        .timeout(timeout)
                        .log(log == null ? null : new PrintStream(Files.newOutputStream(log, StandardOpenOption.APPEND, StandardOpenOption.CREATE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Path findCSharpServerEntry() {
            if (csharpServerEntry != null) {
                return csharpServerEntry;
            }

            // Check for globally installed dotnet tool
            Path toolStore = Paths.get(System.getProperty("user.home"), ".dotnet", "tools", ".store", "openrewrite.csharp");
            if (Files.isDirectory(toolStore)) {
                try (Stream<Path> versions = Files.list(toolStore)) {
                    Optional<Path> dll = versions
                            .sorted(Comparator.reverseOrder())
                            .flatMap(versionDir -> {
                                try {
                                    return Files.walk(versionDir)
                                            .filter(p -> p.getFileName().toString().equals("OpenRewrite.dll"));
                                } catch (IOException e) {
                                    return Stream.empty();
                                }
                            })
                            .findFirst();
                    if (dll.isPresent()) {
                        return dll.get().toAbsolutePath().normalize();
                    }
                } catch (IOException ignored) {
                }
            }

            // Fall back to development paths relative to working directory
            Path basePath = workingDirectory != null ? workingDirectory : Paths.get(System.getProperty("user.dir"));
            Path[] searchPaths = {
                    basePath.resolve("csharp"),
                    basePath.resolve("rewrite-csharp/csharp"),
            };

            for (Path searchPath : searchPaths) {
                Path csproj = searchPath.resolve("OpenRewrite/OpenRewrite.csproj");
                if (Files.exists(csproj)) {
                    return csproj.toAbsolutePath().normalize();
                }
            }

            throw new IllegalStateException(
                    "Could not find C# Rewrite project. Please set csharpServerEntry() on the builder. " +
                    "Expected to find OpenRewrite.dll in ~/.dotnet/tools/.store/openrewrite.csharp/ " +
                    "or OpenRewrite/OpenRewrite.csproj in the project directory.");
        }
    }
}
