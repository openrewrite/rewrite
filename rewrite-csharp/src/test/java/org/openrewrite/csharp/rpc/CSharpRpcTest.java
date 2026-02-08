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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.tree.J;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for C# RPC communication.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpRpcTest {

    private static CSharpRewriteRpc rpc;

    @BeforeAll
    static void setUp() {
        // Find the C# project path
        Path csharpProjectPath = findCSharpProjectPath();

        CSharpRewriteRpc.setFactory(
                CSharpRewriteRpc.builder()
                        .csharpProjectPath(csharpProjectPath)
                        .traceRpcMessages(true)
                        .log(Paths.get("/tmp/csharp-rpc.log"))
        );

        rpc = CSharpRewriteRpc.getOrStart();
    }

    @AfterAll
    static void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    private static Path findCSharpProjectPath() {
        // Try common locations relative to the test run directory
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
                // From rewrite-csharp module dir
                basePath.resolve("csharp"),
                // From rewrite root dir
                basePath.resolve("rewrite-csharp/csharp"),
        };

        for (Path searchPath : searchPaths) {
            if (searchPath.resolve("src/Rewrite.CSharp/Rewrite.CSharp.csproj").toFile().exists()) {
                return searchPath.toAbsolutePath().normalize();
            }
        }

        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    @Test
    void rpcServerStarts() {
        // Just verify the RPC server starts and responds
        assertThat(rpc).isNotNull();
        assertThat(rpc.getCommand()).contains("dotnet");
    }

    @Test
    void parseAndPrintSimpleClass(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class HelloWorld
                    {
                        public void SayHello()
                        {
                            Console.WriteLine("Hello, World!");
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("HelloWorld.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();

        // Verify it's not a parse error
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        // Verify it's a C# compilation unit
        assertThat(parsed).isInstanceOf(Cs.CompilationUnit.class);

        // Verify print roundtrip - the printed output should match the input
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseClassWithProperties(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Person
                    {
                        public string FirstName { get; set; }
                        public string LastName { get; set; }
                        public int Age { get; set; }

                        public string FullName => $"{FirstName} {LastName}";
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Person.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);
        assertThat(parsed).isInstanceOf(Cs.CompilationUnit.class);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseClassWithUsings(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;
                using System.Collections.Generic;
                using System.Linq;

                namespace Services
                {
                    public class DataService
                    {
                        private readonly List<string> _items = new();

                        public void AddItem(string item)
                        {
                            _items.Add(item);
                        }

                        public IEnumerable<string> GetItems()
                        {
                            return _items.Where(x => !string.IsNullOrEmpty(x));
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("DataService.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);
        assertThat(parsed).isInstanceOf(Cs.CompilationUnit.class);

        Cs.CompilationUnit cu = (Cs.CompilationUnit) parsed;

        // Verify usings were parsed
        assertThat(cu.getUsings()).hasSize(3);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseMultipleFiles(@TempDir Path tempDir) throws IOException {
        String source1 = """
                namespace Models
                {
                    public class Person
                    {
                        public string Name { get; set; }
                    }
                }
                """;

        String source2 = """
                namespace Models
                {
                    public class Address
                    {
                        public string Street { get; set; }
                        public string City { get; set; }
                    }
                }
                """;

        Path file1 = tempDir.resolve("Person.cs");
        Path file2 = tempDir.resolve("Address.cs");
        Files.writeString(file1, source1);
        Files.writeString(file2, source2);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(file1, file2),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(2);

        for (SourceFile sf : sourceFiles) {
            assertThat(sf).isNotInstanceOf(ParseError.class);
            assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);
            assertThat(rpc.print(sf)).isNotBlank();
        }
    }

    @Test
    void parseAsyncAwait(@TempDir Path tempDir) throws IOException {
        String source = """
                using System.Threading.Tasks;

                namespace Services
                {
                    public class AsyncService
                    {
                        public async Task<string> GetDataAsync()
                        {
                            await Task.Delay(100);
                            return "data";
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("AsyncService.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseSwitchExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Utils
                {
                    public class Formatter
                    {
                        public string FormatDay(int day) => day switch
                        {
                            1 => "Monday",
                            2 => "Tuesday",
                            3 => "Wednesday",
                            4 => "Thursday",
                            5 => "Friday",
                            6 => "Saturday",
                            7 => "Sunday",
                            _ => "Unknown"
                        };
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Formatter.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parsePatternMatching(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Utils
                {
                    public class TypeChecker
                    {
                        public string Describe(object obj)
                        {
                            if (obj is string s)
                            {
                                return $"String with length {s.Length}";
                            }
                            else if (obj is int n and > 0)
                            {
                                return $"Positive int: {n}";
                            }
                            else if (obj is null)
                            {
                                return "null";
                            }
                            return "unknown";
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("TypeChecker.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseRecord(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public record Point(int X, int Y);

                    public record Person(string FirstName, string LastName)
                    {
                        public string FullName => $"{FirstName} {LastName}";
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Records.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        // Verify print roundtrip
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }
}
