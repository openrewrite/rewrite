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

    // ---- Full working set sweep ----

    /**
     * Discovers and parses all .sln/.slnx files under the working set root.
     * Set the system property "workingSetRoot" to specify the path.
     * Skipped automatically if the property is not set or the directory doesn't exist.
     */
    @Tag("workingSet-full")
    @Test
    @Timeout(value = 3600, unit = TimeUnit.SECONDS)
    void parseWorkingSetSolution() throws IOException {
        String rootProperty = System.getProperty("workingSetRoot");
        assumeTrue(rootProperty != null, "System property 'workingSetRoot' not set");
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
