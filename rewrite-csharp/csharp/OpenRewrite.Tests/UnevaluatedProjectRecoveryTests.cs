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

namespace OpenRewrite.Tests;

public class UnevaluatedProjectRecoveryTests : IDisposable
{
    private readonly string _tempDir;

    public UnevaluatedProjectRecoveryTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "UnevaluatedProject_" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_tempDir);
    }

    public void Dispose()
    {
        try { Directory.Delete(_tempDir, true); }
        catch { /* best effort cleanup */ }
    }

    private string WriteFile(string relativePath, string content)
    {
        var fullPath = Path.Combine(_tempDir, relativePath);
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        File.WriteAllText(fullPath, content);
        return fullPath;
    }

    /// <summary>
    /// A build-only task whose assembly path only exists after a real build. MSBuild fails
    /// evaluation with "The result "" of evaluating the value ... of the "AssemblyFile"
    /// attribute in element &lt;UsingTask&gt; is not valid."
    /// </summary>
    private void WriteBuildOnlyTargets(string relativePath) => WriteFile(relativePath, """
        <Project>
          <UsingTask TaskName="PdbGitTask" AssemblyFile="$(_PdbGitAssemblyFile)" />
        </Project>
        """);

    [Fact]
    public async Task ProjectFailingMSBuildEvaluationIsFlagged()
    {
        WriteBuildOnlyTargets("PdbGit.targets");
        WriteFile("Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <Import Project="PdbGit.targets" />
            </Project>
            """);
        WriteFile("A.cs", "class A { }\n");

        var parser = new SolutionParser();
        var projectPath = Path.Combine(_tempDir, "Broken.csproj");
        var solution = await parser.LoadAsync(projectPath);

        // MSBuild hands back the project stripped of every document.
        Assert.Empty(parser.ParseProject(solution, projectPath, _tempDir));

        var flagged = Assert.Single(parser.UnevaluatedProjects);
        Assert.Equal(projectPath, flagged.Key);
        Assert.Contains("UsingTask", flagged.Value);
    }

    [Fact]
    public async Task SourcesOfAnUnevaluatedProjectAreRecoveredFromDisk()
    {
        WriteBuildOnlyTargets("PdbGit.targets");
        WriteFile("Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <Import Project="PdbGit.targets" />
            </Project>
            """);
        WriteFile("A.cs", "class A { }\n");
        WriteFile("nested/B.cs", "class B { }\n");
        // Build output and tool directories are not repository sources.
        WriteFile("obj/Debug/Generated.cs", "class Generated { }\n");
        WriteFile("bin/Debug/Copied.cs", "class Copied { }\n");

        var parser = new SolutionParser();
        var projectPath = Path.Combine(_tempDir, "Broken.csproj");
        await parser.LoadAsync(projectPath);

        var recovered = parser.ParseProjectWithoutMSBuild(projectPath, _tempDir);

        Assert.Equal(new[] { "A.cs", "nested/B.cs" },
            recovered.Select(sf => sf.SourcePath).OrderBy(p => p, StringComparer.Ordinal));
        foreach (var sourceFile in recovered)
        {
            var cu = Assert.IsType<CompilationUnit>(sourceFile);
            // Recovered without a semantic model: syntax is intact, types are not attested.
            Assert.Equal(File.ReadAllText(Path.Combine(_tempDir, cu.SourcePath)),
                new CSharpPrinter<int>().Print(cu));
        }
    }

    [Fact]
    public async Task AlreadyParsedSourcesAreNotRecoveredTwice()
    {
        WriteBuildOnlyTargets("PdbGit.targets");
        WriteFile("Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <Import Project="PdbGit.targets" />
            </Project>
            """);
        WriteFile("A.cs", "class A { }\n");
        WriteFile("B.cs", "class B { }\n");

        var parser = new SolutionParser();
        var projectPath = Path.Combine(_tempDir, "Broken.csproj");
        await parser.LoadAsync(projectPath);

        var recovered = parser.ParseProjectWithoutMSBuild(projectPath, _tempDir,
            alreadyParsed: new HashSet<string> { "A.cs" });

        Assert.Equal("B.cs", Assert.Single(recovered).SourcePath);
    }

    [Fact]
    public async Task SubtreesOwnedByAnotherProjectAreLeftToThatProject()
    {
        WriteBuildOnlyTargets("PdbGit.targets");
        WriteFile("Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <Import Project="PdbGit.targets" />
            </Project>
            """);
        WriteFile("A.cs", "class A { }\n");
        WriteFile("Nested/Nested.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Nested/N.cs", "class N { }\n");

        var parser = new SolutionParser();
        var projectPath = Path.Combine(_tempDir, "Broken.csproj");
        await parser.LoadAsync(projectPath);

        var recovered = parser.ParseProjectWithoutMSBuild(projectPath, _tempDir);

        Assert.Equal("A.cs", Assert.Single(recovered).SourcePath);
    }

    [Fact]
    public async Task OnlyTheUnevaluatedProjectOfASolutionIsRecovered()
    {
        WriteBuildOnlyTargets("PdbGit.targets");
        WriteFile("Broken/Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <Import Project="../PdbGit.targets" />
            </Project>
            """);
        WriteFile("Broken/A.cs", "class A { }\n");
        WriteFile("Good/Good.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Good/C.cs", "class C { }\n");
        WriteFile("Test.sln", """
            Microsoft Visual Studio Solution File, Format Version 12.00
            Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "Broken", "Broken\Broken.csproj", "{00000000-0000-0000-0000-000000000001}"
            EndProject
            Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "Good", "Good\Good.csproj", "{00000000-0000-0000-0000-000000000002}"
            EndProject
            Global
            	GlobalSection(SolutionConfigurationPlatforms) = preSolution
            		Debug|Any CPU = Debug|Any CPU
            	EndGlobalSection
            EndGlobal
            """);

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Test.sln"));

        var flagged = Assert.Single(parser.UnevaluatedProjects);
        Assert.Equal(Path.Combine(_tempDir, "Broken", "Broken.csproj"), flagged.Key);

        var good = solution.Projects.Single(p => p.FilePath!.EndsWith("Good.csproj", StringComparison.Ordinal));
        Assert.Equal("Good/C.cs", Assert.Single(parser.ParseProject(solution, good.FilePath!, _tempDir)).SourcePath);

        var recovered = parser.ParseProjectWithoutMSBuild(flagged.Key, _tempDir);
        Assert.Equal("Broken/A.cs", Assert.Single(recovered).SourcePath);
    }
}
