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

namespace OpenRewrite.Tests.CSharp;

public class AddUsingTests : RewriteTest
{
    private static string ApplyAddUsing(string source, string type, bool onlyIfReferenced = true)
    {
        var cu = Parse(source);
        var result = new AddUsing<int>(type, onlyIfReferenced).Visit(cu, 0);
        return new CSharpPrinter<object>().Print(result!);
    }

    private static async Task AssertCompiles(string source)
    {
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("AddUsing", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
    }

    [Fact]
    public void AddsToCompilationUnitUsings()
    {
        var after = ApplyAddUsing(
            """
            using System;

            namespace N
            {
                public class C
                {
                    private Task field;
                }
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            using System;
            using System.Threading.Tasks;

            namespace N
            {
                public class C
                {
                    private Task field;
                }
            }
            """, after);
    }

    [Fact]
    public void SortedFileStaysSorted()
    {
        var after = ApplyAddUsing(
            """
            using System;
            using System.Xml;

            public class C
            {
                private Task field;
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            using System;
            using System.Threading.Tasks;
            using System.Xml;

            public class C
            {
                private Task field;
            }
            """, after);
    }

    [Fact]
    public void UnsortedFileAppendsAfterTheLastUsing()
    {
        var after = ApplyAddUsing(
            """
            using System.Xml;
            using System;

            public class C
            {
                private Task field;
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            using System.Xml;
            using System;
            using System.Threading.Tasks;

            public class C
            {
                private Task field;
            }
            """, after);
    }

    /// <summary>
    /// Visual Studio's "System directives first" order is not ordinal-sorted; the insertion
    /// point honors it when that is the order the file already satisfies.
    /// </summary>
    [Fact]
    public void SystemFirstFileKeepsSystemFirst()
    {
        var after = ApplyAddUsing(
            """
            using System.Xml;
            using Aaa;

            public class C
            {
                private Task field;
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            using System.Threading.Tasks;
            using System.Xml;
            using Aaa;

            public class C
            {
                private Task field;
            }
            """, after);
    }

    [Fact]
    public void AddsIntoBlockScopedNamespaceWhenUsingsLiveThere()
    {
        var after = ApplyAddUsing(
            """
            namespace N
            {
                using System;

                public class C
                {
                    private Task field;
                }
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            namespace N
            {
                using System;
                using System.Threading.Tasks;

                public class C
                {
                    private Task field;
                }
            }
            """, after);
    }

    [Fact]
    public void FileScopedNamespaceKeepsUsingsAtCompilationUnitLevel()
    {
        var after = ApplyAddUsing(
            """
            namespace N;

            public class C
            {
                private Task field;
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            using System.Threading.Tasks;

            namespace N;

            public class C
            {
                private Task field;
            }
            """, after);
    }

    [Fact]
    public void FileWithoutUsingsGetsOneAfterTheLicenseHeader()
    {
        var after = ApplyAddUsing(
            """
            // Copyright example.

            namespace N
            {
                public class C
                {
                    private Task field;
                }
            }
            """,
            "System.Threading.Tasks.Task");

        Assert.Equal(
            """
            // Copyright example.

            using System.Threading.Tasks;

            namespace N
            {
                public class C
                {
                    private Task field;
                }
            }
            """, after);
    }

    [Fact]
    public async Task UsingInASiblingNamespaceIsNotVisible()
    {
        var after = ApplyAddUsing(
            """
            namespace A { using System.Text; class A {} }
            namespace B { class B { StringBuilder x; } }
            """,
            "System.Text.StringBuilder",
            onlyIfReferenced: false);

        Assert.Equal(
            """
            namespace A { using System.Text; class A {} }
            namespace B { using System.Text; class B { StringBuilder x; } }
            """, after);
        await AssertCompiles(after);
    }

    [Fact]
    public async Task UnrelatedUsingInTheFirstNamespaceIsNotTheInsertionPoint()
    {
        var after = ApplyAddUsing(
            """
            namespace A
            {
                using System;

                class A
                {
                }
            }

            namespace B
            {
                class B
                {
                    private StringBuilder field;
                }
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            namespace A
            {
                using System;

                class A
                {
                }
            }

            namespace B
            {
                using System.Text;

                class B
                {
                    private StringBuilder field;
                }
            }
            """, after);
        await AssertCompiles(after);
        var reparsed = Parse(after);
        Assert.Same(reparsed, new AddUsing<int>("System.Text.StringBuilder").Visit(reparsed, 0));
    }

    [Fact]
    public void FirstNamespaceUsingGoesAboveTheCommentsOfTheFirstMember()
    {
        var after = ApplyAddUsing(
            """
            namespace A
            {
                using System;
            }

            namespace B
            {
                // Holds text.
                class B
                {
                    private StringBuilder field;
                }
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            namespace A
            {
                using System;
            }

            namespace B
            {
                using System.Text;

                // Holds text.
                class B
                {
                    private StringBuilder field;
                }
            }
            """, after);
    }

    [Fact]
    public async Task UsingInANestedNamespaceIsNotVisibleToItsParent()
    {
        var after = ApplyAddUsing(
            """
            namespace Outer
            {
                using System;

                namespace Inner
                {
                    using System.Text;

                    class C
                    {
                        private StringBuilder field;
                    }
                }

                class D
                {
                    private StringBuilder field;
                }
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            namespace Outer
            {
                using System;
                using System.Text;

                namespace Inner
                {
                    using System.Text;

                    class C
                    {
                        private StringBuilder field;
                    }
                }

                class D
                {
                    private StringBuilder field;
                }
            }
            """, after);
        await AssertCompiles(after);
    }

    [Fact]
    public async Task UsingInAnEnclosingNamespaceIsReferenceEqualNoOp()
    {
        var source =
            """
            namespace Outer
            {
                using System.Text;

                namespace Inner
                {
                    class C
                    {
                        private StringBuilder field;
                    }
                }
            }
            """;
        var cu = Parse(source);

        var result = new AddUsing<int>("System.Text.StringBuilder").Visit(cu, 0);

        Assert.Same(cu, result);
        await AssertCompiles(source);
    }

    [Fact]
    public async Task ReferencesInSeveralNamespacesGetACompilationUnitUsing()
    {
        var after = ApplyAddUsing(
            """
            namespace A
            {
                using System;

                class A
                {
                    private StringBuilder field;
                }
            }

            namespace B
            {
                class B
                {
                    private StringBuilder field;
                }
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            using System.Text;

            namespace A
            {
                using System;

                class A
                {
                    private StringBuilder field;
                }
            }

            namespace B
            {
                class B
                {
                    private StringBuilder field;
                }
            }
            """, after);
        await AssertCompiles(after);
    }

    [Fact]
    public async Task ReferenceOutsideAnyNamespaceGetsACompilationUnitUsing()
    {
        var after = ApplyAddUsing(
            """
            namespace N
            {
                using System.Text;

                class C
                {
                    private StringBuilder field;
                }
            }

            class D
            {
                private StringBuilder field;
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            using System.Text;

            namespace N
            {
                using System.Text;

                class C
                {
                    private StringBuilder field;
                }
            }

            class D
            {
                private StringBuilder field;
            }
            """, after);
        await AssertCompiles(after);
    }

    [Fact]
    public async Task AddsIntoFileScopedNamespaceWhenUsingsLiveThere()
    {
        var after = ApplyAddUsing(
            """
            namespace N;

            using System;

            public class C
            {
                private StringBuilder field;
            }
            """,
            "System.Text.StringBuilder");

        Assert.Equal(
            """
            namespace N;

            using System;
            using System.Text;

            public class C
            {
                private StringBuilder field;
            }
            """, after);
        await AssertCompiles(after);
    }

    [Fact]
    public void UsingInAFileScopedNamespaceIsReferenceEqualNoOp()
    {
        var cu = Parse(
            """
            namespace N;

            using System.Text;

            public class C
            {
                private StringBuilder field;
            }
            """);

        var result = new AddUsing<int>("System.Text.StringBuilder").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    [Fact]
    public void AlreadyPresentUsingIsReferenceEqualNoOp()
    {
        var cu = Parse(
            """
            using System.Threading.Tasks;

            public class C
            {
                private Task field;
            }
            """);

        var result = new AddUsing<int>("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    [Fact]
    public void GlobalUsingInTheSameFileIsReferenceEqualNoOp()
    {
        var cu = Parse(
            """
            global using System.Threading.Tasks;

            public class C
            {
                private Task field;
            }
            """);

        var result = new AddUsing<int>("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    /// <summary>Adding the using would make the short name ambiguous (CS0104): the file already
    /// binds it to its own type.</summary>
    [Fact]
    public void DeclinesWhenTheSimpleNameIsBoundToAnotherType()
    {
        var cu = Parse(
            """
            namespace N
            {
                public class Task
                {
                }

                public class C
                {
                    private Task field;
                }
            }
            """);

        var result = new AddUsing<int>("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    [Fact]
    public void DeclinesWhenTheTypeIsNotReferenced()
    {
        var cu = Parse(
            """
            using System;

            public class C
            {
            }
            """);

        var result = new AddUsing<int>("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    [Fact]
    public void FullyQualifiedReferenceAloneDoesNotCount()
    {
        var cu = Parse(
            """
            using System;

            public class C
            {
                private System.Threading.Tasks.Task field;
            }
            """);

        var result = new AddUsing<int>("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Same(cu, result);
    }

    [Fact]
    public void AddsWithoutAReferenceWhenNotGatedOnOne()
    {
        var after = ApplyAddUsing(
            """
            using System;

            public class C
            {
            }
            """,
            "System.Threading.Tasks.Task",
            onlyIfReferenced: false);

        Assert.Equal(
            """
            using System;
            using System.Threading.Tasks;

            public class C
            {
            }
            """, after);
    }

    [Fact]
    public void TypeWithoutANamespaceIsANoOp()
    {
        var cu = Parse(
            """
            public class C
            {
            }
            """);

        var result = new AddUsing<int>("Task", onlyIfReferenced: false).Visit(cu, 0);

        Assert.Same(cu, result);
    }

    /// <summary>The CSharpVisitor entry point registers the after-visitor once per type, so a
    /// visitor asking repeatedly still yields a single using.</summary>
    [Fact]
    public void MaybeAddUsingOnCSharpVisitorAddsOnceAfterTheVisit()
    {
        var cu = Parse(
            """
            using System;

            public class C
            {
            }
            """);

        var result = new UsingRequester("System.Threading.Tasks.Task").Visit(cu, 0);

        Assert.Equal(
            """
            using System;
            using System.Threading.Tasks;

            public class C
            {
            }
            """, new CSharpPrinter<object>().Print(result!));
    }

    private sealed class UsingRequester(string type) : CSharpVisitor<int>
    {
        public override J VisitClassDeclaration(ClassDeclaration cd, int p)
        {
            MaybeAddUsing(type, onlyIfReferenced: false);
            MaybeAddUsing(type, onlyIfReferenced: false);
            return base.VisitClassDeclaration(cd, p);
        }
    }
}
