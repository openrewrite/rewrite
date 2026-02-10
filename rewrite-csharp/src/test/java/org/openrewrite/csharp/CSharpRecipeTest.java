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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.*;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.csharp.Assertions.csharp;
import static org.openrewrite.csharp.Assertions.csproj;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpRecipeTest implements RewriteTest {

    @BeforeAll
    static void setUpFactory() {
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
                basePath.resolve("csharp"),
                basePath.resolve("rewrite-csharp/csharp"),
        };
        for (Path searchPath : searchPaths) {
            Path csproj = searchPath.resolve("OpenRewrite/OpenRewrite.csproj");
            if (csproj.toFile().exists()) {
                CSharpRewriteRpc.setFactory(
                        CSharpRewriteRpc.builder()
                                .csharpServerEntry(csproj.toAbsolutePath().normalize())
                                .log(Paths.get("/tmp/csharp-rpc.log"))
                );
                return;
            }
        }
        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    @AfterEach
    void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    @Test
    void verifyMethodTypeAttribution() {
        String source = "using Newtonsoft.Json;\nclass Foo {\n    void Test() {\n        var json = JsonConvert.SerializeObject(\"test\");\n    }\n}\n";
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();

        Parser.Input input = new Parser.Input(
                Paths.get("Test.cs"),
                () -> new java.io.ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))
        );

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(input), Paths.get(""),
                List.of("Newtonsoft.Json@13.0.1"),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile sf = sourceFiles.getFirst();
        assertThat(sf).isInstanceOf(Cs.CompilationUnit.class);

        // Find the method invocation
        Cs.CompilationUnit cu = (Cs.CompilationUnit) sf;
        J.MethodInvocation invocation = findFirstMethodInvocation(cu);
        assertThat(invocation).as("Should find SerializeObject invocation").isNotNull();
        assertThat(invocation.getSimpleName()).isEqualTo("SerializeObject");

        // Check method type
        JavaType.Method methodType = invocation.getMethodType();
        assertThat(methodType).as("MethodType should be populated").isNotNull();
        assertThat(methodType.getName()).isEqualTo("SerializeObject");
        assertThat(methodType.getDeclaringType()).isNotNull();
        assertThat(methodType.getDeclaringType().getFullyQualifiedName())
                .isEqualTo("Newtonsoft.Json.JsonConvert");
    }

    private static J.MethodInvocation findFirstMethodInvocation(Object tree) {
        if (tree instanceof J.MethodInvocation mi) {
            return mi;
        }
        if (tree instanceof Cs.CompilationUnit cu) {
            for (Statement member : cu.getMembers()) {
                J.MethodInvocation result = findFirstMethodInvocation(member);
                if (result != null) return result;
            }
        }
        if (tree instanceof Cs.BlockScopeNamespaceDeclaration ns) {
            for (var member : ns.getMembers()) {
                J.MethodInvocation result = findFirstMethodInvocation(member);
                if (result != null) return result;
            }
        }
        if (tree instanceof J.ClassDeclaration cd) {
            for (Statement stmt : cd.getBody().getStatements()) {
                J.MethodInvocation result = findFirstMethodInvocation(stmt);
                if (result != null) return result;
            }
        }
        if (tree instanceof J.MethodDeclaration md && md.getBody() != null) {
            for (Statement stmt : md.getBody().getStatements()) {
                J.MethodInvocation result = findFirstMethodInvocation(stmt);
                if (result != null) return result;
            }
        }
        if (tree instanceof J.VariableDeclarations vd) {
            for (var v : vd.getVariables()) {
                if (v.getInitializer() != null) {
                    J.MethodInvocation result = findFirstMethodInvocation(v.getInitializer());
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    @Test
    void directRecipeApplication() {
        String source = "using Newtonsoft.Json;\nclass Foo {\n    void Test() {\n        var json = JsonConvert.SerializeObject(\"test\");\n    }\n}\n";
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();

        Parser.Input input = new Parser.Input(
                Paths.get("Test.cs"),
                () -> new java.io.ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))
        );

        List<SourceFile> sourceFiles = rpc.parse(
                List.of(input), Paths.get(""),
                List.of("Newtonsoft.Json@13.0.1"),
                new InMemoryExecutionContext()
        ).toList();

        assertThat(sourceFiles).hasSize(1);
        SourceFile sf = sourceFiles.getFirst();

        // Apply ChangeMethodName recipe directly via visitor
        var recipe = new ChangeMethodName(
                "Newtonsoft.Json.JsonConvert SerializeObject(..)", "ToJson", null, null);
        var visitor = recipe.getVisitor();
        var modified = (SourceFile) visitor.visit(sf, new InMemoryExecutionContext());
        assertThat(modified).as("Recipe visitor should produce a modified tree").isNotSameAs(sf);

        String printed = rpc.print(modified);
        assertThat(printed).contains("ToJson");
        assertThat(printed).doesNotContain("SerializeObject");
    }

    @Test
    void changeMethodName() {
        rewriteRun(
                spec -> spec
                        .recipe(new ChangeMethodName(
                                "Newtonsoft.Json.JsonConvert SerializeObject(..)", "ToJson", null, null))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test");
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.ToJson("test");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void findMethods() {
        rewriteRun(
                spec -> spec
                        .recipe(new FindMethods("Newtonsoft.Json.JsonConvert SerializeObject(..)", null))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test");
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = /*~~>*/JsonConvert.SerializeObject("test");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void findTypes() {
        rewriteRun(
                spec -> spec
                        .recipe(new FindTypes("Newtonsoft.Json.JsonConvert", null))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test");
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = /*~~>*/JsonConvert.SerializeObject("test");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void deleteMethodArgument() {
        rewriteRun(
                spec -> spec
                        .recipe(new DeleteMethodArgument("Newtonsoft.Json.JsonConvert SerializeObject(..)", 1))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test", Formatting.Indented);
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void reorderMethodArguments() {
        rewriteRun(
                spec -> spec
                        .recipe(new ReorderMethodArguments(
                                "Newtonsoft.Json.JsonConvert SerializeObject(..)",
                                new String[]{"formatting", "value"},
                                null,
                                null, null))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject("test", Formatting.Indented);
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test() {
                                var json = JsonConvert.SerializeObject(Formatting.Indented, "test");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void changeType() {
        rewriteRun(
                spec -> spec
                        .recipe(new ChangeType(
                                "Newtonsoft.Json.JsonSerializer",
                                "Newtonsoft.Json.JsonWriter", null))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test(JsonSerializer s) {
                            }
                        }
                        """,
                        """
                        using Newtonsoft.Json;
                        class Foo {
                            void Test(JsonWriter s) {
                            }
                        }
                        """
                )
        );
    }

    @Test
    void changePackage() {
        rewriteRun(
                spec -> spec
                        .recipe(new ChangePackage(
                                "Newtonsoft.Json", "MyJson", true))
                        .parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.1"))
                        .typeValidationOptions(TypeValidation.builder()
                                .allowNonWhitespaceInWhitespace(true)
                                .build()),
                csproj(
                        """
                        <Project Sdk="Microsoft.NET.Sdk">
                          <PropertyGroup>
                            <TargetFramework>net9.0</TargetFramework>
                          </PropertyGroup>
                          <ItemGroup>
                            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
                          </ItemGroup>
                        </Project>
                        """
                ),
                csharp(
                        """
                        class Foo {
                            void Test() {
                                var json = Newtonsoft.Json.JsonConvert.SerializeObject("test");
                            }
                        }
                        """,
                        """
                        class Foo {
                            void Test() {
                                var json = MyJson.JsonConvert.SerializeObject("test");
                            }
                        }
                        """
                )
        );
    }
}
