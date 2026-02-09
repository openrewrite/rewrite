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
     * Parses C# source files.
     *
     * @param sourcePaths Paths to the C# source files to parse
     * @param ctx         Execution context for parsing
     * @return Stream of parsed source files
     */
    public Stream<SourceFile> parse(List<Path> sourcePaths, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        return StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;
            private @Nullable ParseResponse response;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (response == null) {
                    parsingListener.intermediateMessage("Starting C# parsing: " + sourcePaths.size() + " files");
                    response = send("Parse", new ParseRequest(sourcePaths), ParseResponse.class);
                    parsingListener.intermediateMessage(String.format("Parsed %,d files", response.size()));
                }

                if (index >= response.size()) {
                    return false;
                }

                String sourceFileId = response.get(index);
                index++;

                // Use the Java CompilationUnit type name for codec lookup
                SourceFile sourceFile = getObject(sourceFileId, Cs.CompilationUnit.class.getName());
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
        private @Nullable Path csharpProjectPath;
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
         * Path to the C# Rewrite.Server project.
         *
         * @param csharpProjectPath The path to the C# project directory containing Rewrite.Server
         * @return This builder
         */
        public Builder csharpProjectPath(Path csharpProjectPath) {
            this.csharpProjectPath = csharpProjectPath;
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
            Path projectPath = findCSharpProjectPath();

            Stream<@Nullable String> cmd = Stream.of(
                    dotnetPath.toString(),
                    "run",
                    "--project", projectPath.resolve("OpenRewrite/OpenRewrite.csproj").toString(),
                    "--framework", "net9.0",
                    "--no-build",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null
            );

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

        private Path findCSharpProjectPath() {
            if (csharpProjectPath != null) {
                return csharpProjectPath;
            }

            // Try to find the C# project in the project structure
            Path basePath = workingDirectory != null ? workingDirectory : Paths.get(System.getProperty("user.dir"));

            // Check common locations relative to nagoya (rewrite) repo
            Path[] searchPaths = {
                    // From rewrite-csharp module dir
                    basePath.resolve("csharp"),
                    // From rewrite root dir
                    basePath.resolve("rewrite-csharp/csharp"),
            };

            for (Path searchPath : searchPaths) {
                if (searchPath != null && Files.exists(searchPath.resolve("OpenRewrite/OpenRewrite.csproj"))) {
                    return searchPath.toAbsolutePath().normalize();
                }
            }

            throw new IllegalStateException(
                    "Could not find C# Rewrite project. Please set csharpProjectPath() on the builder. " +
                    "Expected to find OpenRewrite/OpenRewrite.csproj in the project directory.");
        }
    }
}
