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

using System.Xml.Linq;
using NuGet.ProjectModel;
using OpenRewrite.CSharp.NuGet;
using OpenRewrite.Xml;
using Serilog;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp;

/// <summary>
///     Creates and regenerates MSBuildProject markers.
///     Only the Sdk attribute is read from the csproj XML itself; all other
///     metadata (target frameworks, package references, resolved packages,
///     project references) comes from the in-memory NuGet <see cref="LockFile" />
///     produced by <see cref="NuGetResolver" /> (in-process PackageSpec/RestoreRunner
///     restore — no <c>dotnet restore</c> child process, no <c>project.assets.json</c> read).
///     Legacy <c>packages.config</c> projects get the same full attestation via a
///     PackageSpec synthesized from their packages.config entries.
///     After a recipe modifies a .csproj file, this helper regenerates the
///     marker by re-running the in-process restore against the materialized
///     <see cref="DotNetBuildContext" /> (other .csproj files,
///     Directory.Build.props/.targets, nuget.config, packages.config, etc.).
/// </summary>
public static class MSBuildProjectHelper
{
    // Keyed off ExecutionContext: the set of .csproj source paths that have been
    // structurally modified during this run and whose MSBuildProject marker is
    // therefore stale until the next restore. Mutating visitors add to
    // this set; RegenerateMarkerVisitor consumes from it so files no one touched
    // skip the (expensive) restore.
    private const string StaleAttestationsKey = "OpenRewrite.CSharp.StaleCsprojAttestations";

    /// <summary>
    /// Marks the given .csproj source path as having pending changes whose
    /// MSBuildProject marker no longer reflects on-disk state. Call this from
    /// any visitor after it mutates a project file (independent of whether the
    /// visitor reattests immediately or defers to a trailing
    /// <c>EnsureCsprojAttestation</c>).
    /// </summary>
    public static void MarkAttestationStale(ExecutionContext ctx, string sourcePath)
    {
        var set = ctx.ComputeMessageIfAbsent(
            StaleAttestationsKey,
            _ => new HashSet<string>(StringComparer.OrdinalIgnoreCase));
        lock (set)
        {
            set.Add(sourcePath);
        }
    }

    /// <summary>
    /// Returns true if the given .csproj source path has been marked stale and
    /// has not yet been reattested in this execution.
    /// </summary>
    public static bool IsAttestationStale(ExecutionContext ctx, string sourcePath)
    {
        var set = ctx.GetMessage<HashSet<string>>(StaleAttestationsKey);
        if (set == null) return false;
        lock (set)
        {
            return set.Contains(sourcePath);
        }
    }

    // Atomically removes the stale flag for this source path and returns whether
    // it was set. Used by the marker-regen visitor so a stale flag drives exactly
    // one reattestation regardless of whether it came from a mutating visitor's
    // immediate DoAfterVisit or from a trailing EnsureCsprojAttestation pass.
    private static bool TryConsumeStaleAttestation(ExecutionContext ctx, string sourcePath)
    {
        var set = ctx.GetMessage<HashSet<string>>(StaleAttestationsKey);
        if (set == null) return false;
        lock (set)
        {
            return set.Remove(sourcePath);
        }
    }

    #region Marker creation

    /// <summary>
    ///     Creates an MSBuildProject marker. Only the Sdk attribute is read from the XML document.
    ///     All dependency and framework metadata comes from an in-process NuGet restore of the
    ///     project on disk under <paramref name="rootDir"/> (PackageReference projects via
    ///     restore-graph generation; packages.config projects via a synthesized PackageSpec).
    /// </summary>
    /// <param name="doc">The parsed XML document (only Sdk attribute is read).</param>
    /// <param name="rootDir">Root directory the document's SourcePath is relative to.</param>
    public static MSBuildProject? CreateMarker(Document doc, string? rootDir = null)
    {
        var sdk = doc.Root.GetAttributeValue("Sdk");

        if (rootDir == null)
            return new MSBuildProject(Guid.NewGuid(), sdk);

        var projectPath = Path.GetFullPath(Path.Combine(rootDir, doc.SourcePath));
        if (!File.Exists(projectPath))
            return new MSBuildProject(Guid.NewGuid(), sdk);

        try
        {
            var lockFile = NuGetResolver
                .ResolveProjectLockFileAsync(projectPath, null, CancellationToken.None)
                .GetAwaiter().GetResult();
            if (lockFile == null)
                return new MSBuildProject(Guid.NewGuid(), sdk);

            var projectDir = Path.GetDirectoryName(projectPath)!;
            return CreateFromLockFile(sdk, lockFile, projectDir,
                includeDeclaredPackageReferences: !HasPackagesConfigSibling(projectDir));
        }
        catch (Exception ex)
        {
            Log.Debug("Failed to resolve lock file for {Path}: {Error}", projectPath, ex.Message);
            return new MSBuildProject(Guid.NewGuid(), sdk);
        }
    }

    /// <summary>
    ///     Creates an MSBuildProject marker from a pre-resolved <see cref="LockFile"/>
    ///     (e.g. the one produced during solution restore), avoiding a second restore.
    /// </summary>
    public static MSBuildProject CreateMarker(Document doc, LockFile lockFile, string projectDir)
    {
        var sdk = doc.Root.GetAttributeValue("Sdk");
        return CreateFromLockFile(sdk, lockFile, projectDir,
            includeDeclaredPackageReferences: !HasPackagesConfigSibling(projectDir));
    }

    /// <summary>
    ///     True when the project directory contains a packages.config. Such projects declare
    ///     dependencies in packages.config, not as csproj <c>&lt;PackageReference&gt;</c> items —
    ///     the marker's PackageReferences list must stay 1:1 with actual csproj items (recipes
    ///     use it for idempotency checks), so it is left empty and the direct dependencies are
    ///     represented as depth-0 entries in the resolved graph instead.
    /// </summary>
    private static bool HasPackagesConfigSibling(string projectDir)
    {
        try
        {
            return File.Exists(Path.Combine(projectDir, "packages.config"));
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    ///     Builds the marker from the in-memory NuGet lock file: declared package references
    ///     (from the restore's PackageSpec), the fully-linked resolved package graph with
    ///     per-package asset information, and project references.
    /// </summary>
    public static MSBuildProject CreateFromLockFile(string? sdk, LockFile lockFile, string projectDir,
        bool includeDeclaredPackageReferences = true)
    {
        var spec = lockFile.PackageSpec;

        // Declared dependencies per TFM alias (framework-specific + project-level)
        var declaredByTfm = new Dictionary<string, List<PackageReference>>(StringComparer.OrdinalIgnoreCase);
        if (spec != null)
        {
            foreach (var tfi in spec.TargetFrameworks)
            {
                var tfm = ShortTfm(tfi.FrameworkName, tfi.TargetAlias);
                var refs = new List<PackageReference>();
                foreach (var dep in tfi.Dependencies)
                    refs.Add(new PackageReference(dep.Name, dep.LibraryRange?.VersionRange?.MinVersion?.ToNormalizedString()));
                declaredByTfm[tfm] = refs;
            }
        }

        // Per-package file lists (analyzers, scripts, transforms, legacy content)
        var libraryFiles = new Dictionary<string, IList<string>>(StringComparer.OrdinalIgnoreCase);
        var projectLibraries = new Dictionary<string, string?>(StringComparer.OrdinalIgnoreCase);
        foreach (var library in lockFile.Libraries)
        {
            var key = library.Name + "/" + library.Version;
            if (string.Equals(library.Type, "project", StringComparison.OrdinalIgnoreCase))
                projectLibraries[key] = library.MSBuildProject;
            else if (library.Files != null)
                libraryFiles[key] = library.Files;
        }

        var projectRefs = new List<ProjectReference>();
        foreach (var msbuildProject in projectLibraries.Values)
        {
            if (msbuildProject == null)
                continue;
            try
            {
                var absolutePath = Path.GetFullPath(Path.Combine(projectDir, msbuildProject));
                projectRefs.Add(new ProjectReference(Path.GetRelativePath(projectDir, absolutePath)));
            }
            catch
            {
                projectRefs.Add(new ProjectReference(msbuildProject));
            }
        }

        var tfmList = new List<TargetFramework>();
        foreach (var target in lockFile.Targets)
        {
            // RID-specific targets duplicate the RID-less graph; the marker captures the
            // RID-agnostic view.
            if (!string.IsNullOrEmpty(target.RuntimeIdentifier))
                continue;

            var tfm = ShortTfm(target.TargetFramework, null);
            declaredByTfm.TryGetValue(tfm, out var declared);
            declared ??= declaredByTfm.Values.FirstOrDefault() ?? new List<PackageReference>();

            // First pass: create one node per resolved library with its asset data.
            var nodes = new Dictionary<string, ResolvedPackage>(StringComparer.OrdinalIgnoreCase);
            var dependencyNames = new Dictionary<string, List<string>>(StringComparer.OrdinalIgnoreCase);
            foreach (var library in target.Libraries)
            {
                if (library.Name == null || library.Version == null)
                    continue;
                var isProject = string.Equals(library.Type, "project", StringComparison.OrdinalIgnoreCase);
                var fileKey = library.Name + "/" + library.Version;
                libraryFiles.TryGetValue(fileKey, out var files);

                var analyzers = new List<string>();
                var hasInstallScripts = false;
                var hasXdt = false;
                var hasLegacyContent = false;
                if (files != null)
                {
                    foreach (var file in files)
                    {
                        var normalized = file.Replace('\\', '/');
                        if (normalized.StartsWith("analyzers/", StringComparison.OrdinalIgnoreCase) &&
                            normalized.EndsWith(".dll", StringComparison.OrdinalIgnoreCase))
                            analyzers.Add(normalized);
                        else if (normalized.StartsWith("tools/", StringComparison.OrdinalIgnoreCase) &&
                                 (normalized.EndsWith("install.ps1", StringComparison.OrdinalIgnoreCase) ||
                                  normalized.EndsWith("init.ps1", StringComparison.OrdinalIgnoreCase)))
                            hasInstallScripts = true;
                        else if (normalized.EndsWith(".xdt", StringComparison.OrdinalIgnoreCase) ||
                                 normalized.EndsWith(".transform", StringComparison.OrdinalIgnoreCase))
                            hasXdt = true;
                        else if (normalized.StartsWith("content/", StringComparison.OrdinalIgnoreCase))
                            hasLegacyContent = true;
                    }
                }

                var node = new ResolvedPackage(
                    library.Name,
                    library.Version.ToNormalizedString(),
                    dependencies: new List<ResolvedPackage>(),
                    depth: int.MaxValue,
                    type: isProject ? "project" : "package",
                    compileAssemblies: RealItems(library.CompileTimeAssemblies?.Select(i => i.Path)),
                    runtimeAssemblies: RealItems(library.RuntimeAssemblies?.Select(i => i.Path)),
                    frameworkAssemblies: library.FrameworkAssemblies?.ToList() ?? [],
                    buildFiles: RealItems(library.Build?.Select(i => i.Path)),
                    buildMultiTargetingFiles: RealItems(library.BuildMultiTargeting?.Select(i => i.Path)),
                    contentFiles: RealItems(library.ContentFiles?.Select(i => i.Path)),
                    runtimeTargets: RealItems(library.RuntimeTargets?.Select(i => i.Path)),
                    resourceAssemblies: RealItems(library.ResourceAssemblies?.Select(i => i.Path)),
                    analyzerAssemblies: analyzers,
                    hasInstallScripts: hasInstallScripts,
                    hasXdtTransforms: hasXdt,
                    hasLegacyContentFolder: hasLegacyContent);
                nodes[library.Name] = node;
                dependencyNames[library.Name] =
                    library.Dependencies?.Select(d => d.Id).ToList() ?? new List<string>();
            }

            // Second pass: link dependency edges to the actual resolved nodes (shared instances)
            // and compute depth as the shortest distance from a directly-declared dependency
            // (direct = 0). Nodes unreachable from declared roots (e.g. the roots themselves in
            // pathological graphs) keep the maximum observed depth.
            foreach (var (name, deps) in dependencyNames)
            {
                var list = (List<ResolvedPackage>)nodes[name].Dependencies;
                foreach (var depName in deps)
                {
                    if (nodes.TryGetValue(depName, out var depNode))
                        list.Add(depNode);
                }
            }

            var depths = ComputeDepths(declared.Select(d => d.Include), nodes, dependencyNames);
            var resolved = new List<ResolvedPackage>();
            foreach (var (name, node) in nodes)
                resolved.Add(node.WithDepth(depths.TryGetValue(name, out var d) ? d : 0));
            // Re-link after WithDepth created copies: rebuild edges against the final instances.
            var finalNodes = resolved.ToDictionary(n => n.Name, n => n, StringComparer.OrdinalIgnoreCase);
            foreach (var node in resolved)
            {
                var list = (List<ResolvedPackage>)node.Dependencies;
                for (var i = 0; i < list.Count; i++)
                {
                    if (finalNodes.TryGetValue(list[i].Name, out var final))
                        list[i] = final;
                }
            }

            // Cross-reference declared refs with resolved versions. For packages.config
            // projects the declared entries come from packages.config, not csproj
            // <PackageReference> items — they stay out of PackageReferences (kept 1:1 with
            // csproj items) and are represented as depth-0 nodes in the resolved graph.
            var resolvedLookup = finalNodes;
            var packageRefs = includeDeclaredPackageReferences
                ? declared
                    .Select(pr => resolvedLookup.TryGetValue(pr.Include, out var node)
                        ? pr.WithResolvedVersion(node.ResolvedVersion)
                        : pr)
                    .ToList()
                : new List<PackageReference>();

            tfmList.Add(new TargetFramework(tfm, packageRefs, resolved, projectRefs));
        }

        var packageSources = ReadPackageSourcesFromTree(projectDir);

        return new MSBuildProject(
            Guid.NewGuid(),
            sdk,
            new Dictionary<string, PropertyValue>(),
            packageSources,
            tfmList);
    }

    /// <summary>Strips the special <c>_._</c> placeholder entries from an asset list.</summary>
    private static IList<string> RealItems(IEnumerable<string?>? paths)
    {
        if (paths == null)
            return [];
        return paths
            .Where(p => p != null && !Path.GetFileName(p).Equals("_._", StringComparison.Ordinal))
            .Select(p => p!.Replace('\\', '/'))
            .ToList();
    }

    /// <summary>
    /// BFS from the directly-declared dependencies (depth 0) across the dependency edges.
    /// </summary>
    private static Dictionary<string, int> ComputeDepths(
        IEnumerable<string> roots,
        Dictionary<string, ResolvedPackage> nodes,
        Dictionary<string, List<string>> edges)
    {
        var depths = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase);
        var queue = new Queue<(string Name, int Depth)>();
        foreach (var root in roots)
        {
            if (nodes.ContainsKey(root) && depths.TryAdd(root, 0))
                queue.Enqueue((root, 0));
        }

        while (queue.Count > 0)
        {
            var (name, depth) = queue.Dequeue();
            if (!edges.TryGetValue(name, out var deps))
                continue;
            foreach (var dep in deps)
            {
                if (nodes.ContainsKey(dep) && depths.TryAdd(dep, depth + 1))
                    queue.Enqueue((dep, depth + 1));
            }
        }

        return depths;
    }

    private static string ShortTfm(global::NuGet.Frameworks.NuGetFramework? framework, string? alias)
    {
        if (!string.IsNullOrEmpty(alias))
            return alias;
        if (framework == null)
            return "";
        try
        {
            return framework.GetShortFolderName();
        }
        catch
        {
            return framework.ToString();
        }
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
    ///     3. Runs the in-process NuGet restore to resolve dependencies
    ///     4. Rebuilds the MSBuildProject marker from updated XML + the in-memory LockFile
    ///     5. Replaces the old marker on the document
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

            // In-process restore + marker rebuild from the in-memory lock file
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
            if (document.Markers.FindFirst<MSBuildProject>() == null)
                return document;
            // Gate on the stale flag so files no mutating recipe touched don't
            // trigger a restore. Consuming clears the flag so a second
            // reattestation pass (e.g., immediate + trailing
            // EnsureCsprojAttestation) doesn't run restore twice.
            if (!TryConsumeStaleAttestation(ctx, document.SourcePath))
                return document;
            return RegenerateAndRefreshMarker(document, ctx);
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

    #endregion

    #region Direct version resolution

    /// <summary>
    ///     Returns the resolved concrete version of a direct package dependency by running the
    ///     in-process restore for the project in <paramref name="projectDir"/>. Used by callers
    ///     (such as the InstallRecipes RPC handler) that need the concrete SemVer that NuGet
    ///     selected, rather than the version constraint authored in the csproj —
    ///     <c>dotnet add package &lt;pkg&gt; --version *</c> preserves the wildcard
    ///     verbatim in the csproj, so the resolved version must come from restore output.
    /// </summary>
    /// <exception cref="InvalidOperationException">
    ///     Thrown when the restore produced no lock file, when the package is not a direct
    ///     dependency under any TFM, or when no matching resolved entry exists.
    /// </exception>
    public static string GetResolvedPackageVersion(string projectDir, string packageName)
    {
        var projectPath = Directory.EnumerateFiles(projectDir, "*.csproj").FirstOrDefault()
                          ?? throw new InvalidOperationException($"No .csproj found in {projectDir}");

        var lockFile = NuGetResolver
            .ResolveProjectLockFileAsync(projectPath, null, CancellationToken.None)
            .GetAwaiter().GetResult();
        if (lockFile == null)
        {
            throw new InvalidOperationException(
                $"Restore produced no lock file for {projectPath}; restore did not complete");
        }

        return GetResolvedPackageVersion(lockFile, packageName, projectPath);
    }

    /// <summary>
    ///     Lock-file-level lookup behind <see cref="GetResolvedPackageVersion(string,string)"/>,
    ///     usable directly when the restore result is already in hand.
    /// </summary>
    public static string GetResolvedPackageVersion(LockFile lockFile, string packageName,
        string projectDescription = "project")
    {
        if (!IsDirectDependency(lockFile, packageName))
        {
            throw new InvalidOperationException(
                $"{packageName} is not a direct dependency of {projectDescription}");
        }

        foreach (var target in lockFile.Targets)
        foreach (var library in target.Libraries)
        {
            if (string.Equals(library.Type, "package", StringComparison.OrdinalIgnoreCase) &&
                string.Equals(library.Name, packageName, StringComparison.OrdinalIgnoreCase) &&
                library.Version != null)
            {
                return library.Version.ToNormalizedString();
            }
        }

        throw new InvalidOperationException(
            $"Could not find resolved version for {packageName} in restore result of {projectDescription}");
    }

    private static bool IsDirectDependency(LockFile lockFile, string packageName)
    {
        var spec = lockFile.PackageSpec;
        if (spec == null)
            return false;

        return spec.TargetFrameworks.Any(tfi =>
            tfi.Dependencies.Any(d => string.Equals(d.Name, packageName, StringComparison.OrdinalIgnoreCase)));
    }

    #endregion
}
