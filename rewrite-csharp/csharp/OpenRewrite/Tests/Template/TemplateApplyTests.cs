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
