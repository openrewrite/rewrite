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
/// A run reports a source file as changed when the recipe returns another tree for it, so a recipe with nothing to
/// change returns the tree it was given, not an equal copy.
/// </summary>
public class UnchangedIsSameInstanceTests : RewriteTest
{
    [Fact]
    public void ARecipeThatDeletesAnUnchangedSourceFails()
    {
        var failure = Assert.Throws<TrueException>(() => RewriteRun(
            spec => spec.SetRecipe(new DeleteCompilationUnit()),
            CSharp("class C { }")
        ));

        Assert.Equal("Recipe made unexpected changes to a source file that was not expected to change.",
            failure.Message);
    }

    [Fact]
    public void ARecipeThatReturnsAnEqualCopyFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new CopyCompilationUnit()),
            CSharp("class C { }")
        ));

        Assert.Equal(
            "Return the tree the recipe was given when there is nothing to change.\n" +
            "OpenRewrite.Tests.Core.UnchangedIsSameInstanceTests+CopyCompilationUnit returned a new tree for " +
            "source.cs that prints the same, so the run reports the source file as changed, with an empty diff.",
            failure.Message);
    }

    private class DeleteCompilationUnit : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Delete the compilation unit";
        public override string Description => "Deletes every compilation unit.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J? Visit(global::OpenRewrite.Core.Tree? tree, ExecutionContext ctx) => null;
        }
    }

    private class CopyCompilationUnit : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Copy the compilation unit";
        public override string Description => "Returns a copy of every compilation unit.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx) =>
                cu.WithMarkers(Markers.Build(cu.Markers.MarkerList));
        }
    }
}
