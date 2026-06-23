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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
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

    /**
     * Shut down every C# RPC server across all threads. Only call this when no
     * C# parsing or printing is in flight (e.g. on JVM/service shutdown);
     * {@link #shutdownCurrent()} already reaps servers orphaned by dead threads.
     */
    public static void shutdownAll() {
        MANAGER.shutdownAll();
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
    public Stream<SourceFile> parseSolution(Path path, Path rootDir, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();

        Map<String, Object> options = new HashMap<>();
        options.put(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT,
                ctx.getMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, true));

        return StreamSupport.stream(new Spliterator<SourceFile>() {
            private int index = 0;
            private @Nullable ParseSolutionResponse response;

            @Override
            public boolean tryAdvance(Consumer<? super SourceFile> action) {
                if (response == null) {
                    parsingListener.intermediateMessage("Starting C# solution parsing: " + path);
                    response = send("ParseSolution", new ParseSolution(path, rootDir, options), ParseSolutionResponse.class);
                    parsingListener.intermediateMessage(String.format("Discovered %,d files to parse", response.getItems().size()));
                }

                if (index >= response.getItems().size()) {
                    return false;
                }

                ParseSolutionResponse.Item item = response.getItems().get(index);
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
                return response == null ? Long.MAX_VALUE : response.getItems().size() - index;
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
    public static class Builder implements Supplier<CSharpRewriteRpc> {
        private static final String TOOL_COMMAND = "rewrite-csharp";
        private static final String NUGET_PACKAGE_ID = "OpenRewrite.CSharp.Tool";

        /**
         * Serializes tool installs within this JVM. {@code RewriteRpcProcessManager}
         * holds one RPC per thread, so several threads can each trigger an install of
         * the same version at once. A {@link FileLock} alone is per-JVM (a second
         * channel lock from the same JVM throws {@code OverlappingFileLockException}
         * rather than blocking), so cross-thread serialization needs this monitor in
         * addition to the cross-process file lock.
         */
        private static final Object INSTALL_LOCK = new Object();


        private RecipeMarketplace marketplace = new RecipeMarketplace();
        private List<RecipeBundleResolver> resolvers = new ArrayList<>();
        private final Map<String, String> environment = new HashMap<>();
        private static final Path DEFAULT_DOTNET_PATH = Paths.get("dotnet");
        private Supplier<@Nullable Path> dotnetPathSupplier = () -> DEFAULT_DOTNET_PATH;
        private @Nullable Path csharpServerEntry;
        private @Nullable Path log;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean traceRpcMessages;
        private @Nullable Path workingDirectory;
        private @Nullable Path recipeInstallDir;
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
            return dotnetPath(() -> dotnetPath);
        }

        /**
         * Supplies the path to the dotnet executable. The supplier is invoked at most
         * once, when the RPC is first started. Returning {@code null} uses the built-in
         * default (same as not configuring the path at all). Exceptions thrown by the
         * supplier propagate out of the RPC-start call.
         *
         * @param dotnetPathSupplier Supplier for the path to the dotnet executable
         * @return This builder
         */
        public Builder dotnetPath(Supplier<@Nullable Path> dotnetPathSupplier) {
            this.dotnetPathSupplier = dotnetPathSupplier;
            return this;
        }

        /**
         * Explicit entry point for the C# RPC server, primarily for tests.
         * When set, the server is launched via {@code dotnet run --project <path>}
         * (for {@code .csproj}) or {@code dotnet <path>} (for DLLs), bypassing
         * global tool auto-install.
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
         * Directory in which the server creates its recipe project (where
         * {@code dotnet add package} resolves recipe NuGet packages). Set this so a
         * caller-written {@code NuGet.config} co-located in the directory is honored
         * by dotnet's project-directory config discovery.
         *
         * @param recipeInstallDir The directory to create the recipe project in
         * @return This builder
         */
        public Builder recipeInstallDir(@Nullable Path recipeInstallDir) {
            this.recipeInstallDir = recipeInstallDir;
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
            Path dotnetPath = dotnetPathSupplier.get();
            if (dotnetPath == null) {
                dotnetPath = DEFAULT_DOTNET_PATH;
            }

            Stream<@Nullable String> cmd;

            // An explicit builder value wins; otherwise fall back to the
            // REWRITE_DOTNET_RPC_SERVER environment variable so a source .csproj
            // or a pre-built dll/exe can be selected without code changes (local
            // cross-repo development, parallel git worktrees). A .csproj is launched
            // via `dotnet run --project`; anything else is treated as a server entry
            // assembly/executable run directly.
            Path serverEntry = csharpServerEntry;
            if (serverEntry == null) {
                String envServerEntry = System.getenv("REWRITE_DOTNET_RPC_SERVER");
                if (envServerEntry != null && !envServerEntry.isEmpty()) {
                    serverEntry = Paths.get(envServerEntry);
                }
            }

            if (serverEntry != null) {
                if (serverEntry.toString().endsWith(".csproj")) {
                    cmd = buildCsprojCommand(dotnetPath, serverEntry);
                } else {
                    cmd = Stream.of(
                            dotnetPath.toString(),
                            serverEntry.toAbsolutePath().normalize().toString(),
                            log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                            traceRpcMessages ? "--trace-rpc-messages" : null,
                            recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize()
                    );
                }
            } else {
                // Install and run the tool from a persistent tool-path, bypassing
                // dotnet tool exec which has auth issues with private feeds (dotnet/sdk#51375).
                // REWRITE_DOTNET_RPC_SERVER_VERSION overrides the embedded version so a
                // specific package version of the tool can be pinned without rebuilding.
                String version = System.getenv("REWRITE_DOTNET_RPC_SERVER_VERSION");
                if (version == null || version.isEmpty()) {
                    version = StringUtils.readFully(
                            CSharpRewriteRpc.class.getResourceAsStream("/META-INF/rewrite-csharp-version.txt")).trim();
                }
                cmd = buildToolPathCommand(dotnetPath, version);
            }

            return startProcess(cmd, dotnetPath);
        }

        private CSharpRewriteRpc startProcess(Stream<@Nullable String> cmd, Path dotnetPath) {
            String[] cmdArr = cmd.filter(Objects::nonNull).toArray(String[]::new);
            RewriteRpcProcess process = new RewriteRpcProcess(cmdArr);

            if (workingDirectory != null) {
                process.setWorkingDirectory(workingDirectory);
            }
            process.setStderrRedirect(log);

            process.environment().putAll(environment);

            // The tool is launched as a self-contained apphost (not via the `dotnet`
            // muxer), so it must locate the shared runtime itself. When DOTNET_ROOT is
            // unset and the runtime isn't at a registered install location for the
            // apphost's architecture, launch fails ("You must install .NET ..."). This
            // bites on hosts where `dotnet` lives in a bin/ dir separate from the runtime
            // (e.g. Homebrew, where the binary is under bin/ but the runtime under
            // libexec/), and on arm64 machines using an x64 dotnet. Derive DOTNET_ROOT
            // from the resolved dotnet and set it if the caller hasn't.
            if (!process.environment().containsKey("DOTNET_ROOT")) {
                Path dotnetRoot = resolveDotnetRoot(dotnetPath);
                if (dotnetRoot != null) {
                    process.environment().put("DOTNET_ROOT", dotnetRoot.toString());
                }
            }

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

        /**
         * Derive the {@code DOTNET_ROOT} (the directory containing {@code shared/}) for a
         * resolved {@code dotnet} executable by parsing {@code dotnet --list-runtimes}, whose
         * lines look like {@code Microsoft.NETCore.App 10.0.2 [/path/to/dotnet/shared/Microsoft.NETCore.App]}.
         * The root is two levels up from that path ({@code .../shared/Microsoft.NETCore.App} →
         * {@code .../shared} → root). Returns {@code null} if it can't be determined; callers
         * then fall back to the apphost's own resolution.
         */
        private static @Nullable Path resolveDotnetRoot(Path dotnetPath) {
            Path resolved = null;
            try {
                Process p = new ProcessBuilder(dotnetPath.toString(), "--list-runtimes")
                        .redirectErrorStream(false)
                        .start();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int lb = line.indexOf('[');
                        int rb = line.lastIndexOf(']');
                        if (lb >= 0 && rb > lb) {
                            Path sharedAppDir = Paths.get(line.substring(lb + 1, rb).trim());
                            Path shared = sharedAppDir.getParent();
                            Path root = shared == null ? null : shared.getParent();
                            if (root != null && Files.isDirectory(root)) {
                                resolved = root;
                                break;
                            }
                        }
                    }
                }
                p.waitFor();
            } catch (IOException | InterruptedException ignored) {
                // Best effort: leave DOTNET_ROOT unset and let the apphost resolve.
            }
            return resolved;
        }

        private Stream<@Nullable String> buildCsprojCommand(Path dotnetPath, Path csproj) {
            return Stream.of(
                    dotnetPath.toString(),
                    "run",
                    "--project", csproj.toAbsolutePath().normalize().toString(),
                    "--framework", "net10.0",
                    // Never let `dotnet run` build/restore here: the caller is expected to have
                    // already built the tool (the Gradle integTest depends on csharpBuild). An
                    // implicit restore/build streams MSBuild + NuGet audit output (e.g. NU1903
                    // vulnerability warnings) to stdout, which is the JSON-RPC pipe. That corrupts
                    // the stream: the Java peer rejects the non-Content-Length text and replies
                    // with id-less JSON-RPC errors, which crash the C# SystemTextJsonFormatter
                    // ("Non-default ID required"). --no-build also implies --no-restore.
                    "--no-build",
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null,
                    recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize()
            );
        }

        /**
         * Ensures the tool is installed at a persistent tool-path and returns a command
         * to run it directly. This bypasses {@code dotnet tool exec} entirely, working
         * around https://github.com/dotnet/sdk/issues/51375 where {@code dotnet tool exec}
         * fails to authenticate against private NuGet feeds.
         * <p>
         * Uses {@code dotnet tool install --tool-path} which handles authentication
         * correctly. The tool-path is version-specific so multiple versions can coexist;
         * concurrent installs of the <em>same</em> version (parallel Gradle test forks
         * hitting a fresh daily snapshot) are serialized by {@link #installToolPath}.
         */
        private Stream<@Nullable String> buildToolPathCommand(Path dotnetPath, String version) {
            Path toolPath = Paths.get(System.getProperty("user.home"),
                    ".dotnet", "rewrite-tools", version);
            Path toolExecutable = toolPath.resolve(TOOL_COMMAND);

            if (!Files.isRegularFile(toolExecutable)) {
                installTool(dotnetPath, version, toolPath, toolExecutable);
            }

            return Stream.of(
                    toolExecutable.toAbsolutePath().normalize().toString(),
                    log == null ? null : "--log-file=" + log.toAbsolutePath().normalize(),
                    traceRpcMessages ? "--trace-rpc-messages" : null,
                    recipeInstallDir == null ? null : "--recipe-install-dir=" + recipeInstallDir.toAbsolutePath().normalize()
            );
        }

        private void installTool(Path dotnetPath, String version, Path toolPath, Path toolExecutable) {
            installToolPath(toolPath.getParent(), version, toolExecutable,
                    () -> runDotnetInstall(dotnetPath, version, toolPath));
        }

        /**
         * Runs {@code install} at most once for {@code toolExecutable}, serialized
         * against other threads in this JVM and other processes on this host.
         * {@code dotnet tool install} is not safe to run concurrently into a single
         * {@code --tool-path}: racing installs of the same not-yet-cached version fail
         * with "Directory not empty". Gradle runs test forks in parallel (one JVM per
         * core), each lazily triggering an install of the same daily snapshot, so the
         * install is guarded by an exclusive lock on a {@code <version>.lock} file in
         * the shared tools directory. The executable presence is re-checked under the
         * lock so a winner's install is reused rather than repeated.
         */
        static void installToolPath(Path toolsDir, String version, Path toolExecutable, Runnable install) {
            synchronized (INSTALL_LOCK) {
                try {
                    Files.createDirectories(toolsDir);
                    Path lockFile = toolsDir.resolve(version + ".lock");
                    try (FileChannel channel = FileChannel.open(lockFile,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                         FileLock ignored = channel.lock()) {
                        if (!Files.isRegularFile(toolExecutable)) {
                            install.run();
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to install " + NUGET_PACKAGE_ID + "@" + version, e);
                }
            }
        }

        private void runDotnetInstall(Path dotnetPath, String version, Path toolPath) {
            Path installCwd = null;
            try {
                Files.createDirectories(toolPath);

                List<String> installCmd = new ArrayList<>(Arrays.asList(
                        dotnetPath.toString(),
                        "tool", "install",
                        NUGET_PACKAGE_ID,
                        "--version", version,
                        "--tool-path", toolPath.toString(),
                        "--ignore-failed-sources"
                ));

                // When the tool package exists in the NuGet global cache (e.g. from publishToMavenLocal),
                // add it as a source so the install can resolve it without remote feeds
                Path globalCachePath = Paths.get(System.getProperty("user.home"),
                        ".nuget", "packages", NUGET_PACKAGE_ID.toLowerCase(), version);
                if (Files.isDirectory(globalCachePath)) {
                    installCmd.addAll(Arrays.asList("--add-source", globalCachePath.toString()));
                }

                ProcessBuilder pb = new ProcessBuilder(installCmd);
                // Run from a fresh, empty temp directory so NuGet's working-directory config
                // walk finds no repo-level nuget.config up the hierarchy. Such a config could
                // <clear/> global sources or enable <packageSourceMapping> that rejects
                // --add-source with "The --add-source option cannot be combined with package
                // source mapping". User- and machine-level config still apply, since they are
                // discovered independently of the working directory.
                installCwd = Files.createTempDirectory("rewrite-csharp-tool-install");
                pb.directory(installCwd.toFile());
                pb.environment().putAll(environment);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = StringUtils.readFully(process.getInputStream());
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new RuntimeException(
                            "Failed to install " + NUGET_PACKAGE_ID + "@" + version +
                            " to " + toolPath + " (exit code " + exitCode + "): " + output);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to install " + NUGET_PACKAGE_ID + "@" + version, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while installing " + NUGET_PACKAGE_ID + "@" + version, e);
            } finally {
                if (installCwd != null) {
                    try {
                        Files.deleteIfExists(installCwd);
                    } catch (IOException ignored) {
                        // Best effort: the temp directory is empty and harmless if it lingers.
                    }
                }
            }
        }
    }
}
