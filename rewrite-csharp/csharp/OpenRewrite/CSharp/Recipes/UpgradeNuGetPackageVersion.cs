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
using System.Net.Http;
using System.Text.Json;
using NuGet.Versioning;
using OpenRewrite.Core;
using OpenRewrite.Xml;
using Serilog;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Upgrades a NuGet package reference version in .csproj files.
/// Uses NuGet.Versioning for version parsing and comparison, correctly handling
/// NuGet version ranges, 4-part versions, and prerelease labels.
/// </summary>
public class UpgradeNuGetPackageVersion : ScanningRecipe<UpgradeNuGetPackageVersion.Accumulator>
{
    public override string DisplayName => "Upgrade NuGet package version";

    public override string Description =>
        "Upgrades the version of a NuGet `<PackageReference>` or `<PackageVersion>` in .csproj " +
        "and Directory.Packages.props files. Handles property references by updating the property " +
        "value instead of the version attribute. Uses NuGet.Versioning for correct version semantics.";

    [Option(DisplayName = "Package name",
        Description = "The NuGet package name to upgrade. Supports glob patterns.",
        Example = "Newtonsoft.Json")]
    public string PackageName { get; set; } = "";

    [Option(DisplayName = "New version",
        Description = "An exact version number, or a NuGet version range (e.g. '[14.0,)' for >= 14.0). " +
                       "Use 'latest' to upgrade to the latest available version from NuGet sources.",
        Example = "14.0.0")]
    public string NewVersion { get; set; } = "";

    [Option(DisplayName = "Include prerelease",
        Description = "Whether to include prerelease versions when resolving 'latest' or ranges.",
        Required = false)]
    public bool IncludePrerelease { get; set; }

    public class Accumulator
    {
        /// <summary>
        /// The DotNetBuildContext holding all captured build files.
        /// </summary>
        public DotNetBuildContext BuildContext { get; set; } = null!;

        /// <summary>
        /// Resolved target version per package name.
        /// </summary>
        public Dictionary<string, string> ResolvedVersions { get; } = new();

        /// <summary>
        /// Properties that need updating (property name -> new value), keyed by source path.
        /// </summary>
        public Dictionary<string, Dictionary<string, string>> PropertyUpdates { get; } = new();
    }

    public override Accumulator GetInitialValue(ExecutionContext ctx)
    {
        return new Accumulator { BuildContext = DotNetBuildContext.GetOrCreate(ctx) };
    }

    public override ITreeVisitor<ExecutionContext> GetScanner(Accumulator acc)
    {
        return new BuildContextScanner();
    }

    public override IEnumerable<SourceFile> Generate(Accumulator acc, ExecutionContext ctx)
    {
        AnalyzeBuildContext(acc);
        return [];
    }

    public override ITreeVisitor<ExecutionContext> GetVisitor(Accumulator acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new UpgradeNuGetPackageVersionVisitor(PackageName, acc.ResolvedVersions, acc.PropertyUpdates));
    }

    /// <summary>
    /// Runs after scan, before edit. Analyzes all captured Documents to:
    /// 1. Resolve target versions for matching packages
    /// 2. Determine which properties can safely be updated
    /// </summary>
    private void AnalyzeBuildContext(Accumulator acc)
    {
        // Track ALL packages that use each property (not just targeted ones)
        // so we can detect when a property is shared with non-targeted packages
        var packagesByProperty = new Dictionary<string, HashSet<string>>(); // property name -> set of ALL package names using it
        var propertyDefinitions = new Dictionary<string, List<(string sourcePath, string value)>>(); // property name -> definitions
        MSBuildProject? anyMarker = null;

        foreach (var (sourcePath, doc) in acc.BuildContext.Documents)
        {
            var marker = doc.Markers.FindFirst<MSBuildProject>();
            if (marker != null)
            {
                anyMarker ??= marker;

                // Resolve target versions from marker (which has MSBuild-resolved values)
                foreach (var tfm in marker.TargetFrameworks)
                {
                    foreach (var pkgRef in tfm.PackageReferences)
                    {
                        if (!GlobMatcher.Matches(pkgRef.Include, PackageName))
                            continue;

                        var currentVersion = pkgRef.ResolvedVersion;
                        var targetVersion = ResolveTargetVersion(pkgRef.Include, currentVersion, marker, NewVersion, IncludePrerelease);
                        if (targetVersion != null)
                            acc.ResolvedVersions.TryAdd(pkgRef.Include, targetVersion);
                    }
                }
            }

            // Scan raw XML for property references in PackageReference Version attributes.
            // The marker has MSBuild-resolved values; the XML has the actual "program" text
            // (e.g., $(PropertyName)) which we need to detect property-based versioning.
            ScanPackageReferencePropertyUsage(doc, packagesByProperty);

            // Scan this document's XML for property definitions
            ScanPropertyDefinitions(doc, sourcePath, propertyDefinitions);
        }

        // Also scan XML for PackageVersion elements (CPM in Directory.Packages.props)
        foreach (var (sourcePath, doc) in acc.BuildContext.Documents)
        {
            ScanPackageVersionElements(doc, acc, anyMarker);
        }

        // For packages that use property references and weren't resolved from the marker
        // (e.g., cross-file property definitions where the property wasn't available during
        // standalone restore), resolve using the property's defined value as the current version.
        foreach (var (propName, users) in packagesByProperty)
        {
            if (!propertyDefinitions.TryGetValue(propName, out var propDefs) || propDefs.Count != 1)
                continue;
            var (_, currentValue) = propDefs[0];
            if (IsPropertyReference(currentValue))
                continue;

            foreach (var pkgName in users)
            {
                if (acc.ResolvedVersions.ContainsKey(pkgName))
                    continue;
                if (!GlobMatcher.Matches(pkgName, PackageName))
                    continue;

                var targetVersion = ResolveTargetVersion(pkgName, currentValue, anyMarker, NewVersion, IncludePrerelease);
                if (targetVersion != null)
                    acc.ResolvedVersions.TryAdd(pkgName, targetVersion);
            }
        }

        // Determine which properties are safe to update
        foreach (var (propName, propDefs) in propertyDefinitions)
        {
            // Skip if the property is defined more than once (complex/conditional logic)
            if (propDefs.Count != 1)
            {
                Log.Debug("UpgradeNuGetPackageVersion: skipping property {Property} - defined {Count} times",
                    propName, propDefs.Count);
                continue;
            }

            // Skip if the property is not exclusively used by our targeted packages
            if (!packagesByProperty.TryGetValue(propName, out var users))
                continue;

            var allUsersAreTargeted = users.All(u => acc.ResolvedVersions.ContainsKey(u));
            if (!allUsersAreTargeted)
            {
                Log.Debug("UpgradeNuGetPackageVersion: skipping property {Property} - used by non-targeted packages: {Packages}",
                    propName, string.Join(", ", users.Where(u => !acc.ResolvedVersions.ContainsKey(u))));
                continue;
            }

            // All users are targeted - pick the target version (they should all agree;
            // use the first one since they're all being upgraded to the same spec)
            var targetVersion = users.Select(u => acc.ResolvedVersions.GetValueOrDefault(u)).FirstOrDefault(v => v != null);
            if (targetVersion == null)
                continue;

            var (defSourcePath, currentValue) = propDefs[0];

            // Don't update if the property value itself is a property reference (complex logic)
            if (IsPropertyReference(currentValue))
            {
                Log.Debug("UpgradeNuGetPackageVersion: skipping property {Property} - value is a property reference: {Value}",
                    propName, currentValue);
                continue;
            }

            // Don't update if already at target
            if (currentValue == targetVersion)
                continue;

            if (!acc.PropertyUpdates.TryGetValue(defSourcePath, out var props))
            {
                props = new Dictionary<string, string>();
                acc.PropertyUpdates[defSourcePath] = props;
            }
            props[propName] = targetVersion;
        }
    }

    /// <summary>
    /// Scans raw XML for PackageReference elements whose Version attribute contains a
    /// property reference like $(PropertyName). The marker has MSBuild-resolved values,
    /// so we read the XML directly to discover property-based versioning.
    /// </summary>
    private static void ScanPackageReferencePropertyUsage(Document doc,
        Dictionary<string, HashSet<string>> packagesByProperty)
    {
        var root = doc.Root;
        if (root.ContentList == null) return;

        foreach (var content in root.ContentList)
        {
            if (content is not Tag child || child.Name != "ItemGroup" || child.ContentList == null)
                continue;

            foreach (var item in child.ContentList)
            {
                if (item is not Tag itemTag || itemTag.Name != "PackageReference") continue;

                var include = itemTag.GetAttributeValue("Include");
                var version = itemTag.GetAttributeValue("Version");
                if (include == null || version == null || !IsPropertyReference(version))
                    continue;

                var propName = ExtractPropertyName(version);
                if (!packagesByProperty.TryGetValue(propName, out var users))
                {
                    users = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
                    packagesByProperty[propName] = users;
                }
                users.Add(include);
            }
        }
    }

    /// <summary>
    /// Walks an XML document's PropertyGroup elements to find property definitions.
    /// </summary>
    private static void ScanPropertyDefinitions(Document doc, string sourcePath,
        Dictionary<string, List<(string sourcePath, string value)>> propertyDefinitions)
    {
        var root = doc.Root;
        if (root.ContentList == null) return;

        foreach (var content in root.ContentList)
        {
            if (content is not Tag child || child.Name != "PropertyGroup" || child.ContentList == null)
                continue;

            foreach (var prop in child.ContentList)
            {
                if (prop is not Tag propTag) continue;
                var val = propTag.GetValue();
                if (val == null) continue;

                if (!propertyDefinitions.TryGetValue(propTag.Name, out var defs))
                {
                    defs = [];
                    propertyDefinitions[propTag.Name] = defs;
                }
                defs.Add((sourcePath, val));
            }
        }
    }

    /// <summary>
    /// Scans for PackageVersion elements (Central Package Management) and resolves versions.
    /// </summary>
    private void ScanPackageVersionElements(Document doc, Accumulator acc, MSBuildProject? marker)
    {
        var root = doc.Root;
        if (root.ContentList == null) return;

        foreach (var content in root.ContentList)
        {
            if (content is not Tag child) continue;

            if (child.Name == "ItemGroup" && child.ContentList != null)
            {
                foreach (var item in child.ContentList)
                {
                    if (item is not Tag itemTag || itemTag.Name != "PackageVersion") continue;

                    var include = itemTag.GetAttributeValue("Include");
                    if (include == null || !GlobMatcher.Matches(include, PackageName))
                        continue;

                    var currentVersion = itemTag.GetAttributeValue("Version");
                    if (currentVersion == null) continue;

                    var targetVersion = ResolveTargetVersion(include, currentVersion, marker, NewVersion, IncludePrerelease);
                    if (targetVersion != null)
                        acc.ResolvedVersions.TryAdd(include, targetVersion);
                }
            }
        }
    }

    /// <summary>
    /// Resolves the target version using NuGet.Versioning.
    /// Handles exact versions, NuGet version ranges, and "latest" keyword.
    /// </summary>
    internal static string? ResolveTargetVersion(
        string packageInclude,
        string? currentVersion,
        MSBuildProject? marker,
        string newVersionSpec,
        bool includePrerelease)
    {
        // Exact version: just return it if it's higher than current
        if (NuGetVersion.TryParse(newVersionSpec, out var exactVersion))
        {
            if (currentVersion != null && NuGetVersion.TryParse(currentVersion, out var current))
            {
                if (exactVersion <= current)
                    return null; // already at or above target
            }
            return exactVersion.ToNormalizedString();
        }

        // "latest" or "latest.release": fetch from NuGet sources
        if (newVersionSpec.StartsWith("latest", StringComparison.OrdinalIgnoreCase))
        {
            var available = FetchAvailableVersions(packageInclude, marker);
            if (available.Count == 0) return null;

            var candidates = includePrerelease
                ? available
                : available.Where(v => !v.IsPrerelease).ToList();

            if (candidates.Count == 0) return null;
            var best = candidates.Max()!;

            if (currentVersion != null && NuGetVersion.TryParse(currentVersion, out var current) && best <= current)
                return null;

            return best.ToNormalizedString();
        }

        // NuGet version range: e.g. "[14.0,)" or "14.*"
        if (VersionRange.TryParse(newVersionSpec, out var range))
        {
            var available = FetchAvailableVersions(packageInclude, marker);
            if (available.Count == 0) return null;

            var candidates = includePrerelease
                ? available
                : available.Where(v => !v.IsPrerelease).ToList();

            var best = range.FindBestMatch(candidates);
            if (best == null) return null;

            if (currentVersion != null && NuGetVersion.TryParse(currentVersion, out var current) && best <= current)
                return null;

            return best.ToNormalizedString();
        }

        return null;
    }

    private static List<NuGetVersion> FetchAvailableVersions(string packageName, MSBuildProject? marker)
    {
        var sources = marker?.PackageSources ?? [];

        // Try configured sources first
        foreach (var source in sources)
        {
            var versions = FetchVersionsFromSource(packageName, source.Url);
            if (versions.Count > 0) return versions;
        }

        // Fall back to nuget.org
        return FetchVersionsFromFlatContainer(packageName, "https://api.nuget.org/v3-flatcontainer/");
    }

    private static List<NuGetVersion> FetchVersionsFromSource(string packageName, string sourceUrl)
    {
        try
        {
            var flatContainerUrl = ResolveFlatContainerUrl(sourceUrl);
            if (flatContainerUrl == null) return [];
            return FetchVersionsFromFlatContainer(packageName, flatContainerUrl);
        }
        catch
        {
            return [];
        }
    }

    private static List<NuGetVersion> FetchVersionsFromFlatContainer(string packageName, string baseUrl)
    {
        try
        {
            var url = baseUrl.TrimEnd('/') + "/" + packageName.ToLowerInvariant() + "/index.json";
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
            var json = client.GetStringAsync(url).GetAwaiter().GetResult();
            using var doc = JsonDocument.Parse(json);

            if (!doc.RootElement.TryGetProperty("versions", out var versionsElement))
                return [];

            var result = new List<NuGetVersion>();
            foreach (var v in versionsElement.EnumerateArray())
            {
                if (NuGetVersion.TryParse(v.GetString(), out var version))
                    result.Add(version);
            }
            return result;
        }
        catch (Exception ex)
        {
            Log.Debug("Failed to fetch NuGet versions for {Package}: {Error}", packageName, ex.Message);
            return [];
        }
    }

    private static string? ResolveFlatContainerUrl(string serviceIndexUrl)
    {
        try
        {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
            var json = client.GetStringAsync(serviceIndexUrl).GetAwaiter().GetResult();
            using var doc = JsonDocument.Parse(json);

            if (!doc.RootElement.TryGetProperty("resources", out var resources))
                return null;

            foreach (var resource in resources.EnumerateArray())
            {
                var type = resource.TryGetProperty("@type", out var typeProp) ? typeProp.GetString() : "";
                if (type != null && type.StartsWith("PackageBaseAddress"))
                    return resource.GetProperty("@id").GetString();
            }
        }
        catch
        {
            // Fall through
        }
        return null;
    }

    private static bool IsPropertyReference(string? value)
        => value != null && value.StartsWith("$(") && value.EndsWith(")");

    private static string ExtractPropertyName(string propertyRef)
        => propertyRef[2..^1];
}
