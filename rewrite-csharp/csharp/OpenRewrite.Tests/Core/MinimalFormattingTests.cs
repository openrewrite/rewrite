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
/// Formatting a whole file or class reformats code the recipe didn't change, which test inputs rarely show because
/// they are formatted already, so the harness fails a recipe that does it.
/// </summary>
public class MinimalFormattingTests : RewriteTest
{
    private static string Calls(string name) =>
        $$"""
          class C
          {
              void M()
              {
                  {{name}}();
              }

              void Foo()
              {
              }
          }
          """;

    [Fact]
    public void FormattingTheWholeFileAfterOneEditFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.File)),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.StartsWith(
            "Format only what the recipe changed, not the whole class, namespace or file around it. These calls " +
            "auto-formatted a whole file, namespace or class that existed before the run, with no stopAfter:\n" +
            "  OpenRewrite.Tests.Core.MinimalFormattingTests+RenameFooThenFormat:\n" +
            "    MinimalFormattingTests.RenameFooThenFormat.Visitor.VisitCompilationUnit(MinimalFormattingTests.cs:",
            failure.Message);
        Assert.Contains(") called AutoFormat on\n      all of source.cs\nThat rewrites the whitespace", failure.Message);
        Assert.EndsWith("opts out with spec.SetValidations(new Validations { MinimalFormatting = false }).",
            failure.Message);
    }

    /// <summary>The failure names the recipe of a composite that asked for the format.</summary>
    [Fact]
    public void FormattingTheWholeFileInARecipeListFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new Composite(new RenameFooThenFormat(Format.File))),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains(
            "\n  OpenRewrite.Tests.Core.MinimalFormattingTests+RenameFooThenFormat:\n" +
            "    MinimalFormattingTests.RenameFooThenFormat.Visitor.VisitCompilationUnit(MinimalFormattingTests.cs:",
            failure.Message);
        Assert.DoesNotContain("+Composite", failure.Message);
    }

    [Fact]
    public void FormattingTheWholeClassAfterOneEditFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.Class)),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains(
            "\n    MinimalFormattingTests.RenameFooThenFormat.Visitor.VisitClassDeclaration(MinimalFormattingTests.cs:",
            failure.Message);
        Assert.Contains(") called MaybeAutoFormat on\n      class `C` of source.cs\n", failure.Message);
    }

    [Fact]
    public void AnAutoFormatVisitorRunAfterTheVisitFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.AfterVisit)),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains(
            "\n  OpenRewrite.Tests.Core.MinimalFormattingTests+RenameFooThenFormat:\n" +
            "    A visitor's DoAfterVisit(new AutoFormatVisitor<P>()) ran on\n      all of source.cs\n",
            failure.Message);
    }

    /// <summary>The failure names the after-visit, not the precondition that ran the visitor.</summary>
    [Fact]
    public void AnAutoFormatVisitorRunAfterAVisitBehindAPreconditionFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new BehindAPrecondition(new RenameFooThenFormat(Format.AfterVisit))),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains(
            "\n  OpenRewrite.Tests.Core.MinimalFormattingTests+BehindAPrecondition:\n" +
            "    A visitor's DoAfterVisit(new AutoFormatVisitor<P>()) ran on\n      all of source.cs\n",
            failure.Message);
    }

    /// <summary>A recipe whose own visitor is a formatter may format whole files, behind a precondition too.</summary>
    [Fact]
    public void AFormatterBehindAPreconditionPassesAlongsideAnEdit()
    {
        RewriteRun(
            spec => spec.SetRecipe(new Composite(new RenameFooThenFormat(Format.Call),
                new BehindAPrecondition(new FormatEverything()))),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void AScanningRecipeWhoseVisitorIsAFormatterPassesAlongsideAnEdit()
    {
        RewriteRun(
            spec => spec.SetRecipe(new Composite(new RenameFooThenFormat(Format.Call), new FormatScanned())),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void FormattingOnlyTheChangedElementPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.Call)),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void FormattingOnlyTheClassHeaderPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.ClassHeader)),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    /// <summary>MaybeAutoFormat formats the class once the visit is done, with the body put back.</summary>
    [Fact]
    public void FormattingTheClassWithItsBodySetAsideLaterFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.ClassWithBodySetAsideLater)),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains(") called MaybeAutoFormat on\n      class `C` of source.cs\n", failure.Message);
    }

    [Fact]
    public void FormattingTheClassWithItsBodySetAsidePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.ClassWithBodySetAside)),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void AFormattingRecipePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FormatEverything()),
            CSharp(
                "class C\n{\nvoid M()\n{\n}\n}\n",
                "class C\n{\n    void M()\n    {\n    }\n}\n"
            )
        );
    }

    [Fact]
    public void FormattingThatOnlyChangesWhitespacePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFooThenFormat(Format.FileAlways)),
            CSharp(
                "class C\n{\nvoid M()\n{\n}\n}\n",
                "class C\n{\n    void M()\n    {\n    }\n}\n"
            )
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameFooThenFormat(Format.File))
                .SetValidations(new Validations { MinimalFormatting = false }),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    public enum Format
    {
        /// <summary>The file, once a call in it was renamed.</summary>
        File,

        /// <summary>The file, whether or not a call in it was renamed.</summary>
        FileAlways,

        /// <summary>The class, once a call in it was renamed.</summary>
        Class,

        /// <summary>The class up to its name, once a call in it was renamed.</summary>
        ClassHeader,

        /// <summary>The class with its body set aside, formatted at once, once a call in it was renamed.</summary>
        ClassWithBodySetAside,

        /// <summary>The class with its body set aside, formatted after the visit, once a call was renamed.</summary>
        ClassWithBodySetAsideLater,

        /// <summary>The renamed call.</summary>
        Call,

        /// <summary>The file, with an <see cref="AutoFormatVisitor{P}"/> after the visit.</summary>
        AfterVisit
    }

    /// <summary>Renames calls to Foo and formats what the test asks for.</summary>
    private class RenameFooThenFormat(Format format) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename Foo to Bar";
        public override string Description => "Renames calls to Foo and formats the code.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(format);

        private class Visitor(Format format) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                var visited = base.VisitCompilationUnit(cu, ctx);
                return format == Format.FileAlways || format == Format.File && !ReferenceEquals(visited, cu)
                    ? AutoFormat(visited, ctx, Cursor)
                    : visited;
            }

            public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
            {
                var visited = base.VisitClassDeclaration(classDecl, ctx);
                return format switch
                {
                    Format.Class => MaybeAutoFormat(classDecl, visited, ctx, Cursor),
                    Format.ClassHeader when visited is ClassDeclaration renamed =>
                        MaybeAutoFormat(classDecl, renamed, renamed.Name, ctx, Cursor.ParentTree),
                    Format.ClassWithBodySetAside or Format.ClassWithBodySetAsideLater
                        when visited is ClassDeclaration renamed && !ReferenceEquals(renamed, classDecl) =>
                        FormatWithBodySetAside(classDecl, renamed, ctx),
                    _ => visited
                };
            }

            /// <summary>Formats the class with an empty body, and then puts its body back.</summary>
            private ClassDeclaration FormatWithBodySetAside(ClassDeclaration classDecl, ClassDeclaration renamed,
                ExecutionContext ctx)
            {
                var aside = renamed.WithBody(renamed.Body.WithStatements([]));
                var formatted = format == Format.ClassWithBodySetAside
                    ? MaybeAutoFormat(classDecl, aside, null, ctx, Cursor.ParentTree)
                    : MaybeAutoFormat(classDecl, aside, ctx, Cursor);
                return formatted.WithBody(renamed.Body);
            }

            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                if (visited is not MethodInvocation { Name.SimpleName: "Foo" } call)
                {
                    return visited;
                }
                var renamed = call.WithName(call.Name.WithSimpleName("Bar"));
                if (format == Format.AfterVisit)
                {
                    DoAfterVisit(new AutoFormatVisitor<ExecutionContext>());
                }
                return format == Format.Call ? MaybeAutoFormat(call, renamed, ctx, Cursor) : renamed;
            }
        }
    }

    /// <summary>A recipe whose visitor is a formatter, which may format whole files.</summary>
    private class FormatEverything : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Format everything";
        public override string Description => "Formats every source file.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new AutoFormatVisitor<ExecutionContext>();
    }

    /// <summary>A scanning recipe whose visitor is a formatter.</summary>
    private class FormatScanned : ScanningRecipe<HashSet<string>>
    {
        public override string DisplayName => "Format scanned files";
        public override string Description => "Formats every source file it scanned.";

        public override HashSet<string> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetScanner(HashSet<string> scanned) =>
            ITreeVisitor<ExecutionContext>.Noop();

        public override ITreeVisitor<ExecutionContext> GetVisitor(HashSet<string> scanned) =>
            new AutoFormatVisitor<ExecutionContext>();
    }

    /// <summary>Runs another recipe's visitor behind a precondition that every source file meets.</summary>
    private class BehindAPrecondition(global::OpenRewrite.Core.Recipe recipe) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => recipe.DisplayName;
        public override string Description => recipe.Description;

        public override ITreeVisitor<ExecutionContext> GetVisitor() =>
            Preconditions.Check(new FindSourceFiles(), recipe.GetVisitor());

        private class FindSourceFiles : TreeVisitor<global::OpenRewrite.Core.Tree, ExecutionContext>
        {
            public override global::OpenRewrite.Core.Tree? Visit(global::OpenRewrite.Core.Tree? tree,
                ExecutionContext ctx) => tree is SourceFile ? SearchResult.Found(tree) : tree;
        }
    }

    private class Composite(params global::OpenRewrite.Core.Recipe[] recipes) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Composite";
        public override string Description => "Runs its recipe list.";

        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [.. recipes];
    }
}
