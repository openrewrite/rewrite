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
using OpenRewrite.Test;
using OpenRewrite.Xml;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A test of a recipe on project files says which <see cref="MSBuildProject"/> each of them has, from a restore, from
/// the test or none at all.
/// </summary>
public class ProjectFileMarkersTests : RewriteTest
{
    private const string Project =
        """
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFramework>net8.0</TargetFramework>
          </PropertyGroup>
        </Project>
        """;

    private const string Found =
        """
        <!--~~>--><Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFramework>net8.0</TargetFramework>
          </PropertyGroup>
        </Project>
        """;

    private const string Props =
        """
        <Project>
          <PropertyGroup>
            <LangVersion>latest</LangVersion>
          </PropertyGroup>
        </Project>
        """;

    private static MSBuildProject Restored(string framework, params ResolvedPackage[] packages) =>
        new(global::OpenRewrite.Core.Tree.RandomId(), "Microsoft.NET.Sdk", targetFrameworks: [new TargetFramework(framework, resolvedPackages: packages)]);

    [Fact]
    public void AddMarkersReplacesTheMarkerTheRestoreGaveAProjectFile()
    {
        var marker = Restored("net10.0", new ResolvedPackage("System.Linq.Async", "6.0.1", depth: 2));
        RewriteRun(
            spec => spec.SetRecipe(new FindProjectsResolving("System.Linq.Async")),
            CsProj(Project, Found, "src/App/App.csproj")
                .AddMarkers(marker)
                .AfterRecipe<Document>(doc => Assert.Same(marker, Assert.Single(doc.Markers.MarkerList.OfType<MSBuildProject>())))
        );
    }

    [Fact]
    public void RemoveMarkersHasTheRecipeSeeAFileAsAnXmlParserParsesIt()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindBuildFiles()),
            CsProj(Project, Found, "src/App/App.csproj"),
            CsProj(Props, sourcePath: "Directory.Build.props").RemoveMarkers<MSBuildProject>()
                .AfterRecipe<Document>(doc => Assert.Empty(doc.Markers.MarkerList.OfType<MSBuildProject>()))
        );
    }

    [Fact]
    public void AnXmlSourceHasNoMarkerWhateverItsPath()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindBuildFiles()),
            CsProj(Project, Found, "src/App/App.csproj"),
            Xml(Project, sourcePath: "src/Other/Other.csproj"),
            Xml("<configuration />", sourcePath: "nuget.config;other.config")
                .AfterRecipe<Document>(doc => Assert.Empty(doc.Markers.MarkerList))
        );
    }

    [Fact]
    public void WithoutProjectRestoreAProjectFileHasTheMarkerOfItsSdkOnly()
    {
        RewriteRun(
            spec => spec.SetProjectRestore(false),
            CsProj(Project, sourcePath: "src/App/App.csproj").AfterRecipe<Document>(doc =>
            {
                var marker = Assert.Single(doc.Markers.MarkerList.OfType<MSBuildProject>());
                Assert.Equal("Microsoft.NET.Sdk", marker.Sdk);
                Assert.Empty(marker.TargetFrameworks);
            })
        );
    }

    [Fact]
    public void ARestoredProjectFileHasTheFrameworksItTargets()
    {
        RewriteRun(
            CsProj(Project, sourcePath: "src/App/App.csproj").AfterRecipe<Document>(doc =>
                Assert.Equal(["net8.0"], doc.Markers.FindFirst<MSBuildProject>()!.TargetFrameworks
                    .Select(f => f.TargetFrameworkMoniker)))
        );
    }

    /// <summary>Marks the build files, which are the XML files with an <see cref="MSBuildProject"/>.</summary>
    private class FindBuildFiles : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Find build files";
        public override string Description => "Marks each XML file with an MSBuildProject marker.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : XmlVisitor<ExecutionContext>
        {
            public override global::OpenRewrite.Xml.Xml VisitDocument(Document document, ExecutionContext ctx) =>
                document.Markers.FindFirst<MSBuildProject>() != null ? SearchResult.Found(document) : document;
        }
    }

    /// <summary>Marks the project files whose restore resolved a package as a transitive dependency.</summary>
    private class FindProjectsResolving(string package) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Find projects resolving a package";
        public override string Description => "Marks each project file whose restore resolved the package transitively.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(package);

        private class Visitor(string package) : XmlVisitor<ExecutionContext>
        {
            public override global::OpenRewrite.Xml.Xml VisitDocument(Document document, ExecutionContext ctx) =>
                document.Markers.FindFirst<MSBuildProject>()?.TargetFrameworks
                    .SelectMany(f => f.ResolvedPackages)
                    .Any(p => p.Name == package && p.Depth > 0) == true
                    ? SearchResult.Found(document)
                    : document;
        }
    }
}
