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
        var cu = results[0];

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
