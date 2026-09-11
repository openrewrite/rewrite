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
using OpenRewrite.CSharp.NuGet;

namespace OpenRewrite.Tests;

public class NuGetRestoreGraphTests : IDisposable
{
    private readonly string _tempDir;

    public NuGetRestoreGraphTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "NuGetRestoreGraphTests_" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_tempDir);
    }

    public void Dispose()
    {
        try { Directory.Delete(_tempDir, true); }
        catch { /* best effort cleanup */ }
    }

    private void WriteFile(string relativePath, string content)
    {
        var fullPath = Path.Combine(_tempDir, relativePath);
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        File.WriteAllText(fullPath, content);
    }

    private string WriteSolution(params string[] projectNames)
    {
        foreach (var name in projectNames)
        {
            WriteFile($"{name}/{name}.csproj", """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);
        }

        var entries = string.Join("\n", projectNames.Select(n => $"  <Project Path=\"{n}/{n}.csproj\" />"));
        WriteFile("Test.slnx", $"<Solution>\n{entries}\n</Solution>");
        return Path.Combine(_tempDir, "Test.slnx");
    }

    [Fact]
    public void SolutionCostsOneEvaluationAndCoversEveryProject()
    {
        var solution = WriteSolution("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta");

        var before = NuGetResolver.GraphEvaluationsUnder(_tempDir);
        var dgSpec = NuGetResolver.CreateDependencyGraphSpec(solution);
        var evaluations = NuGetResolver.GraphEvaluationsUnder(_tempDir) - before;

        Assert.NotNull(dgSpec);
        var names = dgSpec.Projects
            .Select(p => Path.GetFileNameWithoutExtension(p.RestoreMetadata?.ProjectPath ?? p.FilePath))
            .OrderBy(n => n, StringComparer.OrdinalIgnoreCase)
            .ToArray();
        Assert.Equal(new[] { "Alpha", "Beta", "Delta", "Epsilon", "Gamma", "Zeta" }, names);
        Assert.Equal(6, dgSpec.Restore.Count);

        Assert.Equal(1, evaluations);
    }

    [Fact]
    public void RepeatedRequestCostsNoEvaluation()
    {
        var solution = WriteSolution("Shared", "Other");
        Assert.NotNull(NuGetResolver.CreateDependencyGraphSpec(solution));

        var before = NuGetResolver.GraphEvaluationsUnder(_tempDir);
        var second = NuGetResolver.CreateDependencyGraphSpec(solution);

        Assert.NotNull(second);
        Assert.Equal(2, second.Projects.Count);
        Assert.Equal(0, NuGetResolver.GraphEvaluationsUnder(_tempDir) - before);
    }

    [Fact]
    public void FailedEvaluationIsNotRetried()
    {
        WriteFile("Broken/Broken.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <Import Project="ThisFileDoesNotExist.props" />
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        var broken = Path.Combine(_tempDir, "Broken", "Broken.csproj");

        Assert.Null(NuGetResolver.CreateDependencyGraphSpec(broken));

        var before = NuGetResolver.GraphEvaluationsUnder(_tempDir);
        Assert.Null(NuGetResolver.CreateDependencyGraphSpec(broken));
        Assert.Equal(0, NuGetResolver.GraphEvaluationsUnder(_tempDir) - before);
    }

    [Fact]
    public void SharedRootFailureStopsTheFallbackEarly()
    {
        var names = Enumerable.Range(1, 10).Select(i => "Proj" + i).ToArray();
        foreach (var name in names)
        {
            WriteFile($"{name}/{name}.csproj", """
                <Project Sdk="Microsoft.NET.Sdk">
                  <Import Project="../Missing/Shared.props" />
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """);
        }
        var entries = string.Join("\n", names.Select(n => $"  <Project Path=\"{n}/{n}.csproj\" />"));
        WriteFile("Broken.slnx", $"<Solution>\n{entries}\n</Solution>");

        var before = NuGetResolver.GraphEvaluationsUnder(_tempDir);
        var dgSpec = NuGetResolver.CreateDependencyGraphSpec(Path.Combine(_tempDir, "Broken.slnx"));
        var evaluations = NuGetResolver.GraphEvaluationsUnder(_tempDir) - before;

        Assert.Null(dgSpec);
        Assert.True(evaluations <= 5,
            $"{evaluations} evaluations for 10 projects failing the same way; the fallback did " +
            "not stop after recognizing one shared root cause");
    }

    [Fact]
    public async Task NoOpRestoreStillYieldsTheSameLockFile()
    {
        WriteFile("Restored/Restored.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
              </ItemGroup>
            </Project>
            """);
        var project = Path.Combine(_tempDir, "Restored", "Restored.csproj");

        var dgSpec = NuGetResolver.CreateDependencyGraphSpec(project);
        Assert.NotNull(dgSpec);

        var first = await NuGetResolver.RestoreAsync(dgSpec, commit: true, CancellationToken.None);
        var firstLockFile = Assert.Contains(project, (IDictionary<string, global::NuGet.ProjectModel.LockFile>)first);

        var noOpsBefore = NuGetResolver.NoOpRestores;
        var second = await NuGetResolver.RestoreAsync(dgSpec, commit: true, CancellationToken.None);
        Assert.True(NuGetResolver.NoOpRestores > noOpsBefore,
            "the second restore of an unchanged project did not hit NuGet's no-op cache");
        var secondLockFile = Assert.Contains(project, (IDictionary<string, global::NuGet.ProjectModel.LockFile>)second);

        Assert.Contains(firstLockFile.Libraries, l => l.Name == "Newtonsoft.Json");
        Assert.Equal(
            firstLockFile.Libraries.Select(l => l.Name + "/" + l.Version).OrderBy(x => x).ToArray(),
            secondLockFile.Libraries.Select(l => l.Name + "/" + l.Version).OrderBy(x => x).ToArray());
    }
}
