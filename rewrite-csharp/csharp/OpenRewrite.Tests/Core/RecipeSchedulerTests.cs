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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

public class RecipeSchedulerTests
{
    private static readonly CSharpParser Parser = new();
    private static readonly CSharpPrinter<object> Printer = new();

    private static SourceFile Parse(string source, string sourcePath) =>
        Parser.Parse(source, sourcePath: sourcePath);

    private static string Print(SourceFile source) => Printer.Print(source);

    [Fact]
    public void ScanningRecipeWithRecipeListRunsItsOwnScannerAndEditor()
    {
        var original = Parse("class C { }", "c.cs");
        var results = RecipeScheduler.Run(
            new CountingScanningRecipe(new Rename("X", "Y")),
            [original],
            new ExecutionContext());

        var result = Assert.Single(results);
        Assert.Same(original, result.Before);
        Assert.Equal("class C1 { }", Print(result.After!));
    }

    [Fact]
    public void ScanningRecipeWithRecipeListGenerates()
    {
        var results = RecipeScheduler.Run(
            new CountingScanningRecipe(new Rename("X", "Y")) { GenerateFile = true },
            [Parse("class C { }", "c.cs")],
            new ExecutionContext());

        Assert.Contains(results, r => r.Before == null && r.After?.SourcePath == "generated.cs");
    }

    [Fact]
    public void PlainRecipeWithVisitorAndRecipeListRunsBoth()
    {
        // The recipe's own visitor runs before its recipe list, so the child sees the
        // class already renamed to B.
        var results = RecipeScheduler.Run(
            new Rename("A", "B", new Rename("B", "C")),
            [Parse("class A { }", "a.cs")],
            new ExecutionContext());

        var result = Assert.Single(results);
        Assert.Equal("class C { }", Print(result.After!));
    }

    [Fact]
    public void NestedScannerSeesEveryFileBeforeAnyEdit()
    {
        var results = RecipeScheduler.Run(
            new Composite(new CountingScanningRecipe()),
            [
                Parse("class A { }", "a.cs"),
                Parse("class B { }", "b.cs"),
                Parse("class D { }", "d.cs")
            ],
            new ExecutionContext());

        Assert.Equal(3, results.Count);
        Assert.Contains(results, r => r.After != null && Print(r.After) == "class A3 { }");
        Assert.Contains(results, r => r.After != null && Print(r.After) == "class B3 { }");
        Assert.Contains(results, r => r.After != null && Print(r.After) == "class D3 { }");
    }

    [Fact]
    public void ResultBeforeIsOriginalSourceAfterChainedEdits()
    {
        var original = Parse("class A { }", "a.cs");
        var results = RecipeScheduler.Run(
            new Composite(new Rename("A", "B"), new Rename("B", "C")),
            [original],
            new ExecutionContext());

        var result = Assert.Single(results);
        Assert.Same(original, result.Before);
        Assert.Equal("class C { }", Print(result.After!));
    }

    [Fact]
    public void ResultBeforeIsOriginalSourceWhenEditedThenDeleted()
    {
        var original = Parse("class A { }", "a.cs");
        var results = RecipeScheduler.Run(
            new Composite(new Rename("A", "B"), new DeleteAll()),
            [original],
            new ExecutionContext());

        var result = Assert.Single(results);
        Assert.Same(original, result.Before);
        Assert.Null(result.After);
    }

    [Fact]
    public void DeletedFileIsNotVisitedByLaterRecipes()
    {
        var original = Parse("class A { }", "a.cs");
        var results = RecipeScheduler.Run(
            new Composite(new DeleteAll(), new Rename("A", "B")),
            [original],
            new ExecutionContext());

        var result = Assert.Single(results);
        Assert.Same(original, result.Before);
        Assert.Null(result.After);
    }

    private class Counter
    {
        public int Count;
    }

    /// <summary>
    /// Counts the source files during the scan phase, then appends the final count to
    /// every class name during the edit phase. The count therefore only shows up in the
    /// output when the scanner actually ran, and its value reveals how many files were
    /// scanned before editing began.
    /// </summary>
    private class CountingScanningRecipe(params OpenRewrite.Core.Recipe[] children) : ScanningRecipe<Counter>
    {
        public bool GenerateFile { get; init; }

        public override string DisplayName => "Counting scanner";
        public override string Description => "Counts source files and appends the count to class names.";

        public override List<OpenRewrite.Core.Recipe> GetRecipeList() => [.. children];

        public override Counter GetInitialValue(ExecutionContext ctx) => new();

        public override ITreeVisitor<ExecutionContext> GetScanner(Counter acc) => new FileCounter(acc);

        public override ITreeVisitor<ExecutionContext> GetVisitor(Counter acc) => new AppendCountToClassName(acc);

        public override IEnumerable<SourceFile> Generate(Counter acc, ExecutionContext ctx) =>
            GenerateFile ? [Parse("class G { }", "generated.cs")] : [];

        private class FileCounter(Counter acc) : CSharpVisitor<ExecutionContext>
        {
            public override J? Visit(OpenRewrite.Core.Tree? tree, ExecutionContext ctx)
            {
                if (tree is SourceFile)
                {
                    acc.Count++;
                }
                return tree as J;
            }
        }

        private class AppendCountToClassName(Counter acc) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
            {
                return cd.WithName(cd.Name.WithSimpleName(cd.Name.SimpleName + acc.Count));
            }
        }
    }

    private class Rename(string from, string to, params OpenRewrite.Core.Recipe[] children) : OpenRewrite.Core.Recipe
    {
        public override string DisplayName => $"Rename {from} to {to}";
        public override string Description => $"Renames class `{from}` to `{to}`.";

        public override List<OpenRewrite.Core.Recipe> GetRecipeList() => [.. children];

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new RenameVisitor(from, to);

        private class RenameVisitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
            {
                return cd.Name.SimpleName == from ? cd.WithName(cd.Name.WithSimpleName(to)) : cd;
            }
        }
    }

    private class DeleteAll : OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Delete all";
        public override string Description => "Deletes every source file it visits.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new DeletingVisitor();

        private class DeletingVisitor : CSharpVisitor<ExecutionContext>
        {
            public override J? Visit(OpenRewrite.Core.Tree? tree, ExecutionContext ctx) => null;
        }
    }

    private class Composite(params OpenRewrite.Core.Recipe[] children) : OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Composite";
        public override string Description => "A plain composite recipe that only contributes a recipe list.";

        public override List<OpenRewrite.Core.Recipe> GetRecipeList() => [.. children];
    }
}
