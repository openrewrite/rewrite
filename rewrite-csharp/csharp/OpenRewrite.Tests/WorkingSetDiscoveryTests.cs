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
namespace OpenRewrite.Tests;

/// <summary>
/// Discovery test to enumerate all .NET solutions/projects in the working set.
/// Run this test to see all project files that will be used as individual parse/print test cases.
/// </summary>
public class WorkingSetDiscoveryTests
{
    [Fact]
    public void DiscoverAllProjectsInWorkingSet()
    {
        if (!Directory.Exists(WorkingSetDiscovery.WorkingSetRoot))
        {
            Console.WriteLine($"SKIP: Working set root not found: {WorkingSetDiscovery.WorkingSetRoot}");
            return;
        }

        var projectDirs = WorkingSetDiscovery.FindDotNetProjectDirs(WorkingSetDiscovery.WorkingSetRoot);
        Console.WriteLine($"Found {projectDirs.Count} .NET project directories:\n");

        foreach (var dir in projectDirs)
        {
            var relativePath = Path.GetRelativePath(WorkingSetDiscovery.WorkingSetRoot, dir).Replace('\\', '/');
            var projectFiles = WorkingSetDiscovery.FindProjectFiles(dir);
            var fileNames = projectFiles.Select(Path.GetFileName);
            Console.WriteLine($"  {relativePath}");
            foreach (var file in fileNames)
            {
                Console.WriteLine($"    -> {file}");
            }
        }
    }

    [Fact]
    public void DiscoverAllProjectFilesForTestCases()
    {
        if (!Directory.Exists(WorkingSetDiscovery.WorkingSetRoot))
        {
            Console.WriteLine($"SKIP: Working set root not found: {WorkingSetDiscovery.WorkingSetRoot}");
            return;
        }

        var allProjects = WorkingSetDiscovery.DiscoverAll();
        Console.WriteLine($"Found {allProjects.Count} project files to parse:\n");

        var byCategory = allProjects.GroupBy(p =>
        {
            var parts = p.DisplayName.Split('/');
            return parts.Length >= 2 ? parts[0] : "(root)";
        });

        foreach (var group in byCategory.OrderBy(g => g.Key))
        {
            Console.WriteLine($"[{group.Key}]");
            foreach (var project in group)
            {
                var ext = Path.GetExtension(project.ProjectFile).ToLowerInvariant();
                var type = ext switch
                {
                    ".sln" => "SLN",
                    ".slnx" => "SLNX",
                    ".csproj" => "CSPROJ",
                    _ => ext
                };
                Console.WriteLine($"  [{type}] {project.DisplayName}");
            }
            Console.WriteLine();
        }
    }
}
