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
using System.Collections;
using System.Runtime.CompilerServices;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;
using OpenRewrite.Xml;
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// An accumulator stays in memory for the whole cycle, so the harness fails a ScanningRecipe whose accumulator keeps
/// trees, cursors, visitors or large markers.
/// </summary>
public class LightweightAccumulatorsTests : RewriteTest
{
    private const string Class =
        """
        class C
        {
            void M()
            {
                Foo();
            }
        }
        """;

    [Fact]
    public void ASourceFileKeptInTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsWhatItScans()),
            CSharp(Class)
        ));

        Assert.StartsWith(
            "Keep the values a recipe needs in its ScanningRecipe accumulator, such as source paths, names, ids and " +
            "positions, and no IHeavyweight objects, such as trees, cursors, visitors, execution contexts and " +
            "MSBuildProject markers:\n" +
            "  OpenRewrite.Tests.Core.LightweightAccumulatorsTests+KeepsWhatItScans keeps\n" +
            "    LightweightAccumulatorsTests.KeepsWhatItScans.Scanned.SourceFiles[source.cs] -> " +
            "CompilationUnit source.cs, after scanning\n" +
            "An accumulator stays in memory from the first source file scanned until the cycle ends",
            failure.Message);
        Assert.EndsWith("opts out with spec.SetValidations(new Validations { LightweightAccumulators = false }).",
            failure.Message);
    }

    [Fact]
    public void AHeavyweightMarkerKeptInTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsTheProject()),
            CsProj(
                """
                <Project>
                </Project>
                """,
                sourcePath: "Directory.Build.props")
        ));

        Assert.Contains("    StrongBox.Value -> MSBuildProject, after scanning\n", failure.Message);
    }

    /// <summary>A target framework of an MSBuildProject marker and the packages it resolves are heavyweight too.</summary>
    [Fact]
    public void WhatAProjectResolvesKeptInTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsWhatTheProjectResolves()),
            CSharp(Class)
        ));

        Assert.Contains(
            "    LightweightAccumulatorsTests.KeepsWhatTheProjectResolves.Resolved.Framework -> TargetFramework, " +
            "after scanning\n", failure.Message);
        Assert.Contains(
            "    LightweightAccumulatorsTests.KeepsWhatTheProjectResolves.Resolved.Packages[0] -> ResolvedPackage, " +
            "after scanning\n", failure.Message);
    }

    /// <summary>A recipe library tags a type of its own that is too large to keep, and the check finds it too.</summary>
    [Fact]
    public void ALibrarysOwnHeavyweightTypeKeptInTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsItsCallGraph()),
            CSharp(Class)
        ));

        Assert.Contains("    StrongBox.Value -> LightweightAccumulatorsTests.CallGraph, after scanning\n",
            failure.Message);
    }

    [Fact]
    public void ACursorKeptInTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsCursors()),
            CSharp(Class)
        ));

        Assert.Contains("    List[0] -> Cursor, after scanning\n", failure.Message);
    }

    [Fact]
    public void ASourceFileKeptWhileEditingFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsWhatItEdits()),
            CSharp(Class)
        ));

        Assert.Contains("    List[0] -> CompilationUnit source.cs, after editing\n", failure.Message);
    }

    /// <summary>A collection class of the recipe's own is read by its fields, not enumerated.</summary>
    [Fact]
    public void ASourceFileKeptInACollectionOfTheRecipesOwnFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new KeepsWhatItScansInItsOwnIndex()),
            CSharp(Class)
        ));

        Assert.Contains(
            "    LightweightAccumulatorsTests.KeepsWhatItScansInItsOwnIndex.Index._files[source.cs] -> " +
            "CompilationUnit source.cs, after scanning\n",
            failure.Message);
    }

    [Fact]
    public void ValuesAndTypeAttributionKeptInTheAccumulatorPass()
    {
        RewriteRun(
            spec => spec.SetRecipe(new KeepsCallsByPath()),
            CSharp(Class)
        );
    }

    /// <summary>A template keeps only the tree of its own code, which a larger repository doesn't make larger.</summary>
    [Fact]
    public void ATemplateKeptInTheAccumulatorPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new KeepsItsTemplate()),
            CSharp(Class, Class.Replace("Foo", "Bar"))
        );
    }

    /// <summary>
    /// The execution context keeps the SDK's build context for the whole run, so an accumulator that refers to it
    /// keeps nothing more.
    /// </summary>
    [Fact]
    public void TheBuildContextKeptInTheAccumulatorPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new KeepsTheBuildContext()),
            CsProj(
                """
                <Project>
                </Project>
                """,
                """
                <!--~~>--><Project>
                </Project>
                """,
                sourcePath: "Directory.Build.props")
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new KeepsWhatItScans())
                .SetValidations(new Validations { LightweightAccumulators = false }),
            CSharp(Class)
        );
    }

    private abstract class ScansOnly<T> : ScanningRecipe<T>
    {
        public override string DisplayName => "Scan";
        public override string Description => "Scans the source files and changes nothing.";

        public override ITreeVisitor<ExecutionContext> GetScanner(T acc) => new Scanner(tree => Scan(acc, tree));

        protected virtual void Scan(T acc, global::OpenRewrite.Core.Tree tree)
        {
        }

        private class Scanner(Action<global::OpenRewrite.Core.Tree> scan) : ITreeVisitor<ExecutionContext>
        {
            public global::OpenRewrite.Core.Tree? Visit(global::OpenRewrite.Core.Tree? tree, ExecutionContext ctx)
            {
                if (tree != null)
                {
                    scan(tree);
                }
                return tree;
            }
        }
    }

    private class KeepsWhatItScans : ScansOnly<KeepsWhatItScans.Scanned>
    {
        public override Scanned GetInitialValue(ExecutionContext ctx) => new();

        protected override void Scan(Scanned acc, global::OpenRewrite.Core.Tree tree)
        {
            if (tree is SourceFile sourceFile)
            {
                acc.SourceFiles[sourceFile.SourcePath] = sourceFile;
            }
        }

        public class Scanned
        {
            public Dictionary<string, SourceFile> SourceFiles { get; } = new();
        }
    }

    private class KeepsTheProject : ScansOnly<StrongBox<MSBuildProject?>>
    {
        public override StrongBox<MSBuildProject?> GetInitialValue(ExecutionContext ctx) => new();

        protected override void Scan(StrongBox<MSBuildProject?> acc, global::OpenRewrite.Core.Tree tree) =>
            acc.Value ??= tree.Markers.FindFirst<MSBuildProject>();
    }

    private class KeepsWhatTheProjectResolves : ScansOnly<KeepsWhatTheProjectResolves.Resolved>
    {
        public override Resolved GetInitialValue(ExecutionContext ctx)
        {
            var package = new ResolvedPackage("Newtonsoft.Json", "13.0.3");
            return new Resolved
            {
                Framework = new TargetFramework("net10.0", resolvedPackages: [package]),
                Packages = [package]
            };
        }

        public class Resolved
        {
            public TargetFramework? Framework { get; init; }
            public List<ResolvedPackage> Packages { get; init; } = [];
        }
    }

    /// <summary>A recipe library's index of every call in a repository, which it tags as too large to keep.</summary>
    private sealed class CallGraph : IHeavyweight
    {
        public Dictionary<string, List<string>> CallersByMethod { get; } = new();
    }

    private class KeepsItsCallGraph : ScansOnly<StrongBox<CallGraph>>
    {
        public override StrongBox<CallGraph> GetInitialValue(ExecutionContext ctx) => new(new CallGraph());
    }

    private class KeepsCursors : ScanningRecipe<List<Cursor>>
    {
        public override string DisplayName => "Keep cursors";
        public override string Description => "Keeps the cursor of each class it scans.";

        public override List<Cursor> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetScanner(List<Cursor> cursors) => new Scanner(cursors);

        private class Scanner(List<Cursor> cursors) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
            {
                cursors.Add(Cursor);
                return base.VisitClassDeclaration(classDecl, ctx);
            }
        }
    }

    private class KeepsWhatItEdits : ScansOnly<List<SourceFile>>
    {
        public override List<SourceFile> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetVisitor(List<SourceFile> edited) => new Editor(edited);

        private class Editor(List<SourceFile> edited) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                edited.Add(cu);
                return cu;
            }
        }
    }

    private class KeepsWhatItScansInItsOwnIndex : ScansOnly<KeepsWhatItScansInItsOwnIndex.Index>
    {
        public override Index GetInitialValue(ExecutionContext ctx) => new();

        protected override void Scan(Index acc, global::OpenRewrite.Core.Tree tree)
        {
            if (tree is SourceFile sourceFile)
            {
                acc.Add(sourceFile);
            }
        }

        public class Index : IEnumerable<SourceFile>
        {
            private readonly Dictionary<string, SourceFile> _files = new();

            public void Add(SourceFile sourceFile) => _files[sourceFile.SourcePath] = sourceFile;

            public IEnumerator<SourceFile> GetEnumerator() =>
                throw new InvalidOperationException("The walk enumerated a collection of the recipe's own");

            IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
        }
    }

    /// <summary>Replaces calls to Foo with a template it keeps in its accumulator.</summary>
    private class KeepsItsTemplate : ScanningRecipe<StrongBox<CSharpTemplate>>
    {
        public override string DisplayName => "Call Bar";
        public override string Description => "Replaces calls to Foo with calls to Bar.";

        public override StrongBox<CSharpTemplate> GetInitialValue(ExecutionContext ctx) =>
            new(CSharpTemplate.Expression("Bar()"));

        public override ITreeVisitor<ExecutionContext> GetScanner(StrongBox<CSharpTemplate> template) =>
            ITreeVisitor<ExecutionContext>.Noop();

        public override ITreeVisitor<ExecutionContext> GetVisitor(StrongBox<CSharpTemplate> template) =>
            new Editor(template.Value!);

        private class Editor(CSharpTemplate template) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx) =>
                method.Name.SimpleName == "Foo" ? template.Apply(Cursor)! : base.VisitMethodInvocation(method, ctx);
        }
    }

    /// <summary>Marks each build file it captured, keeping the build context as the SDK's csproj recipes do.</summary>
    private class KeepsTheBuildContext : ScanningRecipe<DotNetBuildContext>
    {
        public override string DisplayName => "Find build files";
        public override string Description => "Marks each build file that the build context captured.";

        public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

        public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext buildContext) =>
            new BuildContextScanner();

        public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext buildContext) =>
            new Marker(buildContext);

        private class Marker(DotNetBuildContext buildContext) : XmlVisitor<ExecutionContext>
        {
            public override global::OpenRewrite.Xml.Xml VisitDocument(Document document, ExecutionContext ctx) =>
                buildContext.Documents.ContainsKey(document.SourcePath) ? SearchResult.Found(document) : document;
        }
    }

    /// <summary>Keeps the names and types of the methods each source file calls.</summary>
    private class KeepsCallsByPath : ScanningRecipe<Dictionary<string, List<(string Name, JavaType? Type)>>>
    {
        public override string DisplayName => "Keep calls by path";
        public override string Description => "Keeps the name and type of each method call, by source path.";

        public override Dictionary<string, List<(string, JavaType?)>> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetScanner(Dictionary<string, List<(string, JavaType?)>> calls) =>
            new Scanner(calls);

        private class Scanner(Dictionary<string, List<(string, JavaType?)>> calls) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var path = Cursor.FirstEnclosingOrThrow<CompilationUnit>().SourcePath;
                if (!calls.TryGetValue(path, out var inFile))
                {
                    calls[path] = inFile = [];
                }
                inFile.Add((method.Name.SimpleName, method.MethodType));
                return base.VisitMethodInvocation(method, ctx);
            }
        }
    }
}
