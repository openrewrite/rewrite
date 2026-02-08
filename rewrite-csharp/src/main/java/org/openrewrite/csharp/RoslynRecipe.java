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
package org.openrewrite.csharp;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.quark.Quark;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.scheduling.WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT;

/**
 * Base class for executing C# Roslyn-based recipes through a hybrid Java-C# architecture.
 *
 * This abstract class orchestrates the execution of C# code transformations by:
 * 1. Collecting multiple RoslynRecipe instances in a batch during the scanning phase
 * 2. Writing source files to a temporary working directory
 * 3. Executing a C# process (Rewrite.Server.dll) with all collected recipes at once
 * 4. Reading back the modified files and applying changes to the source tree
 *
 * The recipe uses a two-phase execution model:
 * - Scanning phase: Collects all RoslynRecipe instances and writes source files to disk
 * - Generation phase: On the last recipe, executes the C# process with all recipes
 * - Visitor phase: Reads modified files from disk and updates the AST
 *
 * The C# process is invoked via command line with NuGet package references and recipe IDs,
 * allowing it to download and execute Roslyn analyzers and code fixes dynamically.
 */
public abstract class RoslynRecipe extends ScanningRecipe<RoslynRecipe.Accumulator> {

    /**
     * Limits execution to a single cycle as Roslyn recipes are designed to run once.
     * @return 1 to indicate single-pass execution
     */
    @Override
    public int maxCycles() {
        return 1;
    }


    /**
     * Gets the unique identifier for this recipe, typically a Roslyn diagnostic ID (e.g., "CS1234")
     * or a fully qualified type name for OpenRewrite recipes.
     * @return The recipe identifier
     */
    public abstract String getRecipeId();

    /**
     * Gets the NuGet package name containing the Roslyn analyzer or code fix for this recipe.
     * @return The NuGet package name (e.g., "Microsoft.CodeAnalysis.CSharp")
     */
    public abstract String getNugetPackageName();

    /**
     * Gets the version of the NuGet package to use.
     * @return The package version (e.g., "4.8.0") or "SNAPSHOT" for latest prerelease, "RELEASE" for latest stable
     */
    public abstract String getNugetPackageVersion();

    /**
     * Initializes the accumulator and registers this recipe in the batch.
     * Multiple RoslynRecipe instances are collected during scanning to be executed together.
     * @param executionContext The execution context
     * @return An empty accumulator (state is maintained in the Context)
     */
    @Override
    public Accumulator getInitialValue(ExecutionContext executionContext) {
        var ctx = new Context(executionContext);

        ctx.addRecipeInBatch(this);
        ctx.trySetAsFirstRecipeInBatch(this);

        return new Accumulator();
    }

    /**
     * Scanner phase: Writes source files to the working directory for the C# process to consume.
     * Only the first recipe in the batch writes files to avoid duplication.
     * @param acc The accumulator (unused, state is in Context)
     * @return A visitor that writes C# source files to disk
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
            var ctx = Context.wrap(executionContext);
            if (tree instanceof SourceFile
                    && !(tree instanceof Quark)
                    && !(tree instanceof ParseError)
                    && !tree.getClass().getName().equals("org.openrewrite.java.tree.J$CompilationUnit")) {
                SourceFile sourceFile = (SourceFile) tree;

                // only extract initial source files for first roslyn recipe
                if (ctx.isFirstRecipe(RoslynRecipe.this)) {
                    // FIXME filter out more source types; possibly only write plain text, json, and yaml?
                    ctx.writeSource(sourceFile);
                }
            }
            return tree;
            }
        };
    }

    /**
     * Generation phase: Executes the C# process with all collected recipes when the last recipe is reached.
     * This batching strategy optimizes performance by running all Roslyn transformations in a single process.
     * @param acc The accumulator (unused)
     * @param executionContext The execution context containing recipe batch information
     * @return Empty list as file generation happens through the C# process
     */
    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext executionContext) {
        var ctx = Context.wrap(executionContext);
        if(ctx.isLastRecipe(this)) {
            runRoslynRecipe(ctx);
        }
        return emptyList();
    }

    /**
     * Visitor phase: Replaces source files with their modified versions read from disk.
     * Files that were changed by the C# process are read and converted to PlainText nodes.
     * @param acc The accumulator (unused)
     * @return A visitor that updates modified source files
     */
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                var ctx = Context.wrap(executionContext);
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    return ctx.getModifiedSource(sourceFile);
                }
                return tree;
            }
        };
    }

    /**
     * Constructs the command line to execute the C# Roslyn recipe runner.
     * Aggregates all recipes in the batch, deduplicates packages by highest version,
     * and formats the command with all necessary arguments.
     * @param ctx The context containing recipe batch information
     * @return The complete command line string
     */
    private String getRoslynRecipeRunnerCommand(Context ctx){
        List<RoslynRecipe> recipes = ctx.getRecipesInBatch();
        String nugetPackagesArg = recipes.stream()
                .collect(Collectors.toMap(
                        RoslynRecipe::getNugetPackageName,
                        RoslynRecipe::getNugetPackageVersion,
                        (v1, v2) -> v1.compareTo(v2) > 0 ? v1 : v2,
                        LinkedHashMap::new))
                .entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .map(x -> "--package " + x)
                .collect(Collectors.joining(" "));

        String recipeIdsArg = recipes.stream()
                .map(RoslynRecipe::getRecipeId)
                .distinct()
                .map(id -> "--id " + id)
                .collect(Collectors.joining(" "));

        var codeDir = ctx.getCodeDir();

        var recipeCommand = "dotnet ${exec} run-recipe --solution ${solution} ${package} ${recipeId}";
        recipeCommand = recipeCommand.replace("${exec}", Objects.requireNonNull(this.getRoslynRecipeRunnerExecutable()));
        recipeCommand = recipeCommand.replace("${solution}", Objects.requireNonNull(codeDir.toString()));
        recipeCommand = recipeCommand.replace("${package}", Objects.requireNonNull(nugetPackagesArg));
        recipeCommand = recipeCommand.replace("${recipeId}", Objects.requireNonNull(recipeIdsArg));
        return recipeCommand;
    }

    /**
     * Splits a command string into individual arguments for ProcessBuilder.
     * @param command The command string
     * @return List of command arguments
     */
    private List<String> toCommandArguments(String command){
        List<String> args = new ArrayList<>();
        for (String part : command.split(" ")) {
            part = part.trim();
            args.add(part);
        }
        return args;
    }

    /**
     * Executes the C# Roslyn recipe runner process with all collected recipes.
     * This method:
     * 1. Builds the command line with deduplicated packages and recipe IDs
     * 2. Starts the C# process in the working directory
     * 3. Waits for completion (with 5-minute timeout)
     * 4. Detects which files were modified by comparing timestamps
     * @param ctx The context containing working directory and recipe information
     * @throws RuntimeException if the process fails or times out
     */
    @SneakyThrows
    private void runRoslynRecipe(Context ctx) {

        Map<String, String> env = new HashMap<>();

        var command = getRoslynRecipeRunnerCommand(ctx);


        Path out = null, err = null;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(toCommandArguments(command));
            builder.directory(ctx.getCodeDir().toFile());
            builder.environment().put("TERM", "dumb");
            env.forEach(builder.environment()::put);

            out = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "roslyn", null);
            err = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "roslyn", null);
            builder.redirectOutput(ProcessBuilder.Redirect.to(out.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.to(err.toFile()));

            Process process = builder.start();
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                throw new RuntimeException(String.format("Command '%s' timed out after 5 minutes", String.join(" ", command)));
            } else if (process.exitValue() != 0) {
                String error = "Command failed: " + String.join(" ", command);
                if (Files.exists(err)) {
                    error += "\n" + new String(Files.readAllBytes(err));
                }
                error += "\nCommand:" + command;
                throw new RuntimeException(error);
            } else {
                ctx.detectModifiedFiles();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                String content = new String(Files.readAllBytes(out));
                System.out.println(content);
                //noinspection ResultOfMethodCallIgnored
                out.toFile().delete();
            }
            if (err != null) {
                String content = new String(Files.readAllBytes(err));
                System.out.println(content);
                //noinspection ResultOfMethodCallIgnored
                err.toFile().delete();
            }

        }
    }
    private void Test(){}

    /**
     * Gets the path to the C# Roslyn recipe runner executable.
     * Reads from ROSLYN_RECIPE_EXECUTABLE environment variable and ensures it points to Rewrite.Server.dll.
     * @return The full path to the executable
     * @throws IllegalStateException if the environment variable is not set
     */
    private String getRoslynRecipeRunnerExecutable() {
        String executable = System.getenv("ROSLYN_RECIPE_EXECUTABLE");


        if (executable == null) {
            throw new IllegalStateException("ROSLYN_RECIPE_EXECUTABLE environment variable not set");
        }

        if(!executable.endsWith(".dll"))
        {
            executable = ensureTrailingSeparator(executable) + "Rewrite.Server.dll";
        }
        var dir = new File(executable).getParentFile();
        EmbeddedResourceHelper.installDotnetServer(dir);
        return executable;
    }

    /**
     * Ensures a directory path ends with the appropriate file separator for the OS.
     * @param path The directory path
     * @return The path with trailing separator
     */
    public static String ensureTrailingSeparator(@Nullable String path) {
        if (path == null || path.isEmpty()) return File.separator;
        String separator = File.separator;
        if (path.endsWith("/") || path.endsWith("\\")) {
            // Normalize to current OS separator
            if (!path.endsWith(separator)) {
                path = path.substring(0, path.length() - 1) + separator;
            }
            return path;
        }
        return path + separator;
    }

    /**
     * Internal context wrapper that manages state across recipe execution phases.
     * Provides methods for:
     * - Tracking which recipes are in the batch
     * - Managing the working directory and file I/O
     * - Detecting file modifications after C# process execution
     * - Maintaining original file timestamps for comparison
     */
    private static class Context implements ExecutionContext
    {
        private static final String FIRST_RECIPE = RoslynRecipe.class.getName() + ".FIRST_RECIPE";
        private static final String ORIGINAL_FILE_TIMESTAMPS = RoslynRecipe.class.getName() + ".ORIGINAL_FILE_TIMESTAMPS";
        private static final String MODIFIED_FILES = RoslynRecipe.class.getName() + ".MODIFIED_FILES";
        private static final String RECIPE_LIST = RoslynRecipe.class.getName() + ".RECIPE_LIST";

        ExecutionContext ctx;

        public Context(ExecutionContext executionContextContext) {
            this.ctx = executionContextContext;
            WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(); // ensure working directory is created
        }

        public static Context wrap(ExecutionContext context)  {
            return new Context(context);
        }

        public void trySetAsFirstRecipeInBatch(Recipe recipe) {
            ctx.computeMessageIfAbsent(FIRST_RECIPE, x -> recipe);
        }
        public Map<Path, Long> getBeforeModificationTimestamps()
        {
            return ctx.computeMessageIfAbsent(ORIGINAL_FILE_TIMESTAMPS, x -> new HashMap<>());
        }

        public Set<Path> getModifiedFiles()
        {
            return ctx.computeMessageIfAbsent(MODIFIED_FILES, x -> new LinkedHashSet<>());
        }

        public boolean isFirstRecipe(Recipe recipe) {
            Recipe firstRecipe = ctx.getMessage(FIRST_RECIPE);
            return recipe == firstRecipe;
        }

        public Path getCodeDir() {
            Path workingDirectoryRoot = ctx.getMessage(WORKING_DIRECTORY_ROOT);
            if (workingDirectoryRoot == null) {
                WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(); // ensure we have a global root for the whole run
                workingDirectoryRoot = ctx.getMessage(WORKING_DIRECTORY_ROOT);
            }

            assert workingDirectoryRoot != null;

            return Optional.of(workingDirectoryRoot)
                    .map(d -> d.resolve("repo"))
                    .map(d -> {
                        try {
                            if (!Files.exists(d)) {
                                return Files.createDirectory(d).toRealPath();
                            }
                            return d.toRealPath();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .orElseThrow(() -> new IllegalStateException("Failed to create code working directory"));

        }
        boolean isLastRecipe(Recipe recipe) {
            List<RoslynRecipe> recipes = ctx.getMessage(RECIPE_LIST, new ArrayList<>());
            var index = recipes.indexOf(recipe);
            return index == recipes.size() - 1;
//            return ctx.getCycleDetails().getRecipePosition() == recipes.size();
        }
        public List<RoslynRecipe> getRecipesInBatch(){
            return ctx.computeMessageIfAbsent(RECIPE_LIST, x -> new ArrayList<>());
        }

        public void addRecipeInBatch(Recipe recipe) {
            ctx.computeMessage(RECIPE_LIST, null, ArrayList::new, (discard, arr) -> {
                arr.add(recipe);
                return arr;
            });
        }

        public boolean wasModified(SourceFile tree) {
            return getModifiedFiles().contains(getAbsolutePath(tree));
        }

        public Path getAbsolutePath(SourceFile tree) {
            return getCodeDir().resolve(tree.getSourcePath());
        }

        public SourceFile getModifiedSource(SourceFile before) {
            if (!wasModified(before)) {
                return before;
            }
            return new PlainText(
                    before.getId(),
                    before.getSourcePath(),
                    before.getMarkers(),
                    before.getCharset() != null ? before.getCharset().name() : null,
                    before.isCharsetBomMarked(),
                    before.getFileAttributes(),
                    null,
                    readFromDisk(before),
                    emptyList()
            );
        }


        public void writeSource(SourceFile tree) {
            try {
                Path path = getCodeDir().resolve(tree.getSourcePath());
                Files.createDirectories(path.getParent());
                PrintOutputCapture.MarkerPrinter markerPrinter = new PrintOutputCapture.MarkerPrinter() {
                };
                Path written = Files.write(path, tree.printAll(new PrintOutputCapture<>(0, markerPrinter)).getBytes(tree.getCharset() != null ? tree.getCharset() : StandardCharsets.UTF_8));
                getBeforeModificationTimestamps().put(written, Files.getLastModifiedTime(written).toMillis());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private String readFromDisk(SourceFile tree) {
            try {
                Path path = getAbsolutePath(tree);
                return tree.getCharset() != null ? new String(Files.readAllBytes(path), tree.getCharset()) :
                        new String(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @SneakyThrows
        private void detectModifiedFiles()
        {
            var modifiedFiles = getModifiedFiles();
            for (Map.Entry<Path, Long> entry : getBeforeModificationTimestamps().entrySet()) {
                Path path = entry.getKey();
                if (!Files.exists(path) || Files.getLastModifiedTime(path).toMillis() > entry.getValue()) {
                    modifiedFiles.add(path);
                }
            }
        }


        @Override
        public Map<String, @Nullable Object> getMessages() {
            return ctx.getMessages();
        }

        @Override
        public void putMessage(String key, @Nullable Object value) {
            ctx.putMessage(key, value);
        }

        @Override
        public <T> @Nullable T getMessage(String key) {
            return ctx.getMessage(key);
        }

        @Override
        public <T> @Nullable T pollMessage(String key) {
            return ctx.pollMessage(key);
        }

        @Override
        public Consumer<Throwable> getOnError() {
            return ctx.getOnError();
        }

        @Override
        public BiConsumer<Throwable, ExecutionContext> getOnTimeout() {
            return ctx.getOnTimeout();
        }


    }

    /**
     * Accumulator for the ScanningRecipe pattern.
     * Currently empty as all state is maintained in the Context class through ExecutionContext messages.
     * This design allows multiple RoslynRecipe instances to share state during batch execution.
     */
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class Accumulator {

    }
}
