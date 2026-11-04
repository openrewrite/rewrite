/*
 * Copyright 2026 the original author or authors.
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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class ArrayTests : RewriteTest
{
    [Fact]
    public void ArrayTypeDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[] arr;
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayTypeWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int [ ] arr;
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithSize()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[10];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[] { 1, 2, 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ImplicitArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var arr = new[] { 1, 2, 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int [ 10 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayInitializerWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[] { 1 , 2 , 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StringArrayDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    string[] names;
                }
                """
            )
        );
    }

    [Fact]
    public void JaggedArrayType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[][] jagged;
                }
                """
            )
        );
    }

    [Fact]
    public void JaggedArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[][] jagged = new int[3][];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAsMethodParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAsReturnType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[] Bar() {
                        return new int[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void EmptyArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] empty = new int[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayInitializerWithTrailingComma()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[] { 1, 2, 3, };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ImplicitArrayWithTrailingComma()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var arr = new[] { 1, 2, 3, };
                    }
                }
                """
            )
        );
    }

    /// <summary>
    /// An array type carries its resolved type like every other <c>TypeTree</c>, so a recipe can
    /// reason about <c>typeof(int[])</c> or an array-typed declaration.
    /// </summary>
    [Fact]
    public void ArrayTypeIsAttributed()
    {
        var arrayType = FirstArrayType(
            """
            class Foo {
                int[] arr;
            }
            """);

        var elementType = Assert.IsType<JavaType.Array>(arrayType.Type).ElemType;
        Assert.Equal(JavaType.Primitive.PrimitiveKind.Int, Assert.IsType<JavaType.Primitive>(elementType).Kind);
    }

    /// <summary>
    /// The outermost rank carries the whole type. The inner levels of a jagged type are left
    /// unattributed: the syntax nests the ranks in the opposite order from the symbol, so there is
    /// no per-level type to hand out.
    /// </summary>
    [Fact]
    public void JaggedArrayTypeIsAttributedAtTheOutermostRank()
    {
        var arrayType = FirstArrayType(
            """
            class Foo {
                int[][] arr;
            }
            """);

        var elementType = Assert.IsType<JavaType.Array>(arrayType.Type).ElemType;
        Assert.IsType<JavaType.Array>(elementType);
    }

    private static ArrayType FirstArrayType(string code)
    {
        var references = Assemblies.Net90
            .ResolveAsync(LanguageNames.CSharp, CancellationToken.None)
            .GetAwaiter().GetResult();
        var syntaxTree = CSharpSyntaxTree.ParseText(code, path: "source.cs");
        var compilation = CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(references)
            .AddSyntaxTrees(syntaxTree);

        var cu = new CSharpParser().Parse(code, semanticModel: compilation.GetSemanticModel(syntaxTree));

        var collector = new ArrayTypeCollector();
        collector.Visit(cu, 0);
        return collector.Found[0];
    }

    private sealed class ArrayTypeCollector : CSharpVisitor<int>
    {
        internal List<ArrayType> Found { get; } = [];

        public override J VisitArrayType(ArrayType arrayType, int ctx)
        {
            Found.Add(arrayType);
            return base.VisitArrayType(arrayType, ctx);
        }
    }
}
