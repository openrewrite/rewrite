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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.NuGet;

namespace OpenRewrite.Tests;

/// <summary>
/// .NET Framework projects have no reference assemblies on a machine without a targeting pack
/// (any Linux or macOS build agent), so <see cref="SolutionParser"/> provisions them. Without
/// them MSBuild resolves no references at all — not even mscorlib — and every source file is
/// parsed without type attestation. <c>FrameworkPathOverride</c> is cleared for the duration
/// because a developer machine may export it as a workaround for this very problem.
/// </summary>
public class NetFrameworkAttestationTests : IDisposable
{
    private readonly string _tempDir;
    private readonly string? _frameworkPathOverride;
    private readonly string? _referenceAssemblies;

    public NetFrameworkAttestationTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "NetFxTests_" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_tempDir);

        _frameworkPathOverride = Environment.GetEnvironmentVariable("FrameworkPathOverride");
        Environment.SetEnvironmentVariable("FrameworkPathOverride", null);
        _referenceAssemblies = Environment.GetEnvironmentVariable(
            SolutionRestore.ReferenceAssembliesEnvironmentVariable);
    }

    public void Dispose()
    {
        Environment.SetEnvironmentVariable("FrameworkPathOverride", _frameworkPathOverride);
        Environment.SetEnvironmentVariable(
            SolutionRestore.ReferenceAssembliesEnvironmentVariable, _referenceAssemblies);
        try { Directory.Delete(_tempDir, true); }
        catch { /* best effort cleanup */ }
    }

    [Fact]
    public async Task ClassicProjectResolvesFrameworkReferenceAssemblies()
    {
        WriteFile("Legacy.csproj", ClassicProject("v4.8", "Legacy.cs"));
        WriteFile("Legacy.cs", SourceUsingFrameworkTypes("Legacy"));

        await AssertFrameworkTypesResolve("Legacy.csproj");
    }

    [Fact]
    public async Task SdkStyleProjectResolvesFrameworkReferenceAssemblies()
    {
        WriteFile("Sdk48.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net48</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Doc.cs", SourceUsingFrameworkTypes("Sdk48"));

        await AssertFrameworkTypesResolve("Sdk48.csproj");
    }

    /// <summary>
    /// A target framework version other than the one whose package happens to be restored used
    /// to resolve nothing, because a single <c>TargetFrameworkRootPath</c> only covers the one
    /// version underneath it. The sibling project on a later version is what forces v4.7.2 to
    /// resolve through the fallback search paths, and the linked root stands in for the v4.7.2
    /// package so the test downloads nothing.
    /// </summary>
    [Fact]
    public async Task ProjectTargetingAnEarlierFrameworkVersionResolves()
    {
        WriteFile("Old.csproj", ClassicProject("v4.7.2", "Old.cs"));
        WriteFile("Old.cs", SourceUsingFrameworkTypes("Old"));
        WriteFile("newer/Newer.csproj", ClassicProject("v4.8", "Newer.cs"));
        WriteFile("newer/Newer.cs", SourceUsingFrameworkTypes("Newer"));

        Environment.SetEnvironmentVariable(
            SolutionRestore.ReferenceAssembliesEnvironmentVariable, await LinkedReferenceAssemblies("v4.7.2"));

        await AssertFrameworkTypesResolve("Old.csproj");
    }

    /// <summary>
    /// Machines that cannot reach the reference assembly packages can point at a directory
    /// holding them instead. The versions here have no package on nuget.org, so they can only
    /// be resolved from the configured directory.
    /// </summary>
    [Fact]
    public async Task PreProvisionedReferenceAssembliesAreUsed()
    {
        var root = await LinkedReferenceAssemblies("v4.3");
        Environment.SetEnvironmentVariable(SolutionRestore.ReferenceAssembliesEnvironmentVariable, root);

        var assets = await SolutionRestore.RestoreNetFrameworkBuildAssetsAsync(["v4.3"], CancellationToken.None);

        Assert.Equal(root, assets.ReferenceAssemblyRoots[0]);
        Assert.Empty(assets.MissingVersions);
    }

    [Fact]
    public async Task UnobtainableReferenceAssembliesAreReportedAsMissing()
    {
        var assets = await SolutionRestore.RestoreNetFrameworkBuildAssetsAsync(["v3.9"], CancellationToken.None);

        Assert.Equal(["v3.9"], assets.MissingVersions);
        Assert.DoesNotContain(assets.ReferenceAssemblyRoots,
            root => Directory.Exists(Path.Combine(root, ".NETFramework", "v3.9")));
    }

    /// <summary>
    /// The reference assembly search paths are handed to restore-graph generation as one
    /// <c>;</c>-separated property, so a list-valued global property has to reach MSBuild whole:
    /// a separator taken as anything but a list separator loses the graph, and with it dependency
    /// attestation.
    /// </summary>
    [Fact]
    public void ListValuedGlobalPropertiesReachTheRestoreGraph()
    {
        WriteFile("Legacy.csproj", ClassicProject("v4.8", "Legacy.cs"));
        WriteFile("Legacy.cs", SourceUsingFrameworkTypes("Legacy"));

        var properties = new Dictionary<string, string>
        {
            ["TargetFrameworkFallbackSearchPaths"] = "/first/root;/second/root",
        };

        Assert.NotNull(NuGetResolver.CreateDependencyGraphSpec(
            Path.Combine(_tempDir, "Legacy.csproj"), properties));
    }

    [Theory]
    [InlineData("net48", "v4.8")]
    [InlineData("net472", "v4.7.2")]
    [InlineData("net35", "v3.5")]
    [InlineData("net403", "v4.0.3")]
    public void SdkStyleFrameworkMonikersAreDetected(string moniker, string expectedVersion)
    {
        WriteFile("Sdk.csproj", $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>{moniker}</TargetFramework>
              </PropertyGroup>
            </Project>
            """);

        Assert.Equal(new[] { expectedVersion },
            SolutionParser.DetectNetFrameworkVersions(Path.Combine(_tempDir, "Sdk.csproj")));
    }

    [Theory]
    [InlineData("net10.0")]
    [InlineData("netstandard2.0")]
    [InlineData("netcoreapp3.1")]
    public void ModernTargetFrameworksAreNotDetected(string moniker)
    {
        WriteFile("Sdk.csproj", $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>{moniker}</TargetFramework>
              </PropertyGroup>
            </Project>
            """);

        Assert.Empty(SolutionParser.DetectNetFrameworkVersions(Path.Combine(_tempDir, "Sdk.csproj")));
    }

    [Fact]
    public void EveryTargetedFrameworkVersionIsDetectedHighestFirst()
    {
        WriteFile("src/Legacy/Legacy.csproj", ClassicProject("v4.6.1", "Legacy.cs"));
        WriteFile("src/Multi/Multi.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net48;net10.0</TargetFrameworks>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("src/Desktop/Desktop.vbproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net472</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Everything.sln", "");

        Assert.Equal(new[] { "v4.8", "v4.7.2", "v4.6.1" },
            SolutionParser.DetectNetFrameworkVersions(Path.Combine(_tempDir, "Everything.sln")));
    }

    private async Task AssertFrameworkTypesResolve(string projectFileName)
    {
        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, projectFileName));

        var project = Assert.Single(solution.Projects);
        var compilation = await project.GetCompilationAsync();
        Assert.NotNull(compilation);

        Assert.NotNull(compilation.GetTypeByMetadataName("System.String"));
        Assert.NotNull(compilation.GetTypeByMetadataName("System.Xml.XmlDocument"));

        var unresolved = compilation.GetDiagnostics()
            .Where(d => d.Severity == Microsoft.CodeAnalysis.DiagnosticSeverity.Error &&
                        (d.Id == "CS0246" || d.Id == "CS0234" || d.Id == "CS0518" || d.Id == "CS0012"))
            .Select(d => d.ToString())
            .ToList();
        Assert.Empty(unresolved);
    }

    /// <summary>
    /// A reference assembly root offering <paramref name="version"/>, backed by the restored
    /// net48 reference assemblies — symlinked, or copied where a platform needs elevation to
    /// link a directory.
    /// </summary>
    private async Task<string> LinkedReferenceAssemblies(string version)
    {
        var assets = await SolutionRestore.RestoreNetFrameworkBuildAssetsAsync(["v4.8"], CancellationToken.None);
        var net48 = assets.ReferenceAssemblyRoots
            .Select(root => Path.Combine(root, ".NETFramework", "v4.8"))
            .FirstOrDefault(Directory.Exists);
        Assert.NotNull(net48);

        var linked = Path.Combine(_tempDir, "refs");
        var versionDir = Path.Combine(linked, ".NETFramework", version);
        Directory.CreateDirectory(Path.Combine(linked, ".NETFramework"));
        try
        {
            Directory.CreateSymbolicLink(versionDir, net48);
        }
        catch (Exception e) when (e is IOException or UnauthorizedAccessException)
        {
            Directory.CreateDirectory(versionDir);
            foreach (var assembly in new[] { "mscorlib.dll", "System.dll", "System.Core.dll", "System.Xml.dll" })
            {
                var source = Path.Combine(net48, assembly);
                if (File.Exists(source))
                    File.Copy(source, Path.Combine(versionDir, assembly));
            }
        }
        return linked;
    }

    private static string ClassicProject(string targetFrameworkVersion, string sourceFile) => $$"""
        <?xml version="1.0" encoding="utf-8"?>
        <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
          <Import Project="$(MSBuildExtensionsPath)\$(MSBuildToolsVersion)\Microsoft.Common.props" Condition="Exists('$(MSBuildExtensionsPath)\$(MSBuildToolsVersion)\Microsoft.Common.props')" />
          <PropertyGroup>
            <Configuration Condition=" '$(Configuration)' == '' ">Debug</Configuration>
            <Platform Condition=" '$(Platform)' == '' ">AnyCPU</Platform>
            <ProjectGuid>{8F1A0B2C-3D4E-4F5A-9B8C-7D6E5F4A3B2C}</ProjectGuid>
            <OutputType>Library</OutputType>
            <TargetFrameworkVersion>{{targetFrameworkVersion}}</TargetFrameworkVersion>
          </PropertyGroup>
          <ItemGroup>
            <Reference Include="System" />
            <Reference Include="System.Xml" />
          </ItemGroup>
          <ItemGroup>
            <Compile Include="{{sourceFile}}" />
          </ItemGroup>
          <Import Project="$(MSBuildToolsPath)\Microsoft.CSharp.targets" />
        </Project>
        """;

    private static string SourceUsingFrameworkTypes(string ns) => $$"""
        using System.Xml;

        namespace {{ns}}
        {
            public class Doc
            {
                public string Load(XmlDocument doc) => doc.OuterXml;
            }
        }
        """;

    private void WriteFile(string relativePath, string content)
    {
        var fullPath = Path.Combine(_tempDir, relativePath);
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        File.WriteAllText(fullPath, content);
    }
}
