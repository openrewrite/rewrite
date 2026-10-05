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
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// <c>Markup</c> and <c>J.SetPrefix</c> resolve their withers reflectively. Both used to return the
/// node unchanged when the type had none, which turned a modelling gap into a silent no-op:
/// <c>Cs.ExpressionStatement</c> declares neither wither of its own, so every search recipe that
/// marked a bare expression statement (<c>Foo();</c>) reported nothing and looked like a clean run.
/// </summary>
public class MarkupTests : RewriteTest
{
    [Fact]
    public void WarnOnExpressionStatementIsPrinted()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MarkExpressionStatements()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Foo();
                    }

                    void Foo() { }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        /*~~(side effect)~~>*/Foo();
                    }

                    void Foo() { }
                }
                """
            )
        );
    }

    /// <summary>
    /// Marking the statement and marking the expression it wraps print identically — the wrapper has
    /// no syntax of its own — which is exactly why the dropped marker went unnoticed. Recipes should
    /// be free to mark whichever of the two they mean.
    /// </summary>
    [Fact]
    public void MarkingStatementAndExpressionPrintTheSame()
    {
        var parser = new CSharpParser();
        var source = parser.Parse("class C { void M() { Foo(); } void Foo() { } }", sourcePath: "c.cs");
        var printer = new CSharpPrinter<object>();

        var statementMarked = new MarkExpressionStatements().GetVisitor().Visit(source, new ExecutionContext());
        var expressionMarked = new MarkWrappedExpressions().GetVisitor().Visit(source, new ExecutionContext());

        Assert.Equal(printer.Print((SourceFile)expressionMarked!), printer.Print((SourceFile)statementMarked!));
    }

    [Fact]
    public void SetPrefixOnExpressionStatementReachesTheExpression()
    {
        var statement = new ExpressionStatement(Guid.NewGuid(),
            new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty));

        var spaced = J.SetPrefix(statement, Space.SingleSpace);

        Assert.Equal(" ", spaced.Prefix.Whitespace);
        Assert.Equal(" ", spaced.Expression.Prefix.Whitespace);
    }

    [Fact]
    public void MarkupThrowsWhenTheNodeHasNoWithMarkers()
    {
        var ex = Assert.Throws<InvalidOperationException>(
            () => Markup.CreateWarn(new WitherlessNode(), "dropped"));
        Assert.Contains("WithMarkers", ex.Message);
    }

    [Fact]
    public void SetPrefixThrowsWhenTheNodeHasNoWithPrefix()
    {
        var ex = Assert.Throws<InvalidOperationException>(
            () => J.SetPrefix(new WitherlessNode(), Space.SingleSpace));
        Assert.Contains("WithPrefix", ex.Message);
    }

    private class MarkExpressionStatements : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Mark expression statements";
        public override string Description => "Marks every expression statement.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitExpressionStatement(ExpressionStatement es, ExecutionContext ctx) =>
                Markup.CreateWarn((ExpressionStatement)base.VisitExpressionStatement(es, ctx), "side effect");
        }
    }

    private class MarkWrappedExpressions : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Mark wrapped expressions";
        public override string Description => "Marks the expression inside every expression statement.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitExpressionStatement(ExpressionStatement es, ExecutionContext ctx)
            {
                es = (ExpressionStatement)base.VisitExpressionStatement(es, ctx);
                return es.WithExpression(Markup.CreateWarn(es.Expression, "side effect"));
            }
        }
    }

    /// <summary>A node that declares no withers, standing in for a future modelling gap.</summary>
    private sealed class WitherlessNode : J
    {
        public Guid Id { get; } = Guid.NewGuid();
        public Space Prefix => Space.Empty;
        public Markers Markers => Markers.Empty;
        public global::OpenRewrite.Core.Tree WithId(Guid id) => this;
    }
}
