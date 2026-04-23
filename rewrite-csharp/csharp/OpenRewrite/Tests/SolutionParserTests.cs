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
using System.Diagnostics;
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests;

public class SolutionParserTests : IDisposable
{
    private readonly string _tempDir;

    public SolutionParserTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "SolutionParserTests_" + Guid.NewGuid().ToString("N")[..8]);
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

    private string WriteFileWithBom(string relativePath, string content)
    {
        var fullPath = Path.Combine(_tempDir, relativePath);
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        File.WriteAllText(fullPath, content, new System.Text.UTF8Encoding(encoderShouldEmitUTF8Identifier: true));
        return fullPath;
    }

    [Fact]
    public async Task ParseSimpleClass()
    {
        WriteFile("Test.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("HelloWorld.cs", "namespace Test { public class HelloWorld { } }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Test.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Test.csproj"), _tempDir);

        Assert.Single(results);
        var cu = Assert.IsType<CompilationUnit>(results[0]);

        var printed = new CSharpPrinter<int>().Print(cu);
        Assert.Equal("namespace Test { public class HelloWorld { } }\n", printed);
    }

    [Fact]
    public async Task ParseMultipleFiles()
    {
        WriteFile("Multi.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("A.cs", "class A { }\n");
        WriteFile("B.cs", "class B { }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Multi.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Multi.csproj"), _tempDir);

        Assert.Equal(2, results.Count);
    }

    [Fact]
    public async Task ParseWithPreprocessorDirectives()
    {
        WriteFile("Directives.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Conditional.cs", """
            class C
            {
            #if DEBUG
                void DebugMethod() { }
            #else
                void ReleaseMethod() { }
            #endif
            }
            """);

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Directives.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Directives.csproj"), _tempDir);

        Assert.Single(results);
    }

    [Fact]
    public async Task ParseFileWithBomPreservesCharsetBomMarked()
    {
        WriteFile("Bom.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFileWithBom("WithBom.cs", "namespace Test { class WithBom { } }\n");
        WriteFile("NoBom.cs", "namespace Test { class NoBom { } }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Bom.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Bom.csproj"), _tempDir);

        Assert.Equal(2, results.Count);

        var withBom = results.OfType<CompilationUnit>()
            .Single(cu => cu.SourcePath.Contains("WithBom"));
        var noBom = results.OfType<CompilationUnit>()
            .Single(cu => cu.SourcePath.Contains("NoBom"));

        Assert.True(withBom.CharsetBomMarked,
            "File written with UTF-8 BOM should have CharsetBomMarked=true");
        Assert.False(noBom.CharsetBomMarked,
            "File written without BOM should have CharsetBomMarked=false");
    }

    [Fact]
    public async Task GitIgnoredFilesAreExcluded()
    {
        // Initialize a git repo with a .gitignore that excludes **/[Pp]ackages/*
        RunGit("init");
        WriteFile(".gitignore", "**/[Pp]ackages/*\n");

        // Create a project that includes a source file under .nuget/packages/
        // (simulating NuGet source packages like xunit.assert.source)
        WriteFile("Test.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Compile Include=".nuget/packages/SomePackage/Source.cs" />
              </ItemGroup>
            </Project>
            """);
        WriteFile("App.cs", "class App { }\n");
        WriteFile(".nuget/packages/SomePackage/Source.cs", "class Source { }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Test.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Test.csproj"), _tempDir);

        // Only App.cs should be parsed; the file under packages/ should be excluded
        Assert.Single(results);
        var cu = Assert.IsType<CompilationUnit>(results[0]);
        Assert.Contains("App.cs", cu.SourcePath);
    }

    [Fact]
    public async Task NonGitRepoDoesNotFilter()
    {
        // No git init — should parse all files including those that would match gitignore patterns
        WriteFile("Test.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Compile Include=".nuget/packages/SomePackage/Source.cs" />
              </ItemGroup>
            </Project>
            """);
        WriteFile("App.cs", "class App { }\n");
        WriteFile(".nuget/packages/SomePackage/Source.cs", "class Source { }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Test.csproj"));
        var results = parser.ParseProject(solution,
            Path.Combine(_tempDir, "Test.csproj"), _tempDir);

        // Both files should be parsed since there's no git repo
        Assert.Equal(2, results.Count);
    }

    private void RunGit(string args)
    {
        var psi = new ProcessStartInfo("git", args)
        {
            WorkingDirectory = _tempDir,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        using var process = Process.Start(psi)!;
        process.WaitForExit(10_000);
        if (process.ExitCode != 0)
            throw new InvalidOperationException(
                $"git {args} failed: {process.StandardError.ReadToEnd()}");
    }

    [Fact]
    public async Task ParseSolutionFile()
    {
        // Create a project directory
        WriteFile("src/App/App.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("src/App/Program.cs", "class Program { static void Main() { } }\n");

        // Create a solution file
        var slnContent = $$$"""
            Microsoft Visual Studio Solution File, Format Version 12.00
            Project("{{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}}") = "App", "src\App\App.csproj", "{{00000000-0000-0000-0000-000000000001}}"
            EndProject
            Global
            	GlobalSection(SolutionConfigurationPlatforms) = preSolution
            		Debug|Any CPU = Debug|Any CPU
            		Release|Any CPU = Release|Any CPU
            	EndGlobalSection
            EndGlobal
            """;
        WriteFile("Test.sln", slnContent);

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Test.sln"));

        // Should load the project from the solution
        Assert.NotEmpty(solution.Projects);

        var project = solution.Projects.First();
        var results = parser.ParseProject(solution, project.FilePath!, _tempDir);

        Assert.Single(results);
    }
}
