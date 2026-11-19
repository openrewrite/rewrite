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
/// A test inspects the trees of a source before and after the recipe, and sets up what the parser doesn't give a
/// source, such as the project it belongs to, through the source's spec.
/// </summary>
public class SourceSpecHooksTests : RewriteTest
{
    private const string Hashes =
        """
        using System.Security.Cryptography;
        class Test
        {
            byte[] M(byte[] data) => SHA256.Create().ComputeHash(data, 1, data.Length);
        }
        """;

    private static string Calls(string name, string type = "C") =>
        $$"""
          class {{type}}
          {
              void Foo() { }
              void Bar() { }
              void M()
              {
                  {{name}}();
              }
          }
          """;

    [Fact]
    public void AfterRecipeComparesTheBindingsOfTheResultWithTheOriginal()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral("1", "0")).SetReferenceAssemblies(Assemblies.Net48),
            CSharp(Hashes, Hashes.Replace("data, 1,", "data, 0,"))
                .AfterRecipe((before, after) =>
                {
                    var computeHash = Assert.IsType<JavaType.Method>(Call(after, "ComputeHash").MethodType);
                    Assert.Same(Call(before, "ComputeHash").MethodType, computeHash);
                    Assert.True(TypeUtils.IsOfClassType(computeHash.DeclaringType,
                        "System.Security.Cryptography.HashAlgorithm"));
                    Assert.Same(Call(before, "Create").MethodType, Call(after, "Create").MethodType);
                    var ids = new HashSet<Guid>();
                    Assert.All(Nodes(after),
                        node => Assert.True(ids.Add(node.Id), $"Duplicate {node.GetType().Name} ID"));
                })
        );
    }

    [Fact]
    public void AfterRecipeGetsTheParsedTreeTwiceForASourceTheRecipeLeftAlone()
    {
        var called = false;
        RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral("1", "0")),
            CSharp(Calls("Foo")).AfterRecipe((before, after) =>
            {
                Assert.Same(before, after);
                called = true;
            })
        );
        Assert.True(called);
    }

    [Fact]
    public void AfterRecipeWithoutARecipeInspectsTheParsedTree()
    {
        RewriteRun(
            CSharp(Hashes).AfterRecipe(cu => Assert.NotNull(Call(cu, "ComputeHash").MethodType))
        );
    }

    [Fact]
    public void AFailingAfterRecipeAssertionFailsTheTest()
    {
        Assert.Throws<DoesNotContainException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Foo"), Calls("Bar")).AfterRecipe(cu =>
                Assert.DoesNotContain(Nodes(cu).OfType<MethodInvocation>(), call => call.Name.SimpleName == "Bar"))
        ));
    }

    [Fact]
    public void AfterRecipeOfAnotherSourceTypeIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp(Calls("Foo")).AfterRecipe((global::OpenRewrite.Xml.Document _) => { })
        ));

        Assert.Equal("AfterRecipe takes a Document, but source.cs is a CompilationUnit.", failure.Message);
    }

    [Fact]
    public void ExpectResultsSeesTheChangesOfTheRun()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectResults(results => Assert.Equal(["A.cs"], results.Select(r => r.After!.SourcePath))),
            CSharp(Calls("Foo", "A"), Calls("Bar", "A"), "A.cs"),
            CSharp(Calls("Bar", "B"), sourcePath: "B.cs")
        );
    }

    [Fact]
    public void ACSharpSourceIsAtThePathTheTestGivesIt()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Foo"), actual => Calls("Bar"), "src/App/Program.cs")
                .AfterRecipe(cu => Assert.Equal("src/App/Program.cs", cu.SourcePath))
        );
    }

    [Fact]
    public void ACSharpSourceIsAtAPathThatEndsInCs()
    {
        var failure = Assert.Throws<ArgumentException>(() => CSharp(Calls("Foo"), sourcePath: "Program.csx"));

        Assert.StartsWith("A C# source is parsed here at a path that ends in .cs, not Program.csx.", failure.Message);
    }

    [Theory]
    [InlineData("net45")]
    [InlineData("net6.0", "net45")]
    public void TargetFrameworksTheRecipeDoesNotSupportLeaveTheSourceUnchanged(params string[] targetFrameworks)
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar", "net6.0")),
            CSharp(Calls("Foo")).TargetFrameworks(targetFrameworks)
        );
    }

    [Fact]
    public void ASupportedTargetFrameworkLetsTheRecipeChangeTheSource()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar", "net6.0")),
            CSharp(Calls("Foo", "A"), Calls("Bar", "A")).TargetFrameworks("net6.0"),
            CSharp(Calls("Foo", "B"))
        );
    }

    [Fact]
    public void SourcesWithTheSameTargetFrameworksShareAProject()
    {
        var projects = new List<DotNetProject>();
        void Project(CompilationUnit cu) => projects.Add(cu.Markers.FindFirst<DotNetProject>()!);

        RewriteRun(
            CSharp(Calls("Foo", "A")).TargetFrameworks("net6.0").AfterRecipe(Project),
            CSharp(Calls("Bar", "B")).TargetFrameworks("net6.0").AfterRecipe(Project),
            CSharp(Calls("Foo", "D")).TargetFrameworks("net48").AfterRecipe(Project)
        );

        Assert.Same(projects[0], projects[1]);
        Assert.Equal(["Test", "Test", "Test2"], projects.Select(p => p.ProjectName));
        Assert.Equal(["net6.0"], projects[0].TargetFrameworks);
        Assert.Equal("Microsoft.NET.Sdk", projects[0].Sdk);
        Assert.NotEqual(projects[0].Id, projects[2].Id);
    }

    [Fact]
    public void AddMarkersPutsSourcesInTheProjectsATestNames()
    {
        var shared = global::OpenRewrite.Core.Tree.RandomId();
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar", "net6.0"))
                .ExpectResults(results => Assert.All(results,
                    r => Assert.Equal(shared, r.After!.Markers.FindFirst<DotNetProject>()!.Id))),
            CSharp(Calls("Foo", "A"), Calls("Bar", "A")).AddMarkers(new DotNetProject(shared, "Test", ["net6.0"])),
            CSharp(Calls("Foo", "B"), Calls("Bar", "B")).AddMarkers(new DotNetProject(shared, "Test", ["net6.0"]))
        );
    }

    [Fact]
    public void BeforeRecipeAdjustsTheTreeTheRecipeRunsOn()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Foo"))
                .BeforeRecipe(cu => (CompilationUnit)new StripMethodTypes().Visit(cu, 0)!)
                .AfterRecipe(cu => Assert.Null(Call(cu, "Foo").MethodType))
        );
    }

    [Fact]
    public void BeforeRecipeOfAnotherSourceTypeIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp(Calls("Foo")).BeforeRecipe((global::OpenRewrite.Xml.Document doc) => doc)
        ));

        Assert.Equal("BeforeRecipe takes a Document, but source.cs is a CompilationUnit.", failure.Message);
    }

    [Fact]
    public void AnAfterFunctionAssertsOnWhatTheRecipePrinted()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Foo"), actual =>
            {
                Assert.Contains("        Bar();", actual);
                Assert.DoesNotContain("        Foo();", actual);
                return actual;
            })
        );
    }

    [Fact]
    public void AnAfterFunctionComputesTheExpectedText()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Foo"), actual => actual.Replace("Bar();", "Baz();"))
        ));

        Assert.StartsWith("Unexpected result from recipe \"source.cs\":", failure.Message);
    }

    [Fact]
    public void AnAfterFunctionExpectsAChange()
    {
        var failure = Assert.Throws<TrueException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")),
            CSharp(Calls("Bar"), actual => actual)
        ));

        Assert.StartsWith("Recipe was expected to make changes but did not modify the source file.", failure.Message);
    }

    private static MethodInvocation Call(global::OpenRewrite.Core.Tree tree, string name) =>
        Nodes(tree).OfType<MethodInvocation>().First(call => call.Name.SimpleName == name);

    private static List<J> Nodes(global::OpenRewrite.Core.Tree tree)
    {
        var nodes = new List<J>();
        new NodeCollector().Visit(tree, nodes);
        return nodes;
    }

    private class NodeCollector : CSharpVisitor<List<J>>
    {
        public override J? PreVisit(J tree, List<J> nodes)
        {
            nodes.Add(tree);
            return tree;
        }
    }

    private class StripMethodTypes : CSharpVisitor<int>
    {
        public override J VisitMethodInvocation(MethodInvocation method, int p) =>
            ((MethodInvocation)base.VisitMethodInvocation(method, p)).WithMethodType(null);
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

    /// <summary>
    /// Renames calls that have a method type, and only in projects that target just the supported framework if one is
    /// given.
    /// </summary>
    private class RenameCalls(string from, string to, string? supportedFramework = null)
        : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls";
        public override string Description => "Renames the calls to one method.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to, supportedFramework);

        private class Visitor(string from, string to, string? supportedFramework) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitCompilationUnit(CompilationUnit cu, ExecutionContext ctx)
            {
                var targets = cu.Markers.FindFirst<DotNetProject>()?.TargetFrameworks;
                if (supportedFramework != null && !(targets is [var only] && only == supportedFramework))
                {
                    return cu;
                }
                return base.VisitCompilationUnit(cu, ctx);
            }

            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation { MethodType: not null } call && call.Name.SimpleName == from
                    ? call.WithName(call.Name.WithSimpleName(to))
                    : visited;
            }
        }
    }
}
