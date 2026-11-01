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
using OpenRewrite.CSharp.Format;
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
            spec => spec.SetRecipe(new RenameFirstClass { ChecksTheStructure = false }),
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
            spec => spec.SetRecipe(new RenameFirstClass { ChecksTheStructure = true }),
            CSharp("class A { }", "class B { }")
        );
    }

    /// <summary>A recipe that changes nothing is not run on an empty file.</summary>
    [Fact]
    public void ARecipeThatChangesNothingIsNotProbed()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass { ChecksTheStructure = false }),
            CSharp("class C { }")
        );
    }

    /// <summary>A probe of a recipe with its options runs once in the process, whether it passes or fails.</summary>
    [Fact]
    public void AProbeRunsOncePerProcess()
    {
        Assert.Throws<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass { NewName = "D" }),
            CSharp("class A { }", "class D { }")
        ));

        // the same options aren't probed again, and other options are
        RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass { NewName = "D" }),
            CSharp("class A { }", "class D { }")
        );
        Assert.Throws<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFirstClass { NewName = "E" }),
            CSharp("class A { }", "class E { }")
        ));
    }

    /// <summary>xUnit runs test classes in parallel, and a probe that several reach at once still runs once.</summary>
    [Fact]
    public void AProbeThatTestsReachAtOnceRunsOnce()
    {
        var recipe = new RenameFirstClass { NewName = "F" };
        var runs = 0;
        using var start = new Barrier(8);
        var threads = Enumerable.Range(0, 8).Select(_ => new Thread(() =>
        {
            start.SignalAndWait();
            Probes.Once(recipe, typeof(CompilationUnit), nameof(AProbeThatTestsReachAtOnceRunsOnce),
                () => Interlocked.Increment(ref runs));
        })).ToList();
        threads.ForEach(thread => thread.Start());
        threads.ForEach(thread => thread.Join());

        Assert.Equal(1, runs);
    }

    [Fact]
    public void ARecipeWhoseProbesTookTwentySecondsIsNotProbed()
    {
        var recipe = new OverBudget();
        Probes.Charge(recipe, TimeSpan.FromSeconds(21));

        RewriteRun(
            spec => spec.SetRecipe(recipe),
            CSharp("class A { }", "class B { }")
        );
    }

    /// <summary>
    /// Production skips what the formatter can't reconcile, so the probe does too, without switching off the
    /// reconciler's failures for the tests running alongside.
    /// </summary>
    [Fact]
    public void AFormatTheReconcilerRejectsOnAnEmptyFilePasses()
    {
        var recipe = new FillAnEmptyFile();
        RewriteRun(
            spec => spec.SetRecipe(recipe),
            CSharp("class A { }", "class B { }")
        );

        Assert.True(recipe.ReconcilerThrew);
    }

    private class RenameFirstClass : global::OpenRewrite.Core.Recipe
    {
        [Option(DisplayName = "Checks the structure", Description = "Whether a file without members is left alone.")]
        public bool ChecksTheStructure { get; init; }

        [Option(DisplayName = "New name", Description = "The name to give class A.")]
        public string NewName { get; init; } = "B";

        public override string DisplayName => "Rename the first class";
        public override string Description => "Renames the first class of a file from A to the new name.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(ChecksTheStructure, NewName);

        private class Visitor(bool checksTheStructure, string newName) : CSharpVisitor<ExecutionContext>
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
                    first.WithElement(classDecl.WithName(classDecl.Name.WithSimpleName(newName))),
                    .. cu.Members.Skip(1)
                ]);
            }
        }
    }

    /// <summary>A recipe that no other test runs, so that no other test's probes count toward its time.</summary>
    private class OverBudget : RenameFirstClass;

    /// <summary>
    /// Renames class A to B, and fills an empty file with a class whose field type is an Identifier, where the parser
    /// makes a Primitive, which the formatter's reconciler rejects.
    /// </summary>
    private class FillAnEmptyFile : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Fill an empty file";
        public override string Description => "Renames class A to B and adds a class to an empty file.";

        public bool ReconcilerThrew { get; private set; }

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(this);

        private class Visitor(FillAnEmptyFile recipe) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                if (cu.Members.Count > 0)
                {
                    return new RenameFirstClass { ChecksTheStructure = true }.GetVisitor().Visit(cu, ctx) as J ?? cu;
                }
                var filled = new CSharpParser().Parse("class C\n{\nstring x;\n}\n", sourcePath: cu.SourcePath);
                var classDecl = (ClassDeclaration)filled.Members[0].Element;
                var field = classDecl.Body.Statements[0];
                var mismatched = field.WithElement(((VariableDeclarations)field.Element).WithTypeExpression(
                    new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, [], "string", null, null)));
                try
                {
                    return RoslynFormatter.Format(filled.WithMembers([
                        filled.Members[0].WithElement(classDecl.WithBody(classDecl.Body.WithStatements([mismatched])))
                    ]));
                }
                catch (WhitespaceReconciler.WhitespaceReconcileMismatchException)
                {
                    recipe.ReconcilerThrew = true;
                    throw;
                }
            }
        }
    }
}
