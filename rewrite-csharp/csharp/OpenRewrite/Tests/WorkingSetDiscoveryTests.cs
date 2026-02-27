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
