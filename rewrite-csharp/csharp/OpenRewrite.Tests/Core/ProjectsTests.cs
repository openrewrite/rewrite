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
/// The sources of a solution belong to projects, each compiled on its own against the projects it references, and a
/// recipe tells them apart by their <see cref="DotNetProject"/> markers.
/// </summary>
public class ProjectsTests : RewriteTest
{
    private const string Library =
        """
        namespace Lib
        {
            public static class Values
            {
                public static int Foo() => 1;
                public static int Bar() => 2;
            }
        }
        """;

    private const string App = "class App { int M() => Lib.Values.Foo(); }";

    [Fact]
    public void AProjectCompilesAgainstTheProjectsItReferences()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")).ExpectInputCompiles().ExpectOutputCompiles(),
            CSharp(App, App.Replace("Foo()", "Bar()")).InProject("App.csproj", "Lib.csproj")
                .AfterRecipe((before, after) =>
                {
                    var foo = FindFirst<MethodInvocation>(before)!;
                    Assert.Equal("Lib.Values", TypeUtils.GetFullyQualifiedName(foo.MethodType!.DeclaringType));
                    Assert.Equal("App.csproj", after.Markers.FindFirst<DotNetProject>()!.ProjectName);
                }),
            CSharp(Library).InProject("Lib.csproj")
        );
    }

    [Fact]
    public void AProjectDoesNotSeeTheTypesOfAProjectItDoesNotReference()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.ExpectInputCompiles(),
            CSharp(App).InProject("App.csproj"),
            CSharp(Library).InProject("Lib.csproj")
        ));

        Assert.Contains("error CS0103: The name 'Lib' does not exist in the current context", failure.Message);
    }

    [Fact]
    public void ASourceInNoProjectDoesNotSeeTheTypesOfAProject()
    {
        Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.ExpectInputCompiles(),
            CSharp(App),
            CSharp(Library).InProject("Lib.csproj")
        ));
    }

    [Fact]
    public void TheSourcesOfAProjectShareItsMarkerAndFrameworks()
    {
        var projects = new List<DotNetProject>();
        void Project(CompilationUnit cu) => projects.Add(cu.Markers.FindFirst<DotNetProject>()!);

        RewriteRun(
            CSharp(Library).InProject("Test.csproj").TargetFrameworks("net6.0").AfterRecipe(Project),
            CSharp("class Other { }").InProject("Test.csproj").AfterRecipe(Project)
        );

        Assert.Same(projects[0], projects[1]);
        Assert.Equal("Test.csproj", projects[0].ProjectName);
        Assert.Equal(["net6.0"], projects[0].TargetFrameworks);
    }

    [Fact]
    public void AProjectTargetsTheSameFrameworksForAllItsSources()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp(Library).InProject("Test.csproj").TargetFrameworks("net6.0"),
            CSharp("class Other { }").InProject("Test.csproj").TargetFrameworks("net48")
        ));

        Assert.Equal("The sources in Test.csproj target net6.0 and net48, and a project targets the same frameworks " +
                     "for all its sources.", failure.Message);
    }

    [Fact]
    public void AReferenceToAProjectNoSourceIsInIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp(App).InProject("App.csproj", "Lib.csproj")
        ));

        Assert.Equal("App.csproj references Lib.csproj, which no source of the test is in.", failure.Message);
    }

    [Fact]
    public void ProjectsThatReferenceEachOtherAreRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => RewriteRun(
            CSharp(App).InProject("App.csproj", "Lib.csproj"),
            CSharp(Library).InProject("Lib.csproj", "App.csproj")
        ));

        Assert.EndsWith("references itself through its project references.", failure.Message);
    }

    private class RenameCalls(string from, string to) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls";
        public override string Description => "Renames the calls to one method.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to);

        private class Visitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation call && call.Name.SimpleName == from
                    ? call.WithName(call.Name.WithSimpleName(to))
                    : visited;
            }
        }
    }
}
