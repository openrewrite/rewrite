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
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A test expects the recipe to delete a source it runs on through the source's spec.
/// </summary>
public class DeletedSourceTests : RewriteTest
{
    [Fact]
    public void ASourceTheRecipeDeletesIsExpectedDeleted()
    {
        RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")),
            CSharp("class Legacy { }", sourcePath: "Legacy.cs").Deleted(),
            CSharp("class Kept { }", sourcePath: "Kept.cs")
        );
    }

    [Fact]
    public void ASourceTheRecipeKeepsFailsWhenExpectedDeleted()
    {
        var failure = Assert.Throws<TrueException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Other.cs")),
            CSharp("class Legacy { }", sourcePath: "Legacy.cs").Deleted()
        ));

        Assert.Equal("Recipe was expected to delete Legacy.cs but did not change it.", failure.Message);
    }

    [Fact]
    public void ASourceTheRecipeChangesFailsWhenExpectedDeleted()
    {
        var failure = Assert.Throws<TrueException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral("1", "0")),
            CSharp("class Legacy { int x = 1; }", sourcePath: "Legacy.cs").Deleted()
        ));

        Assert.Equal("Recipe was expected to delete Legacy.cs but changed it instead.", failure.Message);
    }

    [Fact]
    public void ASourceExpectedDeletedWithContentAfterIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")),
            CSharp("class Legacy { }", "class Legacy2 { }", "Legacy.cs").Deleted()
        ));

        Assert.Equal("A source the test expects the recipe to delete has no content or trees after the recipe.",
            failure.Message);
    }

    [Fact]
    public void ASourceExpectedDeletedWithoutARecipeIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp("class Legacy { }", sourcePath: "Legacy.cs").Deleted()
        ));

        Assert.Equal("A source the test expects deleted needs a recipe to delete it.", failure.Message);
    }

    [Fact]
    public void ASourceExpectedDeletedInNoCycleIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")).SetExpectedCyclesThatMakeChanges(0),
            CSharp("class Legacy { }", sourcePath: "Legacy.cs").Deleted()
        ));

        Assert.Equal("A source the test expects deleted needs a cycle that makes changes to delete it.",
            failure.Message);
    }

    [Fact]
    public void ASkippedSourceExpectedDeletedIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")),
            CSharp("class Legacy { }", sourcePath: "Legacy.cs").Skip().Deleted()
        ));

        Assert.Equal("A source the test skips is kept from the recipe, which doesn't change it.", failure.Message);
    }

    [Fact]
    public void ASourceTheRecipeDeletesFailsWhenExpectedUnchanged()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")),
            CSharp("class Legacy { }", sourcePath: "Legacy.cs")
        ));

        Assert.Equal("Recipe deleted Legacy.cs, which the test doesn't expect it to delete. " +
                     "Mark the source Deleted() when the recipe is to delete it.", failure.Message);
    }

    [Fact]
    public void ASourceTheRecipeDeletesFailsWhenExpectedChanged()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteSource("Legacy.cs")),
            CSharp("class Legacy { }", "class Legacy2 { }", "Legacy.cs")
        ));

        Assert.Equal("Recipe deleted Legacy.cs, which the test doesn't expect it to delete. " +
                     "Mark the source Deleted() when the recipe is to delete it.", failure.Message);
    }

    private class DeleteSource(string sourcePath) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Delete a source";
        public override string Description => "Deletes the compilation unit at one path.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(sourcePath);

        private class Visitor(string sourcePath) : CSharpVisitor<ExecutionContext>
        {
            public override J? Visit(global::OpenRewrite.Core.Tree? tree, ExecutionContext ctx) =>
                tree is CompilationUnit cu && cu.SourcePath == sourcePath ? null : tree as J;
        }
    }

    private class ChangeLiteral(string from, string to) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change a literal";
        public override string Description => "Changes the integer literals of one value to another.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to);

        private class Visitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitLiteral(Literal literal, ExecutionContext ctx) =>
                literal.ValueSource == from
                    ? literal.WithValue(int.Parse(to)).WithValueSource(to)
                    : base.VisitLiteral(literal, ctx);
        }
    }
}
