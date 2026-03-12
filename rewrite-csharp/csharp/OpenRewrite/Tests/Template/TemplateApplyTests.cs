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

public class TemplateApplyTests : RewriteTest
{
    // ===============================================================
    // Pattern match → template apply
    // ===============================================================

    [Fact]
    public void PatternMatchThenTemplateApply()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void NoChangeWhenPatternDoesNotMatch()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void ReplacesMultipleOccurrences()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
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
                        Console.WriteLine(1);
                        Console.WriteLine(2);
                        Console.WriteLine(3);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TemplateWithoutCapturesProducesFixedAst()
    {
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                "Console.Write(42)",
                "Console.WriteLine(\"hello\")")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void TemplateWithRawCodeSplice()
    {
        var expr = Capture.Of<Expression>("expr");
        var methodName = "Info";
        RewriteRun(
            spec => spec.SetRecipe(new RawSpliceRecipe(expr, methodName)),
            CSharp(
                "class C { void M() { logger.Debug(\"msg\"); } }",
                "class C { void M() { logger.Info(\"msg\"); } }"
            )
        );
    }

    [Fact]
    public void ReplacesBinaryExpression()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{lhs} + {rhs}",
                $"{lhs} - {rhs}")),
            CSharp(
                "class C { void M() { var x = 1 + 2; } }",
                "class C { void M() { var x = 1 - 2; } }"
            )
        );
    }

    [Fact]
    public void ReplacesThrowStatement()
    {
        var msg = Capture.Of<Expression>("msg");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Throw>(
                $"throw new Exception({msg})",
                $"throw new ArgumentException({msg})")),
            CSharp(
                "class C { void M() { throw new Exception(\"oops\"); } }",
                "class C { void M() { throw new ArgumentException(\"oops\"); } }"
            )
        );
    }

    [Fact]
    public void ReplacesNewClassExpression()
    {
        var arg = Capture.Of<Expression>("arg");
        RewriteRun(
            spec => spec.SetRecipe(Replace<NewClass>(
                $"new List({arg})",
                $"new ArrayList({arg})")),
            CSharp(
                "class C { void M() { var x = new List(10); } }",
                "class C { void M() { var x = new ArrayList(10); } }"
            )
        );
    }

    [Fact]
    public void PreservesExpressionCapture()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.Write(1 + 2); } }",
                "class C { void M() { Console.WriteLine(1 + 2); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameCapture()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Of<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.{method}({args})",
                $"Trace.{method}({args})")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Trace.Write(42); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesVariadicArgs()
    {
        var args = Capture.Variadic<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Foo({args})",
                $"Bar({args})")),
            CSharp(
                "class C { void M() { Foo(1, 2, 3); } }",
                "class C { void M() { Bar(1, 2, 3); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameAndVariadicArgs()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Variadic<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"new Random().{method}({args})",
                $"Random.Shared.{method}({args})")),
            CSharp(
                "class C { void M() { new Random().Next(10); } }",
                "class C { void M() { Random.Shared.Next(10); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameAndVariadicArgsWithZeroArgs()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Variadic<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"new Random().{method}({args})",
                $"Random.Shared.{method}({args})")),
            CSharp(
                "class C { void M() { new Random().NextDouble(); } }",
                "class C { void M() { Random.Shared.NextDouble(); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesVariadicArgsInNonTrailingPosition()
    {
        var args = Capture.Variadic<Expression>("args");
        var last = Capture.Of<Expression>("last");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Foo({args}, {last})",
                $"Bar({last}, {args})")),
            CSharp(
                "class C { void M() { Foo(1, 2, 3); } }",
                "class C { void M() { Bar(3, 1, 2); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesFieldNameCapture()
    {
        var field = Capture.Of<Identifier>("field");
        RewriteRun(
            spec => spec.SetRecipe(Replace<FieldAccess>(
                $"DateTime.{field}",
                $"DateTimeOffset.{field}")),
            CSharp(
                "class C { void M() { var x = DateTime.Now; } }",
                "class C { void M() { var x = DateTimeOffset.Now; } }"
            )
        );
    }

    [Fact]
    public void ReplacesAssignmentWithCompoundAssignment()
    {
        var x = Capture.Of<Expression>("x");
        var y = Capture.Of<Expression>("y");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Assignment>(
                $"{x} = {x} + {y}",
                $"{x} += {y}")),
            CSharp(
                "class C { int x; void M() { x = x + 2; } }",
                "class C { int x; void M() { x += 2; } }"
            )
        );
    }

    [Fact]
    public void RemovesStatementByReturningNull()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveEmptyStatementRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        ;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReplacesThrowExWithBareThrow()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UseRethrowRecipe()),
            CSharp(
                """
                using System;
                class C
                {
                    void M()
                    {
                        try { } catch (Exception ex) { throw ex; }
                    }
                }
                """,
                """
                using System;
                class C
                {
                    void M()
                    {
                        try { } catch (Exception ex) { throw; }
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Auto-formatting
    // ===============================================================

    [Fact]
    public void AutoFormatsTemplateResultIndentation()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ReplaceWithIfBlockRecipe()),
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
                // Auto-format should fix the internal indentation of the if-block
                // to match the surrounding context (8 spaces for braces, 12 for body)
                """
                class C
                {
                    void M()
                    {
                        if (true)
                        {
                            Console.WriteLine(42);
                        }
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Recipe factories
    // ===============================================================

    private static Core.Recipe Replace<T>(TemplateStringHandler pattern, TemplateStringHandler template)
        where T : J =>
        new PatternReplaceRecipe<T>(CSharpPattern.Create(pattern), CSharpTemplate.Create(template));

    private static Core.Recipe Replace<T>(string pattern, string template) where T : J =>
        new PatternReplaceRecipe<T>(CSharpPattern.Create(pattern), CSharpTemplate.Create(template));
}

/// <summary>
/// Generic replacement recipe that matches nodes of type <typeparamref name="T"/>
/// against a pattern and replaces them using a template.
/// </summary>
file class PatternReplaceRecipe<T>(CSharpPattern pat, CSharpTemplate tmpl) : Core.Recipe where T : J
{
    public override string DisplayName => $"Replace {typeof(T).Name}";
    public override string Description => $"Replaces {typeof(T).Name} matching the pattern with the template.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new ReplaceVisitor(pat, tmpl);

    private class ReplaceVisitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is T t && pat.Match(t, Cursor) is { } match)
            {
                return (J?)tmpl.Apply(Cursor, values: match);
            }
            return tree;
        }
    }
}

/// <summary>
/// Recipe demonstrating Raw.Code splice — replaces logger.Debug({expr}) with logger.{level}({expr}).
/// </summary>
file class RawSpliceRecipe(Capture<Expression> expr, string level) : Core.Recipe
{
    public override string DisplayName => "Replace logger method";
    public override string Description => "Replaces logger.Debug with logger.<level> using Raw.Code splice.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var pat = CSharpPattern.Create($"logger.Debug({expr})");
        var tmpl = CSharpTemplate.Create($"logger.{Raw.Code(level)}({expr})");
        return new ReplaceVisitor(pat, tmpl);
    }

    private class ReplaceVisitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
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

/// <summary>
/// Recipe that removes empty statements (standalone semicolons) from blocks.
/// Tests that VisitBlock properly handles null returns (statement deletion).
/// </summary>
file class RemoveEmptyStatementRecipe : Core.Recipe
{
    public override string DisplayName => "Remove empty statements";
    public override string Description => "Remove standalone semicolons.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitEmpty(Empty empty, ExecutionContext ctx)
        {
            if (Cursor.Parent?.Value is Block)
                return null!;
            return empty;
        }
    }
}

/// <summary>
/// Replaces Console.Write(expr) statement with an if-block containing Console.WriteLine(42).
/// The template produces a single-line if statement that auto-format should expand to multi-line
/// with correct indentation.
/// </summary>
file class ReplaceWithIfBlockRecipe : Core.Recipe
{
    public override string DisplayName => "Replace Console.Write with if block";
    public override string Description => "Wraps Console.Write in an if block.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitExpressionStatement(ExpressionStatement es, ExecutionContext ctx)
        {
            es = (ExpressionStatement)base.VisitExpressionStatement(es, ctx);
            if (es.Expression is MethodInvocation mi &&
                mi.Select?.Element is Identifier { SimpleName: "Console" } &&
                mi.Name.SimpleName == "Write")
            {
                // Multi-line template with 0-based indentation.
                // Auto-format should fix internal indentation to match the target context.
                var tmpl = CSharpTemplate.Create("if (true)\n{\n    Console.WriteLine(42);\n}");
                return (J)tmpl.Apply(Cursor)!;
            }
            return es;
        }
    }
}

file class UseRethrowRecipe : Core.Recipe
{
    public override string DisplayName => "Use rethrow";
    public override string Description => "Replace throw ex with throw.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitThrow(Throw throwStmt, ExecutionContext ctx)
        {
            throwStmt = (Throw)base.VisitThrow(throwStmt, ctx);

            if (throwStmt.Exception is not Identifier thrownId)
                return throwStmt;

            var cursor = Cursor;
            while (cursor.Parent != null)
            {
                cursor = cursor.Parent;
                if (cursor.Value is Try.Catch catchClause)
                {
                    var catchParam = catchClause.Parameter.Tree.Element;
                    if (catchParam is VariableDeclarations vd &&
                        vd.Variables.Count > 0 &&
                        vd.Variables[0].Element.Name.SimpleName == thrownId.SimpleName)
                    {
                        return throwStmt.WithException(
                            new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty));
                    }
                    break;
                }
            }

            return throwStmt;
        }
    }
}
