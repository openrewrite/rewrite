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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for C# RPC communication.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpRpcTest {

    private static boolean factoryConfigured = false;
    private CSharpRewriteRpc rpc;

    @BeforeEach
    void setUp() {
        if (!factoryConfigured) {
            Path csharpServerEntry = findCSharpServerEntry();
            CSharpRewriteRpc.setFactory(
                    CSharpRewriteRpc.builder()
                            .csharpServerEntry(csharpServerEntry)
                            .traceRpcMessages(true)
                            .log(Paths.get("/tmp/csharp-rpc.log"))
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
        // Try common locations relative to the test run directory
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
                // From rewrite-csharp module dir
                basePath.resolve("csharp"),
                // From rewrite root dir
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

        // Verify no unparsed code hiding in spaces

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

    @Test
    void parseRegionDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                        #region Methods
                        public void A()
                        {
                        }
                        #endregion
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Region.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);
        assertThat(parsed).isInstanceOf(Cs.CompilationUnit.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parsePragmaWarningDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                        #pragma warning disable CS0168
                        public void A()
                        {
                            int x;
                        }
                        #pragma warning restore CS0168
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Pragma.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseNullableDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                #nullable enable
                namespace Test
                {
                    public class Foo
                    {
                        public string? Name { get; set; }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Nullable.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseErrorAndWarningDirectives(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                        public void A()
                        {
                            #warning This method is not implemented
                            throw new System.NotImplementedException();
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("ErrorWarning.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseLineDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                        public void A()
                        {
                            #line 200
                            int x = 1;
                            #line default
                            int y = 2;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Line.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parsePragmaWarningMultipleCodes(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                        #pragma warning disable CS0168, CS0219
                        public void A()
                        {
                            int x;
                            int y;
                        }
                        #pragma warning restore CS0168, CS0219
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("PragmaMulti.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseClassWithFields(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Person
                    {
                        private readonly string _name;
                        public static int Count = 0;
                        private readonly List<string> _items = new();
                        private const int MaxItems = 100;
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

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseClassWithConstructor(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Animal
                    {
                        private readonly string _name;

                        public Animal(string name)
                        {
                            _name = name;
                        }

                        public string GetName()
                        {
                            return _name;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Animal.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseConstructorWithInitializer(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Base
                    {
                        public Base(int x)
                        {
                        }
                    }

                    public class Derived : Base
                    {
                        public Derived(int x) : base(x)
                        {
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Derived.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void parseExpressionBodiedConstructor(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Point
                    {
                        private int _x;

                        public Point(int x) => _x = x;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Point.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);

    }

    @Test
    void getMarketplace() {
        RecipeBundle bundle = new RecipeBundle("nuget", "test-recipes",
                null, null, null);
        RecipeMarketplace marketplace = rpc.getMarketplace(bundle);
        assertThat(marketplace).isNotNull();
        // No IRecipeActivator implementations in the base C# project,
        // so the marketplace should be empty
        assertThat(marketplace.getAllRecipes()).isEmpty();
        assertThat(marketplace.getCategories()).isEmpty();
    }

    @Test
    void parseWithTypeAttribution(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;

                namespace TypeTest
                {
                    public class Calculator
                    {
                        public int Add(int a, int b)
                        {
                            return a + b;
                        }

                        public string Greet(string name)
                        {
                            return "Hello, " + name;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Calculator.cs");
        Files.writeString(sourceFile, source);

        // Parse with empty assembly references to trigger compilation creation
        // (framework refs are automatically included)
        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                Collections.emptyList(),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);
        assertThat(parsed).isInstanceOf(Cs.CompilationUnit.class);

        // Verify print roundtrip still works with type attribution
        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);


        // Navigate the tree: CompilationUnit -> namespace -> class -> methods
        Cs.CompilationUnit cu = (Cs.CompilationUnit) parsed;
        assertThat(cu.getMembers()).isNotEmpty();

        // Find the namespace declaration
        Statement namespaceMember = cu.getMembers().getFirst();

        // Find the class declaration within the namespace
        J.ClassDeclaration classDecl = findFirst(namespaceMember, J.ClassDeclaration.class);
        assertThat(classDecl).as("ClassDeclaration should be found in tree").isNotNull();

        // Find the Add method to verify it was parsed correctly
        J.MethodDeclaration addMethod = findMethodByName(classDecl, "Add");
        assertThat(addMethod).as("Add method should be found").isNotNull();

        // Verify type attribution is present on the method
        assertThat(addMethod.getMethodType())
                .as("MethodType should be populated via type attribution")
                .isNotNull();
        assertThat(addMethod.getMethodType().getName()).isEqualTo("Add");
        assertThat(addMethod.getMethodType().getReturnType()).isInstanceOf(JavaType.Primitive.class);
        assertThat(((JavaType.Primitive) addMethod.getMethodType().getReturnType()).getKeyword()).isEqualTo("int");

        // Verify parameter types
        assertThat(addMethod.getMethodType().getParameterTypes()).hasSize(2);
        assertThat(addMethod.getMethodType().getParameterTypes().get(0)).isInstanceOf(JavaType.Primitive.class);
        assertThat(addMethod.getMethodType().getParameterNames()).containsExactly("a", "b");

        // Verify declaring type
        assertThat(addMethod.getMethodType().getDeclaringType()).isNotNull();
        assertThat(addMethod.getMethodType().getDeclaringType().getFullyQualifiedName())
                .isEqualTo("TypeTest.Calculator");

        // Find the Greet method
        J.MethodDeclaration greetMethod = findMethodByName(classDecl, "Greet");
        assertThat(greetMethod).as("Greet method should be found").isNotNull();
        assertThat(greetMethod.getMethodType()).isNotNull();
        assertThat(greetMethod.getMethodType().getName()).isEqualTo("Greet");
        assertThat(greetMethod.getMethodType().getReturnType()).isInstanceOf(JavaType.Primitive.class);
        assertThat(((JavaType.Primitive) greetMethod.getMethodType().getReturnType()).getKeyword()).isEqualTo("String");
    }

    @Test
    void parseBaseExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Animal
                    {
                        public virtual string Name { get; set; }
                    }

                    public class Dog : Animal
                    {
                        public void Speak()
                        {
                            var name = base.Name;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Dog.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseGenericTypes(@TempDir Path tempDir) throws IOException {
        String source = """
                using System.Collections.Generic;

                namespace Models
                {
                    public class Container
                    {
                        private List<string> _items = new List<string>();
                        private Dictionary<string, int> _counts = new Dictionary<string, int>();

                        public List<string> GetItems()
                        {
                            return _items;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Container.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseEventFieldDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;

                namespace Models
                {
                    public class Button
                    {
                        public event EventHandler Click;
                        public event EventHandler<string> TextChanged;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Button.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseLocalFunction(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Calculator
                    {
                        public int Compute(int x)
                        {
                            int Double(int n)
                            {
                                return n * 2;
                            }

                            return Double(x);
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Calculator.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseThrowExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Validator
                    {
                        public string GetValue(bool valid, string value) =>
                            valid ? value : throw new System.InvalidOperationException();
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Validator.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseTypeOfExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class TypeHelper
                    {
                        public System.Type GetIntType()
                        {
                            return typeof(int);
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("TypeHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseSizeOf(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class SizeHelper
                    {
                        public int GetIntSize()
                        {
                            return sizeof(int);
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("SizeHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseLabeledStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class LabelHelper
                    {
                        public void Process()
                        {
                        start:
                            Console.WriteLine("Hello");
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("LabelHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseUnsafeStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class UnsafeHelper
                    {
                        public unsafe int GetSize()
                        {
                            unsafe
                            {
                                return sizeof(int);
                            }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("UnsafeHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseDefaultExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class DefaultHelper
                    {
                        public int GetDefault()
                        {
                            int x = default(int);
                            string s = default;
                            return x;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("DefaultHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseFixedStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class FixedHelper
                    {
                        public unsafe void Process(byte[] data)
                        {
                            fixed (byte* p = data)
                            {
                            }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("FixedHelper.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseInitializerExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Person
                    {
                        public string Name { get; set; }
                        public int Age { get; set; }
                    }

                    public class Factory
                    {
                        public Person Create()
                        {
                            return new Person { Name = "John", Age = 25 };
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Factory.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseDestructorDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                class Resource
                {
                    ~Resource()
                    {
                        Cleanup();
                    }

                    void Cleanup() { }
                }
                """;

        Path sourceFile = tempDir.resolve("Resource.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseNullForgivingOperator(@TempDir Path tempDir) throws IOException {
        String source = """
                class C
                {
                    string x = null!;
                    string y = GetValue()!;

                    static string GetValue() => null!;
                }
                """;

        Path sourceFile = tempDir.resolve("C.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseNullableType(@TempDir Path tempDir) throws IOException {
        String source = """
                class C
                {
                    int? x;
                    string? y;

                    int? Add(int? a, string? b)
                    {
                        return a;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("C.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseLinqQuery(@TempDir Path tempDir) throws IOException {
        String source = """
                using System.Linq;
                class Program
                {
                    void M()
                    {
                        int[] numbers = { 1, 2, 3, 4, 5 };
                        var result = from n in numbers
                                     where n > 2
                                     orderby n descending
                                     select n * 2;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Linq.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @SuppressWarnings("unchecked")
    private static <T> T findFirst(Object tree, Class<T> type) {
        if (type.isInstance(tree)) {
            return (T) tree;
        }
        if (tree instanceof Cs.CompilationUnit cu) {
            for (Statement member : cu.getMembers()) {
                T result = findFirst(member, type);
                if (result != null) return result;
            }
        }
        if (tree instanceof J.ClassDeclaration cd) {
            for (Statement stmt : cd.getBody().getStatements()) {
                T result = findFirst(stmt, type);
                if (result != null) return result;
            }
        }
        // Handle namespace declarations (which are in Cs.CompilationUnit.members)
        if (tree instanceof Statement stmt) {
            // Namespace members are accessible via the visitor pattern, but for
            // simple traversal we check common wrapper types
            if (tree instanceof Cs.BlockScopeNamespaceDeclaration ns) {
                for (var member : ns.getMembers()) {
                    T result = findFirst(member, type);
                    if (result != null) return result;
                }
            }
            if (tree instanceof Cs.FileScopeNamespaceDeclaration ns) {
                for (var member : ns.getMembers()) {
                    T result = findFirst(member, type);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    @Test
    void parseSwitchStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Dispatcher
                    {
                        public void Handle(int code)
                        {
                            switch (code)
                            {
                                case 1:
                                    break;
                                case 2:
                                case 3:
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Dispatcher.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseUsingStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;
                using System.IO;
                namespace App
                {
                    public class FileReader
                    {
                        public void Read()
                        {
                            using (var stream = new MemoryStream())
                            {
                            }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("FileReader.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseCheckedUnchecked(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Math
                    {
                        public int Compute(int x)
                        {
                            checked
                            {
                                return x + 1;
                            }
                        }

                        public int Fast(int x) => unchecked(x * 2);
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Math.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseGotoStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Flow
                    {
                        public void Run()
                        {
                            goto end;
                            end:
                            return;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Flow.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseEnumDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public enum Color
                    {
                        Red,
                        Green = 1,
                        Blue = 2
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Color.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseDelegateDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public delegate void Handler(int x);
                    public delegate T Factory<T>();
                }
                """;

        Path sourceFile = tempDir.resolve("Handler.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseForEachVariable(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Parser
                    {
                        public void Parse()
                        {
                            var pairs = new (int, string)[] { (1, "a"), (2, "b") };
                            foreach (var (num, str) in pairs)
                            {
                            }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Parser.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseIndexerDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Grid
                    {
                        private int[] data = new int[10];

                        public int this[int index]
                        {
                            get { return data[index]; }
                            set { data[index] = value; }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Grid.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseOperatorDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public struct Point
                    {
                        public int X;
                        public int Y;

                        public static Point operator +(Point a, Point b)
                        {
                            return new Point();
                        }

                        public static implicit operator int(Point p) => p.X;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Point.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseRangeExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Slicer
                    {
                        public void Slice()
                        {
                            var arr = new int[] { 1, 2, 3, 4, 5 };
                            var sub = arr[1..3];
                            var rest = arr[2..];
                            var first = arr[..2];
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Slicer.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseCollectionExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                using System.Collections.Generic;
                namespace App
                {
                    public class Builder
                    {
                        public void Build()
                        {
                            List<int> nums = [1, 2, 3];
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Builder.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseTypeConstraints(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Factory<T> where T : class, new()
                    {
                        public T Create()
                        {
                            return new T();
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Factory.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseEventDeclaration(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;
                namespace App
                {
                    public class Button
                    {
                        public event EventHandler Clicked
                        {
                            add { }
                            remove { }
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Button.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseYieldStatement(@TempDir Path tempDir) throws IOException {
        String source = """
                using System.Collections.Generic;
                namespace App
                {
                    public class Generator
                    {
                        public IEnumerable<int> GetNumbers()
                        {
                            yield return 1;
                            yield return 2;
                            yield break;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Generator.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseTupleType(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Data
                    {
                        public (int X, int Y) GetPoint()
                        {
                            return (1, 2);
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Data.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseStackAllocExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;
                namespace App
                {
                    public class Memory
                    {
                        public void Alloc()
                        {
                            Span<int> nums = stackalloc int[3];
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Memory.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseAnonymousObjectCreation(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Demo
                    {
                        public void Run()
                        {
                            var obj = new { Name = "test", Age = 1 };
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseWithExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public record Person(string Name, int Age);

                    public class Demo
                    {
                        public void Run()
                        {
                            var p = new Person("Alice", 30);
                            var p2 = p with { Name = "Bob" };
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseSpreadElement(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Demo
                    {
                        public void Run()
                        {
                            int[] a = [1, 2];
                            int[] b = [..a, 3];
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    // TODO: parseImplicitElementAccess — requires CsArgument type mapping (Cs$CsArgument not yet on Java side)

    @Test
    void parseAliasQualifiedName(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Demo
                    {
                        public void Run()
                        {
                            global::System.Console.WriteLine("hi");
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseAnonymousMethodExpression(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;

                namespace App
                {
                    public class Demo
                    {
                        public void Run()
                        {
                            Func<int, int> f = delegate(int x) { return x + 1; };
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseRefType(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace App
                {
                    public class Demo
                    {
                        private int _value;

                        public ref int GetRef()
                        {
                            return ref _value;
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    // TODO: parsePointerFieldAccess — requires address-of (&) operator support in unsafe context

    @Test
    void parseImplicitStackAlloc(@TempDir Path tempDir) throws IOException {
        String source = """
                using System;

                namespace App
                {
                    public class Demo
                    {
                        public void Run()
                        {
                            Span<int> s = stackalloc[] { 1, 2, 3 };
                        }
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Demo.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    private static J.MethodDeclaration findMethodByName(J.ClassDeclaration classDecl, String name) {
        for (Statement stmt : classDecl.getBody().getStatements()) {
            if (stmt instanceof J.MethodDeclaration md && md.getSimpleName().equals(name)) {
                return md;
            }
        }
        return null;
    }

    // Cs.ClassDeclaration overload removed — now using J.ClassDeclaration everywhere

    @Test
    void parseSimpleIfEndif(@TempDir Path tempDir) throws IOException {
        String source = """
                #if DEBUG
                using System.Diagnostics;
                #endif
                namespace Test
                {
                    public class Foo
                    {
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("IfEndif.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseSimpleIfElse(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                #if DEBUG
                    public class DebugFoo
                    {
                    }
                #else
                    public class ReleaseFoo
                    {
                    }
                #endif
                }
                """;

        Path sourceFile = tempDir.resolve("IfElse.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseKeywordSplittingDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                public
                #if SOMETHING
                record
                #else
                class
                #endif
                MyObject { }
                """;

        Path sourceFile = tempDir.resolve("KeywordSplit.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseNestedDirectives(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                #if A
                    public class Outer
                    {
                #if B
                        public void InnerMethod() { }
                #endif
                    }
                #endif
                }
                """;

        Path sourceFile = tempDir.resolve("Nested.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseElifDirective(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                #if PLATFORM_A
                        public void PlatformA() { }
                #elif PLATFORM_B
                        public void PlatformB() { }
                #else
                        public void Default() { }
                #endif
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Elif.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseDirectiveWithComplexCondition(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Test
                {
                    public class Foo
                    {
                #if DEBUG && !TRACE
                        public void DebugOnly() { }
                #endif
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Complex.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseDirectiveChangingBaseClass(@TempDir Path tempDir) throws IOException {
        // From PR #6678 comment — directives slicing a class declaration at the base class
        String source = """
                #if(IsSupplyBuildpack)
                public partial class MyBuildpack : SupplyBuildpack
                #elif(IsFinalBuildpack)
                public partial class MyBuildpack : FinalBuildpack
                #elif(IsHttpModuleBuildpack || IsHostedServiceBuildpack)
                public partial class MyBuildpack : PluginInjectorBuildpack
                #endif
                {
                }
                """;

        Path sourceFile = tempDir.resolve("BaseClass.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parsePolyfillConditionalClass(@TempDir Path tempDir) throws IOException {
        // From PR #6678 comment — entire class conditionally included as polyfill
        String source = """
                using System;

                namespace Polyfills
                {
                #if !NET6_0_OR_GREATER
                    internal static class ArgumentNullException
                    {
                        public static void ThrowIfNull(object? argument, string? paramName = null)
                        {
                            if (argument is null)
                                throw new System.ArgumentNullException(paramName);
                        }
                    }
                #endif
                }
                """;

        Path sourceFile = tempDir.resolve("Polyfill.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseClassWithMixedMembersAndBoolProperty(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class NavItem
                    {
                        public NavItem(string? type, NavLevel ol)
                            : this(type, false, null, ol)
                        {
                        }

                        public NavItem(string? type, bool isHidden, string? head, NavLevel ol)
                        {
                            Type = type;
                            IsHidden = isHidden;
                            Head = head;
                            Ol = ol;
                        }

                        public string? Type { get; }
                        public bool IsHidden { get; }
                        public string? Head { get; }
                        public NavLevel Ol { get; }
                    }

                    public class NavLevel
                    {
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("NavItem.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseNullCoalescingOperator(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public class Config
                    {
                        private string _name;

                        public Config(string? name)
                        {
                            _name = name ?? "default";
                        }

                        public string Name => _name;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("Config.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }

    @Test
    void parseClassWithOverrideExpressionBodiedProperty(@TempDir Path tempDir) throws IOException {
        String source = """
                namespace Models
                {
                    public enum ContentType
                    {
                        TEXT,
                        BINARY
                    }

                    public abstract class BaseFile
                    {
                        public abstract ContentType FileType { get; }
                    }

                    public class TextFile : BaseFile
                    {
                        public TextFile(string content)
                        {
                            Content = content ?? throw new System.ArgumentNullException(nameof(content));
                        }

                        public string Content { get; }

                        public override ContentType FileType => ContentType.TEXT;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("TextFile.cs");
        Files.writeString(sourceFile, source);

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(sourceFile),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile parsed = sourceFiles.getFirst();
        assertThat(parsed).isNotInstanceOf(ParseError.class);

        String printed = rpc.print(parsed);
        assertThat(printed).isEqualTo(source);
    }
}
