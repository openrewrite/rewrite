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

[Collection(ProcessEnvironmentCollection.Name)]
public class WorkingDirectoryTests : IDisposable
{
    private readonly string _tempDir = Path.Combine(Path.GetTempPath(),
        "WorkingDirectoryTests_" + Guid.NewGuid().ToString("N")[..8]);

    private readonly string _originalDirectory = Directory.GetCurrentDirectory();

    public void Dispose()
    {
        Directory.SetCurrentDirectory(_originalDirectory);
        try { Directory.Delete(_tempDir, true); }
        catch (IOException) { }
    }

    private string WriteProject(string name)
    {
        var path = Path.Combine(_tempDir, name, name + ".csproj");
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        File.WriteAllText(path, """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """);
        return path;
    }

    [Fact]
    public void RestoreGraphSurvivesADeletedWorkingDirectory()
    {
        var before = WriteProject("Before");
        var after = WriteProject("After");
        var workingDirectory = Path.Combine(_tempDir, "cwd");
        Directory.CreateDirectory(workingDirectory);

        Directory.SetCurrentDirectory(workingDirectory);
        Assert.NotNull(NuGetResolver.CreateDependencyGraphSpec(before));
        Directory.Delete(workingDirectory);

        Assert.NotNull(NuGetResolver.CreateDependencyGraphSpec(after));
    }

    [Fact]
    public async Task InProcessBuildsNeverLendTheirProjectDirectoryOutsideTheGate()
    {
        var projects = Enumerable.Range(0, 3).Select(i => WriteProject("Borrowed" + i)).ToList();
        var observed = new HashSet<string>();
        using var done = new CancellationTokenSource();

        var observer = Task.Run(() =>
        {
            while (!done.IsCancellationRequested)
            {
                using (MSBuildEnvironment.EnterOperatingEnvironment())
                    observed.Add(Directory.GetCurrentDirectory());
            }
        });

        foreach (var project in projects)
            Assert.NotNull(NuGetResolver.CreateDependencyGraphSpec(project));
        await done.CancelAsync();
        await observer;

        Assert.NotEmpty(observed);
        Assert.DoesNotContain(observed, directory => directory.Contains(Path.GetFileName(_tempDir)));
    }
}
