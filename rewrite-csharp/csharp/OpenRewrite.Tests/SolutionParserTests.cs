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
using LibGit2Sharp;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.NuGet;
using Serilog;
using Serilog.Core;
using Serilog.Events;

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
        Repository.Init(_tempDir);
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

    [Fact]
    public async Task WindowsTargetedProjectResolvesReferencesOnAnyOperatingSystem()
    {
        WriteFile("Win.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0-windows</TargetFramework>
                <UseWindowsForms>true</UseWindowsForms>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget { }\n");

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(Path.Combine(_tempDir, "Win.csproj"));

        // Without EnableWindowsTargeting the SDK fails evaluation with NETSDK1100 on
        // Linux/macOS, and the project loads with no references to attest against.
        var project = Assert.Single(solution.Projects);
        Assert.NotEmpty(project.MetadataReferences);

        var results = parser.ParseProject(solution, Path.Combine(_tempDir, "Win.csproj"), _tempDir);
        Assert.Single(results);
    }

    [Fact]
    public void AndroidDesignTimeBuildsUseTheManagedResourceGenerator()
    {
        var properties = new Dictionary<string, string>();

        NuGetResolver.ApplyAndroidDesignTimeDefault(properties);

        if (OperatingSystem.IsWindows())
            Assert.DoesNotContain("AndroidUseManagedDesignTimeResourceGenerator", properties.Keys);
        else
            Assert.Equal("true", properties["AndroidUseManagedDesignTimeResourceGenerator"]);
    }

    [Fact]
    public void OutOfSupportWorkloadsDoNotFailEvaluation()
    {
        var properties = new Dictionary<string, string>();

        NuGetResolver.ApplyOutOfSupportWorkloadsDefault(properties);

        Assert.Equal("false", properties["CheckEolWorkloads"]);
    }

    [Fact]
    public async Task ProjectsThatResolveNoReferencesAreWarnedAboutOnce()
    {
        WriteFile("Unsupported.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <DisableImplicitFrameworkReferences>true</DisableImplicitFrameworkReferences>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget { }\n");

        var warnings = new List<string>();
        var previousLogger = Log.Logger;
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Warning()
            .WriteTo.Sink(new CollectingSink(warnings))
            .CreateLogger();
        try
        {
            await new SolutionParser().LoadAsync(Path.Combine(_tempDir, "Unsupported.csproj"));
        }
        finally
        {
            Log.Logger = previousLogger;
        }

        var warning = Assert.Single(warnings, w => w.Contains("Unsupported.csproj") &&
                                                   w.Contains("resolved no reference metadata"));
        Assert.Contains("1 of 1", warning);
    }

    [Fact]
    public void WindowsTargetedProjectProducesRestoreGraph()
    {
        var csproj = WriteFile("MultiTarget.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net48;net10.0-windows</TargetFrameworks>
                <UseWPF>true</UseWPF>
              </PropertyGroup>
            </Project>
            """);

        var dgSpec = NuGetResolver.CreateDependencyGraphSpec(csproj);

        Assert.NotNull(dgSpec);
        Assert.NotEmpty(dgSpec.Projects);
    }

    [Fact]
    public async Task OnlyTargetFrameworksTheSdkSupportsAreParsed()
    {
        var csproj = WriteFile("Multi.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net99.0;net10.0</TargetFrameworks>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
              </ItemGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget : Newtonsoft.Json.JsonException { }\n");

        var parser = new SolutionParser();
        Microsoft.CodeAnalysis.Solution? solution = null;
        var errors = await CaptureErrors(async () => solution = await parser.LoadAsync(csproj));

        Assert.Empty(parser.ProjectsNotParsed);
        var error = Assert.Single(errors, e => e.Contains("Multi.csproj"));
        Assert.Contains("is parsed for net10.0 only", error);
        Assert.Contains("net99.0", error);
        Assert.Contains("newer .NET SDK", error);

        var results = parser.ParseProject(solution!, csproj, _tempDir);
        AssertWidgetExtends("Newtonsoft.Json.JsonException", Assert.IsType<CompilationUnit>(Assert.Single(results)));
    }

    [Theory]
    [InlineData("TargetFramework", "net99.0")]
    [InlineData("TargetFrameworks", "net99.0")]
    [InlineData("TargetFramework", "netstandard9.0")]
    [InlineData("TargetFramework", "net99.0-ios")]
    [InlineData("TargetFrameworks", "net99.0-android;net99.0-ios")]
    public async Task ProjectNeedingANewerSdkIsNotParsed(string property, string targetFramework)
    {
        var csproj = WriteFile("Future.csproj", $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <{property}>{targetFramework}</{property}>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget : System.Exception { }\n");

        var parser = new SolutionParser();
        var errors = await CaptureErrors(() => parser.LoadAsync(csproj));

        Assert.Contains(parser.ProjectsNotParsed, p => p.EndsWith("Future.csproj"));
        var error = Assert.Single(errors, e => e.Contains("Future.csproj"));
        Assert.Contains("is not parsed", error);
        Assert.Contains(targetFramework.Replace(";", ", "), error);
        Assert.Contains("newer .NET SDK", error);
        Assert.DoesNotContain("workload", error);
    }

    [Theory]
    [InlineData("TargetFramework")]
    [InlineData("TargetFrameworks")]
    public async Task ProjectWithoutItsWorkloadIsNotParsed(string property)
    {
        var csproj = WriteFile("Handset.csproj", $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <{property}>net10.0-ios</{property}>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget : System.Exception { }\n");

        var parser = new SolutionParser();
        var errors = await CaptureErrors(() => parser.LoadAsync(csproj));

        Assert.Contains(parser.ProjectsNotParsed, p => p.EndsWith("Handset.csproj"));
        var error = Assert.Single(errors, e => e.Contains("Handset.csproj"));
        Assert.Contains("is not parsed", error);
        Assert.Contains("net10.0-ios", error);
    }

    [Fact]
    public async Task OnlyTargetFrameworksWithTheirWorkloadAreParsed()
    {
        var csproj = WriteFile("Tablet.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net10.0-ios;net9.0</TargetFrameworks>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
              </ItemGroup>
            </Project>
            """);
        WriteFile("Widget.cs", "class Widget : Newtonsoft.Json.JsonException { }\n");

        var parser = new SolutionParser();
        Microsoft.CodeAnalysis.Solution? solution = null;
        var errors = await CaptureErrors(async () => solution = await parser.LoadAsync(csproj));

        Assert.Empty(parser.ProjectsNotParsed);
        Assert.Contains(parser.RestoredLockFiles.Keys, k => k.EndsWith("Tablet.csproj"));
        var error = Assert.Single(errors, e => e.Contains("Tablet.csproj"));
        Assert.Contains("is parsed for net9.0 only", error);
        Assert.Contains("net10.0-ios", error);

        var results = parser.ParseProject(solution!, csproj, _tempDir);
        AssertWidgetExtends("Newtonsoft.Json.JsonException", Assert.IsType<CompilationUnit>(Assert.Single(results)));
    }

    [Fact]
    public async Task ProjectWithoutItsWorkloadDoesNotStopTheRestOfTheSolution()
    {
        WriteFile("Phone/Phone.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0-ios</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Phone/PhoneWidget.cs", "class PhoneWidget { }\n");
        var library = WriteFile("Library/Library.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        WriteFile("Library/Widget.cs", "class Widget : System.Exception { }\n");
        var slnx = WriteFile("Devices.slnx", """
            <Solution>
              <Project Path="Phone/Phone.csproj" />
              <Project Path="Library/Library.csproj" />
            </Solution>
            """);

        var parser = new SolutionParser();
        var solution = await parser.LoadAsync(slnx);

        Assert.Contains(parser.ProjectsNotParsed, p => p.EndsWith("Phone.csproj"));
        var results = parser.ParseProject(solution, library, _tempDir);
        AssertWidgetExtendsException(Assert.IsType<CompilationUnit>(Assert.Single(results)));
    }

    [Fact]
    public void RestoreGraphLeavesOutTargetFrameworksWithoutTheirWorkload()
    {
        var csproj = WriteFile("Watch.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net10.0;net10.0-ios</TargetFrameworks>
              </PropertyGroup>
            </Project>
            """);

        var dgSpec = NuGetResolver.CreateDependencyGraphSpec(csproj);

        Assert.NotNull(dgSpec);
        var framework = Assert.Single(dgSpec.Projects.Single().TargetFrameworks);
        Assert.Equal("net10.0", framework.TargetAlias);
    }

    private static async Task<List<string>> CaptureErrors(Func<Task> action)
    {
        var errors = new List<string>();
        var previousLogger = Log.Logger;
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Error()
            .WriteTo.Sink(new CollectingSink(errors))
            .CreateLogger();
        try
        {
            await action();
        }
        finally
        {
            Log.Logger = previousLogger;
        }
        return errors;
    }

    private static void AssertWidgetExtendsException(CompilationUnit cu) =>
        AssertWidgetExtends("System.Exception", cu);

    private static void AssertWidgetExtends(string supertype, CompilationUnit cu)
    {
        var widget = Assert.IsType<OpenRewrite.Java.ClassDeclaration>(cu.Members[0].Element);
        var type = Assert.IsType<OpenRewrite.Java.JavaType.Class>(widget.Type);
        Assert.Equal(supertype, Assert.IsType<OpenRewrite.Java.JavaType.Class>(type.Supertype).FullyQualifiedName);
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

    private sealed class CollectingSink(List<string> messages) : ILogEventSink
    {
        public void Emit(LogEvent logEvent)
        {
            lock (messages)
                messages.Add(logEvent.RenderMessage());
        }
    }
}
