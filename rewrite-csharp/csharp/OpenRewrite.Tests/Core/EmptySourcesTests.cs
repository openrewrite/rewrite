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
                    return new RenameFirstClass(checksTheStructure: true).GetVisitor().Visit(cu, ctx) as J ?? cu;
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
