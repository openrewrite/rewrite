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
/// A recipe that keeps information for itself in markers leaves them on the LST for every recipe
/// and tool after it, so the harness fails it whether or not the printed source changed.
/// </summary>
public class AddedMarkersTests : RewriteTest
{
    private const string CallsFoo =
        """
        class C
        {
            void M()
            {
                Foo();
            }

            void Foo() { }
        }
        """;

    private const string CallsBar =
        """
        class C
        {
            void M()
            {
                Bar();
            }

            void Foo() { }
        }
        """;

    [Fact]
    public void MarkerAddedAlongsideAChangeFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeFooCalls(call => Tag(RenameToBar(call)))),
            CSharp(CallsFoo, CallsBar)
        ));

        Assert.StartsWith(
            "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of " +
            "its output contain:\n" +
            "  source.cs\n" +
            "    OpenRewrite.Tests.Core.AddedMarkersTests+Visited on OpenRewrite.Java.MethodInvocation `Bar()`\n" +
            "Markers travel with the LST to every recipe that runs after this one",
            failure.Message);
        Assert.EndsWith("  - report a finding with SearchResult, and a problem with Markup", failure.Message);
    }

    [Fact]
    public void MarkerAddedWithoutAChangeFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeFooCalls(Tag)),
            CSharp(CallsFoo)
        ));

        Assert.Contains(
            """
              source.cs (the printed source is unchanged)
                OpenRewrite.Tests.Core.AddedMarkersTests+Visited on OpenRewrite.Java.MethodInvocation `Foo()`
            """,
            failure.Message);
    }

    /// <summary>
    /// The build file carries an <see cref="MSBuildProject"/> marker on the source file. That explains
    /// the marker there, not on an element the recipe chose to stash it on.
    /// </summary>
    [Fact]
    public void SourceFileMarkerAttachedToAnElementFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new TagFooCallsWithProject()),
            CsProj(
                """
                <Project>
                </Project>
                """,
                sourcePath: "Directory.Build.props"),
            CSharp(CallsFoo)
        ));

        Assert.Contains(
            """
              source.cs (the printed source is unchanged)
                OpenRewrite.CSharp.MSBuildProject on OpenRewrite.Java.MethodInvocation `Foo()`
            """,
            failure.Message);
    }

    [Fact]
    public void SearchResultPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeFooCalls(call => SearchResult.Found(call))),
            CSharp(
                CallsFoo,
                """
                class C
                {
                    void M()
                    {
                        /*~~>*/Foo();
                    }

                    void Foo() { }
                }
                """
            )
        );
    }

    /// <summary>
    /// The input has no struct, so nothing in it explains the <see cref="Struct"/> marker; parsing the
    /// struct the recipe wrote does.
    /// </summary>
    [Fact]
    public void SyntaxMarkerOfTheWrittenCodePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ClassToStruct()),
            CSharp(
                "class Point { }",
                "struct Point { }"
            )
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ChangeFooCalls(call => Tag(RenameToBar(call))))
                .SetValidations(new Validations { ReportingMarkersOnly = false }),
            CSharp(CallsFoo, CallsBar)
        );
    }

    private static MethodInvocation Tag(MethodInvocation call) =>
        call.WithMarkers(call.Markers.Add(new Visited(Guid.NewGuid())));

    private static MethodInvocation RenameToBar(MethodInvocation call) =>
        call.WithName(call.Name.WithSimpleName("Bar"));

    /// <summary>A marker of the recipe's own, the kind it might use to remember what it changed.</summary>
    private sealed class Visited(Guid id) : Marker
    {
        public Guid Id { get; } = id;
    }

    private class ChangeFooCalls(Func<MethodInvocation, MethodInvocation> change) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change calls to Foo";
        public override string Description => "Applies a change to every call to Foo.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new FooCalls(change);
    }

    /// <summary>Copies the <see cref="MSBuildProject"/> marker of the build file onto every call to Foo.</summary>
    private class TagFooCallsWithProject : ScanningRecipe<List<MSBuildProject>>
    {
        public override string DisplayName => "Tag calls to Foo with the project";
        public override string Description => "Copies the build file's MSBuildProject marker onto calls to Foo.";

        public override List<MSBuildProject> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetScanner(List<MSBuildProject> projects) =>
            new ProjectCollector(projects);

        public override ITreeVisitor<ExecutionContext> GetVisitor(List<MSBuildProject> projects) =>
            new FooCalls(call => projects.Count == 0 ? call : call.WithMarkers(call.Markers.Add(projects[0])));

        private class ProjectCollector(List<MSBuildProject> projects) : ITreeVisitor<ExecutionContext>
        {
            public global::OpenRewrite.Core.Tree? Visit(global::OpenRewrite.Core.Tree? tree, ExecutionContext ctx)
            {
                if (tree?.Markers.FindFirst<MSBuildProject>() is { } project)
                {
                    projects.Add(project);
                }
                return tree;
            }
        }
    }

    private class FooCalls(Func<MethodInvocation, MethodInvocation> change) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
        {
            var visited = base.VisitMethodInvocation(method, ctx);
            return visited is MethodInvocation { Name.SimpleName: "Foo" } call ? change(call) : visited;
        }
    }

    private class ClassToStruct : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Class to struct";
        public override string Description => "Turns every class into a struct.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
            {
                var visited = base.VisitClassDeclaration(classDecl, ctx);
                return visited is ClassDeclaration { ClassKind.Type: ClassDeclaration.KindType.Class } cd &&
                       cd.Markers.FindFirst<Struct>() == null
                    ? cd.WithMarkers(cd.Markers.Add(new Struct(Guid.NewGuid())))
                    : visited;
            }
        }
    }
}
