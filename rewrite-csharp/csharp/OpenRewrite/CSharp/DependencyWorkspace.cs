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
using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;
using NuGet.Frameworks;
using NuGet.ProjectModel;
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Restores one set of NuGet package references once and returns the compile assemblies they
/// resolve to, transitive packages included.
/// </summary>
internal static class DependencyWorkspace
{
    /// <summary>
    /// A workspace holds only a project file, its restore output and the assemblies that restore
    /// resolved to, so it is kept across processes. It sits beside the other <c>~/.rewrite</c>
    /// caches, out of reach of a <c>$TMPDIR</c> reaper.
    /// </summary>
    private static readonly string WorkspaceBase = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".rewrite", "csharp-workspaces");

    private const string TargetFramework = SolutionParser.LooseSourceTargetFramework;

    /// <summary>
    /// The compile assemblies a successful restore resolved to, one path per line. A process reuses
    /// them while every one of them still exists, without evaluating the project again.
    /// </summary>
    private const string Restored = "compile-assemblies.txt";

    private static readonly ConcurrentDictionary<string, Lazy<IReadOnlyList<string>>> Resolved = new();

    /// <summary>
    /// The compile assemblies of <paramref name="packages"/>, each an <c>id@version</c> coordinate.
    /// A set that cannot be restored resolves to no assemblies, so its sources still parse, without
    /// the types the packages would have contributed.
    /// </summary>
    public static IReadOnlyList<string> CompileAssemblies(IEnumerable<string> packages)
    {
        var coordinates = packages.Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(c => c, StringComparer.OrdinalIgnoreCase)
            .ToList();
        if (coordinates.Count == 0)
            return [];
        var key = Hash(coordinates);
        return Resolved.GetOrAdd(key, _ => new Lazy<IReadOnlyList<string>>(() => Restore(key, coordinates))).Value;
    }

    private static IReadOnlyList<string> Restore(string key, IReadOnlyList<string> coordinates)
    {
        var dir = Path.Combine(WorkspaceBase, key);
        var csproj = Path.Combine(dir, "Workspace.csproj");
        var restored = Path.Combine(dir, Restored);
        try
        {
            if (File.Exists(restored))
            {
                var assemblies = File.ReadAllLines(restored);
                if (assemblies.All(File.Exists))
                    return assemblies;
            }

            Directory.CreateDirectory(dir);
            var content = ProjectFile(coordinates);
            if (!File.Exists(csproj) || File.ReadAllText(csproj) != content)
                File.WriteAllText(csproj, content);

            var lockFiles = SolutionRestore.RunAsync(csproj, hasPackagesConfig: false,
                new Dictionary<string, string>(), CancellationToken.None).GetAwaiter().GetResult();
            if (!lockFiles.TryGetValue(Path.GetFullPath(csproj), out var lockFile))
            {
                Log.Warning("Restoring {Packages} produced no assets, so their types stay unresolved",
                    string.Join(", ", coordinates));
                return [];
            }
            var compileAssemblies = CompileAssemblies(lockFile);
            // A restore with errors, such as an unreachable feed, is not persisted, so a later process restores again.
            if (lockFile.LogMessages.Any(m => m.Level == global::NuGet.Common.LogLevel.Error))
            {
                Log.Warning("Restoring {Packages} reported errors, so some of their types may stay unresolved",
                    string.Join(", ", coordinates));
                return compileAssemblies;
            }
            // A concurrent process may write the same list, so each writes its own file and moves it in place.
            var temp = restored + "." + Guid.NewGuid().ToString("N");
            File.WriteAllLines(temp, compileAssemblies);
            File.Move(temp, restored, overwrite: true);
            return compileAssemblies;
        }
        catch (Exception ex)
        {
            Log.Warning("Restoring {Packages} failed, so their types stay unresolved: {ExType}: {ExMessage}",
                string.Join(", ", coordinates), ex.GetType().Name, ex.Message);
            return [];
        }
    }

    private static IReadOnlyList<string> CompileAssemblies(LockFile lockFile)
    {
        var packageFolder = lockFile.PackageFolders.FirstOrDefault()?.Path;
        var target = lockFile.Targets.FirstOrDefault(t =>
            t.RuntimeIdentifier == null && t.TargetFramework == NuGetFramework.Parse(TargetFramework));
        if (packageFolder == null || target == null)
            return [];

        var assemblies = new List<string>();
        foreach (var library in target.Libraries.Where(l => l.Type == "package"))
        {
            var libraryPath = lockFile.GetLibrary(library.Name, library.Version)?.Path;
            if (libraryPath == null)
                continue;
            assemblies.AddRange(library.CompileTimeAssemblies
                .Select(item => item.Path)
                .Where(path => !Path.GetFileName(path).Equals("_._", StringComparison.Ordinal))
                .Select(path => Path.Combine(packageFolder, libraryPath, path))
                .Where(File.Exists));
        }
        return assemblies;
    }

    private static string ProjectFile(IReadOnlyList<string> coordinates)
    {
        var sb = new StringBuilder();
        sb.Append("<Project Sdk=\"Microsoft.NET.Sdk\">\n");
        sb.Append($"  <PropertyGroup>\n    <TargetFramework>{TargetFramework}</TargetFramework>\n  </PropertyGroup>\n");
        sb.Append("  <ItemGroup>\n");
        foreach (var coordinate in coordinates)
        {
            var parts = coordinate.Split('@', 2);
            sb.Append($"    <PackageReference Include=\"{System.Security.SecurityElement.Escape(parts[0])}\" " +
                      $"Version=\"{System.Security.SecurityElement.Escape(parts[1])}\" />\n");
        }
        sb.Append("  </ItemGroup>\n</Project>\n");
        return sb.ToString();
    }

    private static string Hash(IReadOnlyList<string> coordinates)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(TargetFramework + "\n" + string.Join("\n", coordinates)));
        return Convert.ToHexString(bytes, 0, 8).ToLowerInvariant();
    }
}
