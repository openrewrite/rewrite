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
/// A recipe edits the source files it is given, and a tool that applies its results finds each by its id and path and
/// writes it in its charset.
/// </summary>
public class SourceFileIdentityTests : RewriteTest
{
    private static string Calls(string name) =>
        $$"""
          class C
          {
              void Foo() { }
              void Bar() { }
              void M()
              {
                  {{name}}();
              }
          }
          """;

    private static readonly Validations Identity = new() { SourceFileIdentity = true };

    [Fact]
    public void ARecipeThatEditsTheSourceFileKeepsItsIdentity()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu)).SetValidations(Identity),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void ARecipeThatGivesTheSourceFileAnotherIdFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu.WithId(global::OpenRewrite.Core.Tree.RandomId())))
                .SetValidations(Identity),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("changed what identifies a source file:\n  source.cs:\n    its id ", failure.Message);
    }

    [Fact]
    public void ARecipeThatMovesTheSourceFileFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu.WithSourcePath("Moved.cs"))).SetValidations(Identity),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("  source.cs:\n    its path became Moved.cs\n", failure.Message);
    }

    [Fact]
    public void ARecipeThatChangesTheCharsetOrByteOrderMarkFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu.WithCharset("UTF-16").WithCharsetBomMarked(true)))
                .SetValidations(Identity),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("    its charset UTF-8 became UTF-16\n    it gained a byte order mark\n", failure.Message);
    }

    [Fact]
    public void ARecipeThatGivesTheSourceFileOtherMarkersFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu.WithMarkers(new Markers(cu.Markers.Id, [..cu.Markers.MarkerList]))))
                .SetValidations(Identity),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("    its markers are other ones\n", failure.Message);
    }

    [Fact]
    public void TheCheckIsOptIn()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCallsAnd(cu => cu.WithSourcePath("Moved.cs"))),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    /// <summary>Renames the calls to Foo to calls to Bar, and adjusts the compilation unit.</summary>
    private class RenameCallsAnd(Func<CompilationUnit, CompilationUnit> adjust) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls and adjust the compilation unit";
        public override string Description => "Renames calls to Foo to Bar and adjusts the compilation unit.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(adjust);

        private class Visitor(Func<CompilationUnit, CompilationUnit> adjust) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit compilationUnit, ExecutionContext ctx)
            {
                var visited = base.VisitCompilationUnit(compilationUnit, ctx);
                return visited is CompilationUnit cu && !ReferenceEquals(cu, compilationUnit) ? adjust(cu) : visited;
            }

            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation { Name.SimpleName: "Foo" } call
                    ? call.WithName(call.Name.WithSimpleName("Bar"))
                    : visited;
            }
        }
    }
}
