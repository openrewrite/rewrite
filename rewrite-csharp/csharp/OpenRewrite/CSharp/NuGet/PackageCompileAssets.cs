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
using NuGet.Client;
using NuGet.Configuration;
using NuGet.ContentModel;
using NuGet.Frameworks;
using NuGet.Packaging;
using NuGet.ProjectModel;
using NuGet.RuntimeModel;
using NuGet.Versioning;

namespace OpenRewrite.CSharp.NuGet;

internal static class PackageCompileAssets
{
    private static readonly ConcurrentDictionary<string, byte> RestoredPackageFolders = new(StringComparer.Ordinal);
    private static readonly ManagedCodeConventions Conventions = new(new RuntimeGraph());
    private static readonly Lazy<IReadOnlyList<string>> SettingsPackageFolders = new(() =>
    {
        var settings = Settings.LoadDefaultSettings(root: null);
        return SettingsUtility.GetFallbackPackageFolders(settings)
            .Prepend(SettingsUtility.GetGlobalPackagesFolder(settings))
            .ToList();
    });

    public static void RecordPackageFolders(LockFile lockFile)
    {
        foreach (var folder in lockFile.PackageFolders)
        {
            if (!string.IsNullOrEmpty(folder.Path))
                RestoredPackageFolders.TryAdd(Path.GetFullPath(folder.Path), 0);
        }
    }

    public static IReadOnlyList<string> PackageFolders()
    {
        return RestoredPackageFolders.Keys
            .Concat(SettingsPackageFolders.Value)
            .Where(f => !string.IsNullOrEmpty(f))
            .Select(Path.GetFullPath)
            .Distinct(StringComparer.Ordinal)
            .ToList();
    }

    public static string? FindPackageDirectory(string id, string version, IEnumerable<string> packageFolders)
    {
        if (!NuGetVersion.TryParse(version, out var parsed))
            return null;
        return packageFolders
            .Select(folder => new VersionFolderPathResolver(folder).GetInstallPath(id, parsed))
            .FirstOrDefault(Directory.Exists);
    }

    public static List<string> CompileAssemblies(string packageDirectory, NuGetFramework framework)
    {
        var items = new ContentItemCollection();
        items.Load(new PackageFolderReader(packageDirectory).GetFiles());
        var group = items.FindBestItemGroup(
            Conventions.Criteria.ForFramework(framework),
            Conventions.Patterns.CompileRefAssemblies,
            Conventions.Patterns.CompileLibAssemblies);
        if (group == null)
            return [];
        return group.Items
            .Select(item => item.Path)
            .Where(path => !Path.GetFileName(path).Equals("_._", StringComparison.Ordinal))
            .Select(path => Path.Combine(packageDirectory, path))
            .Where(File.Exists)
            .ToList();
    }
}
