namespace OpenRewrite.Tests;

/// <summary>
/// Discovers .NET solutions and projects in the working set directory,
/// mirroring the logic from DotNetBuildStep.java in moderne-cli.
///
/// Algorithm:
/// 1. Walk the directory tree from root
/// 2. Skip excluded dirs (bin, obj, .vs, packages, TestResults)
/// 3. When a directory contains .sln/.slnx/.csproj, mark it as a project dir and prune subtree
/// 4. For each project dir, prefer .sln/.slnx files; fall back to .csproj
/// </summary>
public static class WorkingSetDiscovery
{
    public const string WorkingSetRoot = @"C:\Projects\moderneinc\moderne-cli\working-set-csharp";

    private static readonly HashSet<string> ExcludedDirNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "bin", "obj", ".vs", "packages", "TestResults"
    };

    /// <summary>
    /// Find all .NET project directories under the given root, mirroring DotNetBuildStep.findDotNetProjectDirs.
    /// Each returned directory contains at least one .sln, .slnx, or .csproj file.
    /// Subtrees are pruned once a project directory is found.
    /// </summary>
    public static List<string> FindDotNetProjectDirs(string rootDir)
    {
        var projectDirs = new List<string>();
        WalkDirectory(rootDir, rootDir, projectDirs);
        return projectDirs;
    }

    private static void WalkDirectory(string dir, string rootDir, List<string> projectDirs)
    {
        // Check exclusion (skip for root itself)
        if (dir != rootDir)
        {
            var dirName = Path.GetFileName(dir);
            if (ExcludedDirNames.Contains(dirName))
                return;
        }

        // If this directory is a .NET project, add it and prune subtree
        if (IsDotNetProject(dir))
        {
            projectDirs.Add(dir);
            return; // prune subtree
        }

        // Recurse into subdirectories
        try
        {
            foreach (var subDir in Directory.GetDirectories(dir).OrderBy(d => d))
            {
                WalkDirectory(subDir, rootDir, projectDirs);
            }
        }
        catch (UnauthorizedAccessException)
        {
            // skip inaccessible directories
        }
    }

    /// <summary>
    /// Check if a directory contains any .sln, .slnx, or .csproj file (immediate children only).
    /// </summary>
    private static bool IsDotNetProject(string dir)
    {
        try
        {
            return Directory.EnumerateFiles(dir)
                .Any(f =>
                {
                    var name = Path.GetFileName(f);
                    return name.EndsWith(".sln", StringComparison.OrdinalIgnoreCase) ||
                           name.EndsWith(".slnx", StringComparison.OrdinalIgnoreCase) ||
                           name.EndsWith(".csproj", StringComparison.OrdinalIgnoreCase);
                });
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// For a given project directory, find the project files to parse.
    /// Prefers .sln/.slnx files; falls back to .csproj if no solutions found.
    /// Mirrors DotNetBuildStep build processing logic.
    /// </summary>
    public static List<string> FindProjectFiles(string projectDir)
    {
        var slnFiles = FindFiles(projectDir, ".sln", ".slnx");
        if (slnFiles.Count > 0)
            return slnFiles;

        return FindFiles(projectDir, ".csproj");
    }

    private static List<string> FindFiles(string dir, params string[] extensions)
    {
        try
        {
            return Directory.EnumerateFiles(dir)
                .Where(f => extensions.Any(ext =>
                    f.EndsWith(ext, StringComparison.OrdinalIgnoreCase)))
                .OrderBy(f => f)
                .ToList();
        }
        catch
        {
            return [];
        }
    }

    /// <summary>
    /// Returns all (projectDir, projectFile) pairs for the working set.
    /// Each entry represents a solution or project file that should be parsed.
    /// The returned display name is relative to the working set root for readability.
    /// </summary>
    public static List<(string DisplayName, string ProjectDir, string ProjectFile)> DiscoverAll(string? rootDir = null)
    {
        rootDir ??= WorkingSetRoot;
        var results = new List<(string DisplayName, string ProjectDir, string ProjectFile)>();

        foreach (var projectDir in FindDotNetProjectDirs(rootDir))
        {
            foreach (var projectFile in FindProjectFiles(projectDir))
            {
                var displayName = Path.GetRelativePath(rootDir, projectFile).Replace('\\', '/');
                results.Add((displayName, projectDir, projectFile));
            }
        }

        return results;
    }
}
