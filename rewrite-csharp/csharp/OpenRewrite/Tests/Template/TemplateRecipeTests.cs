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
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Template;

public class TemplateRecipeTests : RewriteTest
{
    // ===============================================================
    // Template replacement (apply)
    // ===============================================================

    [Fact]
    public void MigrateConsoleWriteToWriteLine()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateConsoleWriteRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(42);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenAlreadyWriteLine()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateConsoleWriteRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Pattern search (find) — search markers with /*~~>*/
    // ===============================================================

    [Fact]
    public void FindConsoleWriteAddsSearchMarker()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindConsoleWriteRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(42);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        /*~~>*/Console.Write(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindConsoleWriteWithDescriptionAddsSearchMarker()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindConsoleWriteWithDescriptionRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(42);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        /*~~(Use Console.WriteLine instead)~~>*/Console.Write(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindDoesNotMarkWhenNoMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindConsoleWriteRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindMarksMultipleOccurrences()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindConsoleWriteRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(1);
                        Console.WriteLine(2);
                        Console.Write(3);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        /*~~>*/Console.Write(1);
                        Console.WriteLine(2);
                        /*~~>*/Console.Write(3);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindBinaryAddExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindBinaryAddRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 + 2;
                        var y = 3 - 4;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        var x = /*~~>*/1 + 2;
                        var y = 3 - 4;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindThrowNewException()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindThrowExceptionRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        throw new Exception("oops");
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        /*~~>*/throw new Exception("oops");
                    }
                }
                """
            )
        );
    }
}

// ===============================================================
// Replacement recipes
// ===============================================================

class MigrateConsoleWriteRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Migrate Console.Write to Console.WriteLine";
    public override string Description => "Replaces Console.Write with Console.WriteLine.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Expression($"Console.Write({expr})");
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({expr})");

        return new MigrateVisitor(pat, tmpl);
    }

    private class MigrateVisitor(CSharpPattern pat, CSharpTemplate tmpl)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            if (pat.Match(mi, Cursor) is { } match)
            {
                return (J)tmpl.Apply(Cursor, values: match)!;
            }
            return mi;
        }
    }
}

// ===============================================================
// Search recipes using CSharpPattern.Find
// ===============================================================

class FindConsoleWriteRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Find Console.Write calls";
    public override string Description => "Marks Console.Write calls with a search marker.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Expression($"Console.Write({expr})");

        return new FindVisitor(pat);
    }

    private class FindVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            return pat.Find(mi, Cursor);
        }
    }
}

class FindConsoleWriteWithDescriptionRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Find Console.Write calls with description";
    public override string Description => "Marks Console.Write calls with a descriptive search marker.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Expression($"Console.Write({expr})");

        return new FindVisitor(pat);
    }

    private class FindVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            return pat.Find(mi, Cursor, "Use Console.WriteLine instead");
        }
    }
}

class FindBinaryAddRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Find addition expressions";
    public override string Description => "Marks binary addition expressions with a search marker.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        var pat = CSharpPattern.Expression($"{lhs} + {rhs}");

        return new FindVisitor(pat);
    }

    private class FindVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            return pat.Find(binary, Cursor);
        }
    }
}

class FindThrowExceptionRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Find throw new Exception";
    public override string Description => "Marks throw new Exception statements with a search marker.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var msg = Capture.Of<Expression>("msg");
        var pat = CSharpPattern.Statement($"throw new Exception({msg})");

        return new FindVisitor(pat);
    }

    private class FindVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitThrow(Throw @throw, ExecutionContext ctx)
        {
            @throw = (Throw)base.VisitThrow(@throw, ctx);
            return pat.Find(@throw, Cursor);
        }
    }
}
