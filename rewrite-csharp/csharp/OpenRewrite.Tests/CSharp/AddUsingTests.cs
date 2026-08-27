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
