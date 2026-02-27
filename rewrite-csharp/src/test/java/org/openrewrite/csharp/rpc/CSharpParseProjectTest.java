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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for C# ParseSolution RPC.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpParseProjectTest {

    private static boolean factoryConfigured = false;
    private CSharpRewriteRpc rpc;

    @BeforeEach
    void setUp() {
        if (!factoryConfigured) {
            Path csharpServerEntry = findCSharpServerEntry();
            CSharpRewriteRpc.setFactory(
                    CSharpRewriteRpc.builder()
                            .csharpServerEntry(csharpServerEntry)
                            .traceRpcMessages(false)
                            .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-project.log"))
            );
            factoryConfigured = true;
        }
        rpc = CSharpRewriteRpc.getOrStart();
    }

    @AfterEach
    void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    private static Path findCSharpServerEntry() {
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
                basePath.resolve("csharp"),
                basePath.resolve("rewrite-csharp/csharp"),
        };

        for (Path searchPath : searchPaths) {
            Path csproj = searchPath.resolve("OpenRewrite/OpenRewrite.csproj");
            if (csproj.toFile().exists()) {
                return csproj.toAbsolutePath().normalize();
            }
        }

        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    @Test
    void parseSimpleProject(@TempDir Path tempDir) throws IOException {
        // Create a minimal .csproj
        Files.writeString(tempDir.resolve("Test.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);

        // Create source files
        Files.writeString(tempDir.resolve("Program.cs"), """
                namespace Test
                {
                    public class Program
                    {
                        public static void Main(string[] args)
                        {
                        }
                    }
                }
                """);

        Files.writeString(tempDir.resolve("Helper.cs"), """
                namespace Test
                {
                    public class Helper
                    {
                        public string GetMessage() => "hello";
                    }
                }
                """);

        List<SourceFile> sourceFiles = rpc.parseSolution(
                tempDir.resolve("Test.csproj"),
                tempDir,
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(2);
        for (SourceFile sf : sourceFiles) {
            assertThat(sf).isNotInstanceOf(ParseError.class);
            assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);
        }
    }

    @Test
    void parseProjectWithNuGetReference(@TempDir Path tempDir) throws IOException {
        // Create a .csproj with a NuGet reference
        Files.writeString(tempDir.resolve("Test.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """);

        Files.writeString(tempDir.resolve("Program.cs"), """
                using Newtonsoft.Json;

                namespace Test
                {
                    public class Program
                    {
                        public string Serialize(object obj)
                        {
                            return JsonConvert.SerializeObject(obj);
                        }
                    }
                }
                """);

        List<SourceFile> sourceFiles = rpc.parseSolution(
                tempDir.resolve("Test.csproj"),
                tempDir,
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile sf = sourceFiles.getFirst();
        assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);

        // Print should round-trip
        String printed = rpc.print(sf);
        assertThat(printed).contains("JsonConvert.SerializeObject");
    }

    @Test
    void parseProjectWithPartialClasses(@TempDir Path tempDir) throws IOException {
        // Create a .csproj
        Files.writeString(tempDir.resolve("Test.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);

        // Create two partial class files
        Files.writeString(tempDir.resolve("PersonProperties.cs"), """
                namespace Models
                {
                    public partial class Person
                    {
                        public string FirstName { get; set; }
                        public string LastName { get; set; }
                    }
                }
                """);

        Files.writeString(tempDir.resolve("PersonMethods.cs"), """
                namespace Models
                {
                    public partial class Person
                    {
                        public string GetFullName()
                        {
                            return FirstName + " " + LastName;
                        }
                    }
                }
                """);

        List<SourceFile> sourceFiles = rpc.parseSolution(
                tempDir.resolve("Test.csproj"),
                tempDir,
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(2);
        for (SourceFile sf : sourceFiles) {
            assertThat(sf).isNotInstanceOf(ParseError.class);
            assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);
        }
    }

    @Test
    void generatedFilesExcludedFromLst(@TempDir Path tempDir) throws IOException {
        // Create a .csproj
        Files.writeString(tempDir.resolve("Test.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);

        // Create a user source file
        Files.writeString(tempDir.resolve("Program.cs"), """
                namespace Test
                {
                    public class Program
                    {
                    }
                }
                """);

        // Simulate source generator output in obj/
        Path generatedDir = tempDir.resolve("obj/Debug/net10.0/generated/MyGenerator/MyGenerator.MySourceGenerator");
        Files.createDirectories(generatedDir);
        Files.writeString(generatedDir.resolve("Generated.cs"), """
                namespace Test
                {
                    public static class GeneratedHelper
                    {
                        public static string Version => "1.0.0";
                    }
                }
                """);

        List<SourceFile> sourceFiles = rpc.parseSolution(
                tempDir.resolve("Test.csproj"),
                tempDir,
                new InMemoryExecutionContext()
        ).toList();

        // Only the user file should be in the LST — generated files are excluded
        assertThat(sourceFiles).hasSize(1);
        SourceFile sf = sourceFiles.getFirst();
        assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);
        assertThat(sf.getSourcePath().toString()).doesNotContain("obj");
    }

    @Test
    void parseProjectRelativePaths(@TempDir Path tempDir) throws IOException {
        // Create project in a subdirectory
        Path projectDir = tempDir.resolve("src/MyApp");
        Files.createDirectories(projectDir);

        Files.writeString(projectDir.resolve("MyApp.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);

        Files.writeString(projectDir.resolve("Program.cs"), """
                namespace MyApp
                {
                    public class Program
                    {
                    }
                }
                """);

        // Parse with rootDir pointing to the temp dir root
        List<SourceFile> sourceFiles = rpc.parseSolution(
                projectDir.resolve("MyApp.csproj"),
                tempDir,
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile sf = sourceFiles.getFirst();

        // Source path should be relative to tempDir, not the project dir
        String sourcePath = sf.getSourcePath().toString();
        assertThat(sourcePath).startsWith("src");
    }

    // ---- Individual solution/project tests for targeted debugging ----
    //
    // These mirror the entry points that DotNetBuildStep would discover for each
    // repo in the working set (pruning logic: walk dirs, skip bin/obj/.vs/packages/TestResults,
    // once a dir has .sln/.slnx/.csproj add it & prune subtree; prefer .sln/.slnx over .csproj).

    private static final Path WORKING_SET = Paths.get("C:/Projects/moderneinc/moderne-cli/working-set-csharp");

    // -- AI Library --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseSemanticKernel() { parseSingleSolution(WORKING_SET.resolve("AI Library/microsoft/semantic-kernel/dotnet/SK-dotnet.slnx")); }

    // -- Algorithm --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseWaveFunctionCollapse() { parseSingleSolution(WORKING_SET.resolve("Algorithm/mxgmn/WaveFunctionCollapse/WaveFunctionCollapse.csproj")); }

    // -- Architecture Sample --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseModularMonolithBuild() { parseSingleSolution(WORKING_SET.resolve("Architecture Sample/kgrzybek/modular-monolith-with-ddd/build/_build.csproj")); }

    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseModularMonolithSolution() { parseSingleSolution(WORKING_SET.resolve("Architecture Sample/kgrzybek/modular-monolith-with-ddd/src/CompanyName.MyMeetings.sln")); }

    // -- CLI Tool --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseBBDown() { parseSingleSolution(WORKING_SET.resolve("CLI Tool/nilaoda/BBDown/BBDown.sln")); }

    // -- Desktop App --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseFlowLauncher() { parseSingleSolution(WORKING_SET.resolve("Desktop App/Flow-Launcher/Flow.Launcher/Flow.Launcher.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseBulkCrapUninstaller() { parseSingleSolution(WORKING_SET.resolve("Desktop App/Klocman/Bulk-Crap-Uninstaller/source/BulkCrapUninstaller.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseScreenToGif() { parseSingleSolution(WORKING_SET.resolve("Desktop App/NickeManarin/ScreenToGif/GifRecorder.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseShareX() { parseSingleSolution(WORKING_SET.resolve("Desktop App/ShareX/ShareX/ShareX.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseYoutubeDownloader() { parseSingleSolution(WORKING_SET.resolve("Desktop App/Tyrrrz/YoutubeDownloader/YoutubeDownloader.sln")); }

    // -- Developer Tool --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseILSpy() { parseSingleSolution(WORKING_SET.resolve("Developer Tool/icsharpcode/ILSpy/ILSpy.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseILSpyInstaller() { parseSingleSolution(WORKING_SET.resolve("Developer Tool/icsharpcode/ILSpy/ILSpy.Installer.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseILSpyVSExtensions() { parseSingleSolution(WORKING_SET.resolve("Developer Tool/icsharpcode/ILSpy/ILSpy.VSExtensions.sln")); }

    // -- Game Framework: MonoGame --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameBuild() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/Build.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameAndroid() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Framework.Android.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameDesktopGL() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Framework.DesktopGL.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameiOS() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Framework.iOS.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameNative() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Framework.Native.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameWindowsDX() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Framework.WindowsDX.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameToolsLinux() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Tools.Linux.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameToolsMac() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Tools.Mac.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseMonoGameToolsWindows() { parseSingleSolution(WORKING_SET.resolve("Game Framework/MonoGame/MonoGame/MonoGame.Tools.Windows.sln")); }

    // -- Library --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parsePolly() { parseSingleSolution(WORKING_SET.resolve("Library/App-vNext/Polly/Polly.slnx")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseDapper() { parseSingleSolution(WORKING_SET.resolve("Library/DapperLib/Dapper/Dapper.sln")); }

    // -- Media Server --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseJellyfin() { parseSingleSolution(WORKING_SET.resolve("Media Server/jellyfin/jellyfin/Jellyfin.sln")); }

    // -- Network Tool --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseNetch() { parseSingleSolution(WORKING_SET.resolve("Network Tool/netchx/netch/Netch.sln")); }

    // -- System Tool --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseWinSW() { parseSingleSolution(WORKING_SET.resolve("System Tool/winsw/winsw/src/WinSW.sln")); }

    // -- Template --
    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseCleanArchitecture() { parseSingleSolution(WORKING_SET.resolve("Template/ardalis/CleanArchitecture/Clean.Architecture.slnx")); }

    @Tag("workingSet") @Test @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void parseCfBuildpackTemplate() { parseSingleSolution(WORKING_SET.resolve("Template/macsux/cf-buildpack-template/MyBuildpack.sln")); }

    // -- UI Framework --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseMaterialDesign() { parseSingleSolution(WORKING_SET.resolve("UI Framework/MaterialDesignInXAML/MaterialDesignInXamlToolkit/MaterialDesignToolkit.Full.slnx")); }

    // -- Web API --
    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseBitwarden() { parseSingleSolution(WORKING_SET.resolve("Web API/bitwarden/server/bitwarden-server.sln")); }

    @Tag("workingSet") @Test @Timeout(value = 1200, unit = TimeUnit.SECONDS)
    void parseJackett() { parseSingleSolution(WORKING_SET.resolve("Web API/Jackett/Jackett/src/Jackett.sln")); }

    /**
     * Parses a single solution file with a fresh RPC instance and 10-minute timeout.
     * Skipped via assumeTrue if the solution file doesn't exist on this machine.
     */
    private void parseSingleSolution(Path solutionPath) {
        assumeTrue(Files.exists(solutionPath), "Solution not found: " + solutionPath);
        Path rootDir = solutionPath.getParent();

        // Restart RPC with extended timeout for large solutions
        CSharpRewriteRpc.shutdownCurrent();
        CSharpRewriteRpc.setFactory(
                CSharpRewriteRpc.builder()
                        .csharpServerEntry(findCSharpServerEntry())
                        .traceRpcMessages(false)
                        .timeout(Duration.ofMinutes(40))
                        .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-project.log"))
        );
        factoryConfigured = true;
        rpc = CSharpRewriteRpc.getOrStart();

        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
            System.err.println("  Execution error: " + t.getMessage());
            t.printStackTrace(System.err);
        });

        List<SourceFile> sourceFiles = rpc.parseSolution(solutionPath, rootDir, ctx).toList();

        int parseErrors = 0;
        for (SourceFile sf : sourceFiles) {
            if (sf instanceof ParseError pe) {
                parseErrors++;
                System.err.println("  PARSE ERROR: " + sf.getSourcePath());
                System.err.println("    " + pe.getText());
            }
        }

        System.out.println("  Parsed " + sourceFiles.size() + " files" +
                (parseErrors > 0 ? ", " + parseErrors + " parse errors" : ""));

        assertThat(sourceFiles).as("Should parse at least one file").isNotEmpty();
        assertThat(parseErrors).as("Parse errors").isZero();
    }

    // ---- Full working set sweep ----

    /**
     * Discovers and parses all .sln/.slnx files under WORKING_SET_ROOT.
     * Set the system property "workingSetRoot" to override the default path.
     * Skipped automatically if the root directory doesn't exist on this machine.
     */
    @Tag("workingSet-full")
    @Test
    @Timeout(value = 3600, unit = TimeUnit.SECONDS)
    void parseWorkingSetSolution() throws IOException {
        String rootProperty = System.getProperty("workingSetRoot",
                "C:/Projects/moderneinc/moderne-cli/working-set-csharp");
        Path workingSetRoot = Paths.get(rootProperty);
        assumeTrue(Files.isDirectory(workingSetRoot),
                "Working set root not found: " + workingSetRoot);

        // Find all .sln and .slnx files
        List<Path> solutionFiles;
        try (var walk = Files.walk(workingSetRoot)) {
            solutionFiles = walk
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".sln") || name.endsWith(".slnx");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        }

        System.out.println("Found " + solutionFiles.size() + " solution files under " + workingSetRoot);
        assumeTrue(!solutionFiles.isEmpty(), "No .sln/.slnx files found under " + workingSetRoot);

        int totalFiles = 0;
        int totalParseErrors = 0;
        int totalSolutions = 0;
        int failedSolutions = 0;

        for (int i = 0; i < solutionFiles.size(); i++) {
            Path solutionPath = solutionFiles.get(i);
            Path rootDir = solutionPath.getParent();
            String relative = workingSetRoot.relativize(solutionPath).toString();

            System.out.println("\n[" + (i + 1) + "/" + solutionFiles.size() + "] Parsing: " + relative);
            System.out.flush();

            // Restart RPC for each solution to avoid state leaks and OOM
            CSharpRewriteRpc.shutdownCurrent();
            CSharpRewriteRpc.setFactory(
                    CSharpRewriteRpc.builder()
                            .csharpServerEntry(findCSharpServerEntry())
                            .traceRpcMessages(false)
                            .timeout(Duration.ofMinutes(40))
                            .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-project.log"))
            );
            factoryConfigured = true;
            rpc = CSharpRewriteRpc.getOrStart();

            InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
                System.err.println("  Execution error: " + t.getMessage());
                t.printStackTrace(System.err);
            });

            try {
                List<SourceFile> sourceFiles = rpc.parseSolution(solutionPath, rootDir, ctx).toList();

                int parseErrors = 0;
                for (SourceFile sf : sourceFiles) {
                    if (sf instanceof ParseError pe) {
                        parseErrors++;
                        System.err.println("  PARSE ERROR: " + sf.getSourcePath());
                        System.err.println("    " + pe.getText());
                    }
                }

                totalFiles += sourceFiles.size();
                totalParseErrors += parseErrors;
                totalSolutions++;

                System.out.println("  OK: " + sourceFiles.size() + " files" +
                        (parseErrors > 0 ? ", " + parseErrors + " parse errors" : ""));
            } catch (Exception e) {
                failedSolutions++;
                System.err.println("  FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
            System.out.flush();
            System.err.flush();
        }

        System.out.println("\n========== SUMMARY ==========");
        System.out.println("Solutions found:  " + solutionFiles.size());
        System.out.println("Solutions parsed: " + totalSolutions);
        System.out.println("Solutions failed: " + failedSolutions);
        System.out.println("Total files:      " + totalFiles);
        System.out.println("Parse errors:     " + totalParseErrors);
        System.out.println("=============================");

        assertThat(failedSolutions).as("Solutions that failed to parse").isZero();
        assertThat(totalParseErrors).as("Total parse errors across all solutions").isZero();
    }
}
