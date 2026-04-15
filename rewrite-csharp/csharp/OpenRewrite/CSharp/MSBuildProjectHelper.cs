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

using System.Diagnostics;
using System.Text.Json;
using System.Xml.Linq;
using OpenRewrite.Xml;
using Serilog;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp;

/// <summary>
///     Creates and regenerates MSBuildProject markers.
///     Only the Sdk attribute is read from the csproj XML itself; all other
///     metadata (target frameworks, package references, resolved packages,
///     project references) comes from project.assets.json produced by dotnet restore.
///     After a recipe modifies a .csproj file, this helper regenerates the
///     marker by running `dotnet restore` and re-reading project.assets.json.
///     Uses <see cref="DotNetBuildContext" /> to materialize the full repository
///     build context (other .csproj files, Directory.Build.props/.targets,
///     nuget.config, etc.) before running restore, so that project references
///     and MSBuild imports resolve correctly.
/// </summary>
public static class MSBuildProjectHelper
{
    private static readonly TimeSpan RestoreTimeout = TimeSpan.FromMinutes(2);

    #region Marker creation

    /// <summary>
    ///     Creates an MSBuildProject marker. Only the Sdk attribute is read from the XML document.
    ///     All dependency and framework metadata comes from project.assets.json.
    /// </summary>
    /// <param name="doc">The parsed XML document (only Sdk attribute is read).</param>
    /// <param name="rootDir">Root directory for resolving project.assets.json and nuget.config.</param>
    public static MSBuildProject? CreateMarker(Document doc, string? rootDir = null)
    {
        var root = doc.Root;
        var sdk = root.GetAttributeValue("Sdk");

        if (rootDir == null)
            return new MSBuildProject(Guid.NewGuid(), sdk);

        var projectDir = Path.GetDirectoryName(Path.Combine(rootDir, doc.SourcePath));
        if (projectDir == null)
            return new MSBuildProject(Guid.NewGuid(), sdk);

        var assetsPath = Path.Combine(projectDir, "obj", "project.assets.json");
        if (!File.Exists(assetsPath))
            return new MSBuildProject(Guid.NewGuid(), sdk);

        try
        {
            return CreateFromAssetsJson(sdk, assetsPath, projectDir);
        }
        catch (Exception ex)
        {
            Log.Debug("Failed to read project.assets.json at {Path}: {Error}", assetsPath, ex.Message);
            return new MSBuildProject(Guid.NewGuid(), sdk);
        }
    }

    private static MSBuildProject CreateFromAssetsJson(string? sdk, string assetsPath, string projectDir)
    {
        var json = File.ReadAllText(assetsPath);
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        // Read declared dependencies per TFM from project.frameworks
        var declaredDeps = ReadDeclaredDependencies(root);

        // Read resolved packages from targets
        var resolvedByTfm = ReadResolvedPackages(root);

        // Read project references from libraries (type=project)
        var projectRefs = ReadProjectReferences(root, projectDir);

        // Build per-TFM metadata
        var tfmList = new List<TargetFramework>();
        // Use targets keys as the authoritative list of TFMs
        if (root.TryGetProperty("targets", out var targets))
            foreach (var target in targets.EnumerateObject())
            {
                var tfm = NormalizeTfm(target.Name);
                declaredDeps.TryGetValue(tfm, out var pkgRefs);
                resolvedByTfm.TryGetValue(tfm, out var resolved);

                // Cross-reference declared package refs with resolved versions
                if (pkgRefs != null && resolved != null)
                {
                    var resolvedLookup = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
                    foreach (var rp in resolved)
                        resolvedLookup[rp.Name] = rp.ResolvedVersion;

                    pkgRefs = pkgRefs.Select(pr =>
                    {
                        resolvedLookup.TryGetValue(pr.Include, out var resolvedVersion);
                        return pr.WithResolvedVersion(resolvedVersion);
                    }).ToList();
                }

                tfmList.Add(new TargetFramework(
                    tfm,
                    pkgRefs ?? [],
                    resolved ?? [],
                    projectRefs));
            }

        // Read package sources from nuget.config files walking up the directory tree
        var packageSources = ReadPackageSourcesFromTree(projectDir);

        return new MSBuildProject(
            Guid.NewGuid(),
            sdk,
            new Dictionary<string, PropertyValue>(),
            packageSources,
            tfmList);
    }

    private static Dictionary<string, IList<PackageReference>> ReadDeclaredDependencies(JsonElement root)
    {
        var result = new Dictionary<string, IList<PackageReference>>();

        if (!root.TryGetProperty("project", out var project) ||
            !project.TryGetProperty("frameworks", out var frameworks))
            return result;

        foreach (var fw in frameworks.EnumerateObject())
        {
            var tfm = fw.Name;
            var pkgRefs = new List<PackageReference>();

            if (fw.Value.TryGetProperty("dependencies", out var deps))
                foreach (var dep in deps.EnumerateObject())
                {
                    // Skip project references — they have "target": "Project"
                    if (dep.Value.TryGetProperty("target", out var targetProp) &&
                        targetProp.GetString() == "Project")
                        continue;

                    string? requestedVersion = null;
                    if (dep.Value.TryGetProperty("version", out var versionProp)) requestedVersion = ParseVersionRange(versionProp.GetString());

                    pkgRefs.Add(new PackageReference(dep.Name, requestedVersion));
                }

            result[tfm] = pkgRefs;
        }

        return result;
    }

    /// <summary>
    ///     Parses NuGet version range notation to extract the minimum version.
    ///     "[4.13.1, )" → "4.13.1", "[1.0.0]" → "1.0.0"
    /// </summary>
    private static string? ParseVersionRange(string? versionRange)
    {
        if (string.IsNullOrEmpty(versionRange))
            return null;

        var trimmed = versionRange.TrimStart('[', '(');
        var commaIdx = trimmed.IndexOf(',');
        if (commaIdx >= 0)
            trimmed = trimmed[..commaIdx];
        trimmed = trimmed.TrimEnd(']', ')').Trim();
        return trimmed.Length > 0 ? trimmed : null;
    }

    private static Dictionary<string, IList<ResolvedPackage>> ReadResolvedPackages(JsonElement root)
    {
        var result = new Dictionary<string, IList<ResolvedPackage>>();

        if (!root.TryGetProperty("targets", out var targets))
            return result;

        // Build a set of project-type libraries to exclude from resolved packages
        var projectLibraries = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        if (root.TryGetProperty("libraries", out var libraries))
            foreach (var lib in libraries.EnumerateObject())
                if (lib.Value.TryGetProperty("type", out var typeProp) &&
                    typeProp.GetString() == "project")
                    projectLibraries.Add(lib.Name);

        foreach (var target in targets.EnumerateObject())
        {
            var normalizedTfm = NormalizeTfm(target.Name);
            var packages = new List<ResolvedPackage>();

            foreach (var pkg in target.Value.EnumerateObject())
            {
                // Skip project references in the targets list
                if (projectLibraries.Contains(pkg.Name))
                    continue;

                var parts = pkg.Name.Split('/');
                if (parts.Length == 2)
                {
                    var deps = new List<ResolvedPackage>();
                    if (pkg.Value.TryGetProperty("dependencies", out var depsElement))
                        foreach (var dep in depsElement.EnumerateObject())
                            deps.Add(new ResolvedPackage(dep.Name, dep.Value.GetString() ?? ""));

                    packages.Add(new ResolvedPackage(parts[0], parts[1], deps));
                }
            }

            result[normalizedTfm] = packages;
        }

        return result;
    }

    private static List<ProjectReference> ReadProjectReferences(JsonElement root, string projectDir)
    {
        var projectRefs = new List<ProjectReference>();

        if (!root.TryGetProperty("libraries", out var libraries))
            return projectRefs;

        foreach (var lib in libraries.EnumerateObject())
        {
            if (!lib.Value.TryGetProperty("type", out var typeProp) ||
                typeProp.GetString() != "project")
                continue;

            // msbuildProject contains the relative path from the assets.json location
            if (lib.Value.TryGetProperty("msbuildProject", out var msbuildProjectProp))
            {
                var relativePath = msbuildProjectProp.GetString();
                if (relativePath != null)
                {
                    // msbuildProject path is relative to the project directory
                    // Normalize it to a clean relative path
                    var absolutePath = Path.GetFullPath(Path.Combine(projectDir, relativePath));
                    var normalizedRelative = Path.GetRelativePath(projectDir, absolutePath);
                    projectRefs.Add(new ProjectReference(normalizedRelative));
                }
            }
        }

        return projectRefs;
    }

    private static string NormalizeTfm(string tfm)
    {
        if (tfm.StartsWith(".NETCoreApp,Version=v"))
        {
            var version = tfm[".NETCoreApp,Version=v".Length..];
            return "net" + version;
        }

        if (tfm.StartsWith(".NETStandard,Version=v"))
        {
            var version = tfm[".NETStandard,Version=v".Length..];
            return "netstandard" + version;
        }

        if (tfm.StartsWith(".NETFramework,Version=v"))
        {
            var version = tfm[".NETFramework,Version=v".Length..].Replace(".", "");
            return "net" + version;
        }

        return tfm;
    }

    /// <summary>
    ///     Reads package sources by walking up the directory tree from projectDir to the root,
    ///     collecting nuget.config files (case-insensitive). Processes them from farthest to
    ///     closest, honoring &lt;clear/&gt; and &lt;remove/&gt; elements in packageSources.
    /// </summary>
    private static List<PackageSource> ReadPackageSourcesFromTree(string projectDir)
    {
        // Collect nuget.config files from project dir up to root
        var configFiles = new List<string>();
        var dir = projectDir;
        while (dir != null)
        {
            var configPath = FindNuGetConfig(dir);
            if (configPath != null)
                configFiles.Add(configPath);

            var parent = Path.GetDirectoryName(dir);
            if (parent == dir) break; // root
            dir = parent;
        }

        // Process from farthest (root) to closest (project) so that closer configs
        // can override or clear sources from farther ones
        configFiles.Reverse();

        var sources = new List<PackageSource>();
        foreach (var configFile in configFiles)
            try
            {
                ApplyPackageSources(configFile, sources);
            }
            catch (Exception ex)
            {
                Log.Debug("Failed to read nuget.config at {Path}: {Error}", configFile, ex.Message);
            }

        return sources;
    }

    /// <summary>
    ///     Finds a nuget.config file in the given directory using case-insensitive matching.
    /// </summary>
    private static string? FindNuGetConfig(string directory)
    {
        try
        {
            foreach (var file in Directory.EnumerateFiles(directory))
                if (Path.GetFileName(file).Equals("nuget.config", StringComparison.OrdinalIgnoreCase))
                    return file;
        }
        catch
        {
            // Permission denied or other IO error
        }

        return null;
    }

    /// <summary>
    ///     Applies package sources from a nuget.config file, honoring &lt;clear/&gt; elements.
    ///     A &lt;clear/&gt; element removes all previously accumulated sources.
    /// </summary>
    private static void ApplyPackageSources(string configPath, List<PackageSource> sources)
    {
        var xmlDoc = XDocument.Load(configPath);
        var packageSources = xmlDoc.Root?.Element("packageSources");
        if (packageSources == null) return;

        foreach (var element in packageSources.Elements())
            if (element.Name.LocalName.Equals("clear", StringComparison.OrdinalIgnoreCase))
            {
                sources.Clear();
            }
            else if (element.Name.LocalName.Equals("add", StringComparison.OrdinalIgnoreCase))
            {
                var key = element.Attribute("key")?.Value;
                var url = element.Attribute("value")?.Value;
                if (key != null && url != null)
                {
                    // Remove existing source with same key before adding (closer config wins)
                    sources.RemoveAll(s => s.Key.Equals(key, StringComparison.OrdinalIgnoreCase));
                    sources.Add(new PackageSource(key, url));
                }
            }
            else if (element.Name.LocalName.Equals("remove", StringComparison.OrdinalIgnoreCase))
            {
                var key = element.Attribute("key")?.Value;
                if (key != null) sources.RemoveAll(s => s.Key.Equals(key, StringComparison.OrdinalIgnoreCase));
            }
    }

    #endregion

    #region Marker regeneration

    /// <summary>
    ///     Regenerates the MSBuildProject marker on a .csproj document after mutation.
    ///     1. Writes all captured build files from DotNetBuildContext to a temp directory
    ///     2. Writes the modified .csproj (overwriting the scanned version if present)
    ///     3. Runs `dotnet restore` to resolve dependencies
    ///     4. Reads back project.assets.json
    ///     5. Rebuilds the MSBuildProject marker from updated XML + fresh assets
    ///     6. Replaces the old marker on the document
    /// </summary>
    public static Document RegenerateAndRefreshMarker(Document updated, ExecutionContext ctx)
    {
        var existingMarker = updated.Markers.FindFirst<MSBuildProject>();
        if (existingMarker == null)
            return updated;

        // Print the updated .csproj content
        var content = XmlParser.Print(updated);

        string? tempDir = null;
        try
        {
            tempDir = Path.Combine(Path.GetTempPath(), "openrewrite-dotnet-" + Guid.NewGuid().ToString("N")[..8]);
            Directory.CreateDirectory(tempDir);

            // Materialize all captured build files from the repository context
            var buildContext = DotNetBuildContext.Get(ctx);
            if (buildContext != null) buildContext.MaterializeAll(tempDir, updated.SourcePath);

            // Write the modified .csproj file (overwrites any version from build context)
            var csprojPath = Path.Combine(tempDir, updated.SourcePath);
            var csprojDir = Path.GetDirectoryName(csprojPath);
            if (csprojDir != null)
                Directory.CreateDirectory(csprojDir);
            File.WriteAllText(csprojPath, content);

            // Run dotnet restore
            var restoreResult = RunDotnetRestore(csprojPath);
            if (!restoreResult.Success)
            {
                Log.Debug("MSBuildProjectHelper: dotnet restore failed for {Path}: {Error}",
                    updated.SourcePath, restoreResult.Error);
                return RefreshMarkerFromXmlOnly(updated, existingMarker);
            }

            // Rebuild marker from XML + project.assets.json
            var newMarker = CreateMarker(updated, tempDir);
            if (newMarker != null)
            {
                newMarker = newMarker.WithId(existingMarker.Id);
                updated = updated.WithMarkers(
                    updated.Markers.Remove<MSBuildProject>().Add(newMarker));
            }

            return updated;
        }
        catch (Exception ex)
        {
            Log.Debug("MSBuildProjectHelper: reattestation failed for {Path}: {Error}",
                updated.SourcePath, ex.Message);
            return RefreshMarkerFromXmlOnly(updated, existingMarker);
        }
        finally
        {
            if (tempDir != null)
                try
                {
                    Directory.Delete(tempDir, true);
                }
                catch
                {
                    /* best effort cleanup */
                }
        }
    }

    /// <summary>
    ///     Creates an XmlVisitor that regenerates the MSBuildProject marker on modified documents.
    ///     Register via <c>DoAfterVisit(MSBuildProjectHelper.RegenerateMarkerVisitor())</c>
    ///     from any visitor that modifies .csproj files.
    /// </summary>
    public static XmlVisitor<ExecutionContext> RegenerateMarkerVisitor()
    {
        return new MarkerRegenVisitor();
    }

    private class MarkerRegenVisitor : XmlVisitor<ExecutionContext>
    {
        public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
        {
            if (document.Markers.FindFirst<MSBuildProject>() != null)
                return RegenerateAndRefreshMarker(document, ctx);
            return document;
        }
    }

    private static Document RefreshMarkerFromXmlOnly(Document updated, MSBuildProject existingMarker)
    {
        var newMarker = CreateMarker(updated);
        if (newMarker != null)
        {
            newMarker = newMarker.WithId(existingMarker.Id);
            updated = updated.WithMarkers(
                updated.Markers.Remove<MSBuildProject>().Add(newMarker));
        }

        return updated;
    }

    private record RestoreResult(bool Success, string? Error);

    private static RestoreResult RunDotnetRestore(string csprojPath)
    {
        try
        {
            var psi = new ProcessStartInfo("dotnet")
            {
                WorkingDirectory = Path.GetDirectoryName(csprojPath) ?? ".",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            psi.ArgumentList.Add("restore");
            psi.ArgumentList.Add(csprojPath);
            psi.ArgumentList.Add("/p:NuGetAudit=false");
            psi.ArgumentList.Add("/p:RestoreIgnoreFailedSources=true");
            psi.ArgumentList.Add("--ignore-failed-sources");
            psi.Environment["NUGET_ENHANCED_MAX_NETWORK_TRY_COUNT"] = "1";
            psi.Environment["NUGET_ENHANCED_NETWORK_RETRY_DELAY_MILLISECONDS"] = "100";

            using var process = Process.Start(psi);
            if (process == null)
                return new RestoreResult(false, "Failed to start dotnet restore");

            var stderrTask = process.StandardError.ReadToEndAsync();
            var stdout = process.StandardOutput.ReadToEnd();
            var stderr = stderrTask.GetAwaiter().GetResult();
            process.WaitForExit((int)RestoreTimeout.TotalMilliseconds);

            if (!process.HasExited)
            {
                try
                {
                    process.Kill(true);
                }
                catch
                {
                }

                return new RestoreResult(false, "dotnet restore timed out");
            }

            return process.ExitCode == 0
                ? new RestoreResult(true, null)
                : new RestoreResult(false, $"Exit code {process.ExitCode}: {stderr}");
        }
        catch (Exception ex)
        {
            return new RestoreResult(false, ex.Message);
        }
    }

    #endregion
}