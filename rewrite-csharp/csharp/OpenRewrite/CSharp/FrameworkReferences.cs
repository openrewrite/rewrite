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
using NuGet.Frameworks;
using NuGet.Versioning;
using OpenRewrite.CSharp.NuGet;

namespace OpenRewrite.CSharp;

internal sealed record FrameworkReferences(IReadOnlyList<string> Modules, IReadOnlyList<string> All)
{
    private static readonly ConcurrentDictionary<string, Lazy<Task<FrameworkReferences?>>> Resolved =
        new(StringComparer.OrdinalIgnoreCase);

    public static Task<FrameworkReferences?> ResolveAsync(string targetFramework) =>
        Resolved.GetOrAdd(targetFramework,
            tfm => new Lazy<Task<FrameworkReferences?>>(() => LoadAsync(tfm))).Value;

    private static async Task<FrameworkReferences?> LoadAsync(string targetFramework)
    {
        var dir = await ReferenceDirectoryAsync(NuGetFramework.Parse(targetFramework));
        if (dir == null)
        {
            return null;
        }
        var modules = Directory.GetFiles(dir, "*.dll").OrderBy(p => p, StringComparer.Ordinal).ToList();
        var facades = Path.Combine(dir, "Facades");
        var all = Directory.Exists(facades)
            ? modules.Concat(Directory.GetFiles(facades, "*.dll").OrderBy(p => p, StringComparer.Ordinal)).ToList()
            : modules;
        return new FrameworkReferences(modules, all);
    }

    private static async Task<string?> ReferenceDirectoryAsync(NuGetFramework framework)
    {
        var version = framework.Version;
        var folder = new NuGetFramework(framework.Framework, version).GetShortFolderName();
        switch (framework.Framework)
        {
            case FrameworkConstants.FrameworkIdentifiers.NetCoreApp when version.Major >= 3:
                return await TargetingPackAsync("Microsoft.NETCore.App.Ref", version, Path.Combine("ref", folder));
            case FrameworkConstants.FrameworkIdentifiers.NetCoreApp when version.Major == 2:
                return await TargetingPackAsync("Microsoft.NETCore.App", version, Path.Combine("ref", folder));
            case FrameworkConstants.FrameworkIdentifiers.NetStandard when version >= new Version(2, 1):
                return await TargetingPackAsync("NETStandard.Library.Ref", version, Path.Combine("ref", folder));
            case FrameworkConstants.FrameworkIdentifiers.NetStandard when version.Major == 2:
                return await TargetingPackAsync("NETStandard.Library", version, Path.Combine("build", folder, "ref"));
            case FrameworkConstants.FrameworkIdentifiers.Net:
                var targetFrameworkVersion = "v" + version.ToString(version.Build > 0 ? 3 : 2);
                var root = await SolutionRestore.NetFrameworkReferenceAssemblyRootAsync(
                    targetFrameworkVersion, CancellationToken.None);
                return root == null ? null : Path.Combine(root, ".NETFramework", targetFrameworkVersion);
            default:
                return null;
        }
    }

    private static async Task<string?> TargetingPackAsync(string packageId, Version frameworkVersion, string assetsDir)
    {
        var packDirs = MSBuildEnvironment.CandidateDotnetRoots()
            .Select(root => Path.Combine(root, "packs", packageId))
            .Concat(SolutionParser.NuGetCacheRoots.Select(root => Path.Combine(root, packageId.ToLowerInvariant())))
            .Where(Directory.Exists);

        string? best = null;
        NuGetVersion? bestVersion = null;
        foreach (var versionDir in packDirs.SelectMany(Directory.EnumerateDirectories))
        {
            if (NuGetVersion.TryParse(Path.GetFileName(versionDir), out var packVersion)
                && !packVersion.IsPrerelease
                && packVersion.Major == frameworkVersion.Major
                && packVersion.Minor == frameworkVersion.Minor
                && (bestVersion == null || packVersion > bestVersion)
                && Directory.Exists(Path.Combine(versionDir, assetsDir)))
            {
                best = Path.Combine(versionDir, assetsDir);
                bestVersion = packVersion;
            }
        }
        if (best != null)
        {
            return best;
        }

        var fallback = new NuGetVersion(frameworkVersion.Major, frameworkVersion.Minor, 0).ToString();
        var cacheDir = Path.Combine(Path.GetTempPath(), "openrewrite-reference-packs", packageId + "." + fallback);
        var installed = Path.Combine(cacheDir, packageId, assetsDir);
        if (!Directory.Exists(installed))
        {
            await NuGetResolver.InstallPackageAsync(packageId, fallback, cacheDir, excludeVersion: true,
                CancellationToken.None);
        }
        return Directory.Exists(installed) ? installed : null;
    }
}
