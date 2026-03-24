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

public class AnnotateTests : RewriteTest
{
    [Fact]
    public void DefaultAnnotatorAddsSearchResult()
    {
        var pat = CSharpPattern.Expression("Console.WriteLine(\"hello\")");
        RewriteRun(
            spec => spec.SetRecipe(new AnnotateRecipe<MethodInvocation>(pat)),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void DefaultAnnotatorWithDescription()
    {
        var pat = CSharpPattern.Expression("Console.WriteLine(\"hello\")");
        RewriteRun(
            spec => spec.SetRecipe(new AnnotateRecipe<MethodInvocation>(
                pat, description: "found it")),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~(found it)~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void CustomAnnotatorWithMarkupWarn()
    {
        var pat = CSharpPattern.Expression("Console.WriteLine(\"hello\")");
        RewriteRun(
            spec => spec.SetRecipe(new AnnotateRecipe<MethodInvocation>(
                pat, annotator: (node, _, _) => Markup.CreateWarn(node, "Avoid Console.WriteLine"))),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~(Avoid Console.WriteLine)~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void CustomAnnotatorReceivesMatchResult()
    {
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Expression($"Console.WriteLine({expr})");
        RewriteRun(
            spec => spec.SetRecipe(new AnnotateRecipe<MethodInvocation>(
                pat, annotator: (node, _, match) =>
                    Markup.CreateWarn(node, $"Found arg: {match.Get(expr)!.GetType().Name}"))),
            CSharp(
                "class C { void M() { Console.WriteLine(42); } }",
                "class C { void M() { /*~~(Found arg: Literal)~~>*/Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void NoMatchReturnsNodeUnchanged()
    {
        var pat = CSharpPattern.Expression("Console.Write(\"hello\")");
        RewriteRun(
            spec => spec.SetRecipe(new AnnotateRecipe<MethodInvocation>(
                pat, annotator: (node, _, _) => Markup.CreateWarn(node, "should not appear"))),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void FindDelegatesToAnnotate()
    {
        // Verifies that Find() produces the same result as Annotate() with the default annotator
        var pat = CSharpPattern.Expression("Console.WriteLine(\"hello\")");
        RewriteRun(
            spec => spec.SetRecipe(new FindVsAnnotateRecipe(pat)),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    // ===============================================================
    // ToFindVisitor
    // ===============================================================

    [Fact]
    public void ToFindVisitorDefaultSearchResult()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToFindVisitorRecipe(
                CSharpPattern.Expression("Console.WriteLine(\"hello\")")
                    .ToFindVisitor())),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void ToFindVisitorWithDescription()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToFindVisitorRecipe(
                CSharpPattern.Expression("Console.WriteLine(\"hello\")")
                    .ToFindVisitor("found it"))),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~(found it)~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void ToFindVisitorWithCustomAnnotator()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToFindVisitorRecipe(
                CSharpPattern.Expression("Console.WriteLine(\"hello\")")
                    .ToFindVisitor((node, _, _) => Markup.CreateWarn(node, "Avoid Console.WriteLine")))),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~(Avoid Console.WriteLine)~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void ToFindVisitorAnnotatorReceivesMatchResult()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(new ToFindVisitorRecipe(
                CSharpPattern.Expression($"Console.WriteLine({expr})")
                    .ToFindVisitor((node, _, match) =>
                        Markup.CreateWarn(node, $"Found arg: {match.Get(expr)!.GetType().Name}")))),
            CSharp(
                "class C { void M() { Console.WriteLine(42); } }",
                "class C { void M() { /*~~(Found arg: Literal)~~>*/Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void ToFindVisitorNoMatchLeavesUnchanged()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToFindVisitorRecipe(
                CSharpPattern.Expression("Console.Write(\"hello\")")
                    .ToFindVisitor((node, _, _) => Markup.CreateWarn(node, "should not appear")))),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }"
            )
        );
    }
}

/// <summary>
/// Test recipe that uses <see cref="CSharpPattern.Find{T}"/> with a configurable annotator.
/// </summary>
file class AnnotateRecipe<T>(
    CSharpPattern pat,
    Func<T, Cursor, MatchResult, T>? annotator = null,
    string? description = null) : Core.Recipe where T : J
{
    public override string DisplayName => "Annotate test";
    public override string Description => "Test recipe for Annotate.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor(pat, annotator, description);

    private class Visitor(
        CSharpPattern pat,
        Func<T, Cursor, MatchResult, T>? annotator,
        string? description) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is T t)
            {
                return annotator != null
                    ? pat.Find(t, Cursor, annotator)
                    : pat.Find(t, Cursor, description);
            }
            return tree;
        }
    }
}

/// <summary>
/// Test recipe that verifies Find() and Annotate() with SearchResult produce equivalent results.
/// Uses Find() — the point is that Find() should still work after being refactored to use Annotate().
/// </summary>
file class FindVsAnnotateRecipe(CSharpPattern pat) : Core.Recipe
{
    public override string DisplayName => "Find vs Annotate";
    public override string Description => "Verifies Find delegates to Annotate.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor(pat);

    private class Visitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is MethodInvocation mi)
            {
                return pat.Find(mi, Cursor);
            }
            return tree;
        }
    }
}

/// <summary>
/// Thin recipe wrapper around a pre-built visitor from <see cref="CSharpPattern.ToFindVisitor"/>.
/// </summary>
file class ToFindVisitorRecipe(CSharpVisitor<ExecutionContext> visitor) : Core.Recipe
{
    public override string DisplayName => "ToFindVisitor test";
    public override string Description => "Test recipe wrapping ToFindVisitor.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => visitor;
}
