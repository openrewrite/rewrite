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
/// Repositories hold empty files that a test's sources don't show, so the harness runs a recipe that changed a C#
/// file on an empty one too, and fails it if it throws there.
/// </summary>
public class EmptySourcesTests : RewriteTest
{
    [Fact]
    public void ARecipeThatThrowsOnAnEmptyFileFails()
    {
        var failure = Assert.Throws<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass(checksTheStructure: false)),
            CSharp("class A { }", "class B { }")
        ));

        Assert.StartsWith(
            "Check for the code the recipe edits before using it, so that it handles an empty file or leaves it " +
            "unchanged instead of throwing.\n" +
            "OpenRewrite.Tests.Core.EmptySourcesTests+RenameFirstClass threw when run on an empty CompilationUnit " +
            "at source.cs:\n" +
            "    System.ArgumentOutOfRangeException: Index was out of range.",
            failure.Message);
        Assert.Contains(
            "\n        at OpenRewrite.Tests.Core.EmptySourcesTests.RenameFirstClass.Visitor.VisitCompilationUnit(" +
            "EmptySourcesTests.cs:",
            failure.Message);
        Assert.EndsWith("Then add a test with an empty source, CSharp(\"\"), that states the intended result.",
            failure.Message);
        Assert.IsType<ArgumentOutOfRangeException>(failure.InnerException);
    }

    [Fact]
    public void ARecipeThatChecksTheStructurePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass(checksTheStructure: true)),
            CSharp("class A { }", "class B { }")
        );
    }

    /// <summary>A recipe that changes nothing is not run on an empty file.</summary>
    [Fact]
    public void ARecipeThatChangesNothingIsNotProbed()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass(checksTheStructure: false)),
            CSharp("class C { }")
        );
    }

    private class RenameFirstClass(bool checksTheStructure) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename the first class";
        public override string Description => "Renames the first class of a file from A to B.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(checksTheStructure);

        private class Visitor(bool checksTheStructure) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                if (checksTheStructure && cu.Members.Count == 0)
                {
                    return cu;
                }
                var first = cu.Members[0];
                if (first.Element is not ClassDeclaration { Name.SimpleName: "A" } classDecl)
                {
                    return cu;
                }
                return cu.WithMembers([
                    first.WithElement(classDecl.WithName(classDecl.Name.WithSimpleName("B"))),
                    .. cu.Members.Skip(1)
                ]);
            }
        }
    }
}
