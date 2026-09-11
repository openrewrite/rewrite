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
using System.Xml.Linq;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.MSBuild;
using NuGet.Frameworks;
using NuGet.ProjectModel;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Format;
using OpenRewrite.CSharp.NuGet;
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Serializes in-process NuGet restores so that only one runs at a time (multiple tests may
/// load solutions concurrently) and caches which paths have already been restored.
/// All package operations run through <see cref="NuGetResolver"/> — no <c>dotnet restore</c>
/// or <c>nuget.exe</c> child processes.
/// </summary>
internal static class SolutionRestore
{
    private static readonly SemaphoreSlim Gate = new(1, 1);
    private static readonly Dictionary<string, IReadOnlyDictionary<string, LockFile>> Restored =
        new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// .NET Framework build assets that are not present on non-Windows machines. They are
    /// restored as NuGet packages and handed to MSBuildWorkspace as MSBuild properties so
    /// legacy projects can be evaluated: <c>VSToolsPath</c> resolves the web-application
    /// targets import and <c>TargetFrameworkRootPath</c> /
    /// <c>TargetFrameworkFallbackSearchPaths</c> resolve the reference assemblies.
    /// </summary>
    private const string WebTargetsPackage = "MSBuild.Microsoft.VisualStudio.Web_WebApplication.Targets";
    private const string WebTargetsVersion = "12.0.2";
    private const string ReferenceAssembliesVersion = "1.0.3";

    /// <summary>
    /// Directories to search for pre-provisioned .NET Framework reference assemblies before
    /// restoring them from NuGet, separated by <c>;</c>. Each entry is a
    /// <c>TargetFrameworkRootPath</c>, i.e. a directory containing
    /// <c>.NETFramework/&lt;version&gt;</c> subdirectories. Set this on machines with no access
    /// to the reference-assembly packages.
    /// </summary>
    public const string ReferenceAssembliesEnvironmentVariable = "REWRITE_DOTNET_REFERENCE_ASSEMBLIES";

    /// <summary>
    /// The .NET Framework versions that ship as <c>Microsoft.NETFramework.ReferenceAssemblies.*</c>
    /// packages, keyed by MSBuild <c>TargetFrameworkVersion</c>.
    /// </summary>
    private static readonly IReadOnlyDictionary<string, string> ReferenceAssemblyPackages =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["v2.0"] = "net20", ["v3.5"] = "net35", ["v4.0"] = "net40", ["v4.0.3"] = "net403",
            ["v4.5"] = "net45", ["v4.5.1"] = "net451", ["v4.5.2"] = "net452", ["v4.6"] = "net46",
            ["v4.6.1"] = "net461", ["v4.6.2"] = "net462", ["v4.7"] = "net47", ["v4.7.1"] = "net471",
            ["v4.7.2"] = "net472", ["v4.8"] = "net48", ["v4.8.1"] = "net481",
        };

    private static string? _vsToolsPath;
    private static bool _vsToolsPathResolved;

    // Reference-assembly root per TargetFrameworkVersion; a null value records a version whose
    // package could not be provisioned, so it is only attempted once per process.
    private static readonly Dictionary<string, string?> ReferenceAssemblyRoots =
        new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// MSBuild property values pointing at .NET Framework build assets.
    /// <see cref="VSToolsPath"/> is null when the web-application targets could not be restored,
    /// and <see cref="MissingVersions"/> lists the target framework versions whose reference
    /// assemblies are unavailable — those projects evaluate without type attestation.
    /// </summary>
    internal record NetFrameworkBuildAssets(
        string? VSToolsPath,
        IReadOnlyList<string> ReferenceAssemblyRoots,
        IReadOnlyList<string> MissingVersions);

    /// <summary>
    /// Restores a solution/project in-process: PackageReference projects via restore-graph
    /// generation + RestoreRunner (committing assets/props/targets so MSBuildWorkspace can
    /// compile), legacy packages.config projects via the solution-local packages folder plus a
    /// synthesized attestation graph. Returns the in-memory LockFile per project path.
    /// </summary>
    public static async Task<IReadOnlyDictionary<string, LockFile>> RunAsync(
        string path,
        bool hasPackagesConfig,
        IDictionary<string, string> msbuildProperties,
        CancellationToken ct)
    {
        var key = Path.GetFullPath(path);

        lock (Restored)
        {
            if (Restored.TryGetValue(key, out var cached))
            {
                Log.Debug("restore: skipped (already restored) {Path}", path);
                return cached;
            }
        }

        await Gate.WaitAsync(ct);
        try
        {
            lock (Restored)
            {
                if (Restored.TryGetValue(key, out var cached))
                    return cached;
            }

            var lockFiles = new Dictionary<string, LockFile>(StringComparer.OrdinalIgnoreCase);
            var rootDir = Path.GetDirectoryName(key) ?? ".";

            if (hasPackagesConfig)
            {
                // Materialize the solution-local packages/ folder for legacy HintPaths.
                var packagesConfigs = Directory
                    .EnumerateFiles(rootDir, "packages.config", SearchOption.AllDirectories)
                    .ToList();
                Log.Debug(">> packages.config restore ({Count} configs)", packagesConfigs.Count);
                await NuGetResolver.InstallPackagesConfigPackagesAsync(rootDir, packagesConfigs, ct);
                Log.Debug("<< packages.config restore");

                // Synthesized attestation graph per legacy project.
                foreach (var packagesConfig in packagesConfigs)
                {
                    var projectDir = Path.GetDirectoryName(packagesConfig)!;
                    foreach (var projectFile in Directory.EnumerateFiles(projectDir, "*.*proj"))
                    {
                        var lockFile = await NuGetResolver.RestorePackagesConfigGraphAsync(
                            projectFile, packagesConfig, NuGetResolver.ReadLegacyFramework(projectFile), ct);
                        if (lockFile != null)
                            lockFiles[Path.GetFullPath(projectFile)] = lockFile;
                    }
                }
            }

            // In-process restore of PackageReference projects (replaces `dotnet restore`).
            var sw = Stopwatch.StartNew();
            Log.Debug(">> in-process restore ({FileName})", Path.GetFileName(path));
            var dgSpec = NuGetResolver.CreateDependencyGraphSpec(key,
                msbuildProperties.Count > 0 ? msbuildProperties : null);
            if (dgSpec != null)
            {
                foreach (var (projectPath, lockFile) in await NuGetResolver.RestoreAsync(dgSpec, commit: true, ct))
                    lockFiles.TryAdd(projectPath, lockFile);
            }
            else if (!hasPackagesConfig)
            {
                // Degrade rather than abort: MSBuildWorkspace may still evaluate the
                // solution (packages already cached, or projects without dependencies),
                // and markers fall back to SDK-only attestation.
                Log.Warning("In-process restore could not produce a dependency graph for {Path}; " +
                            "continuing with degraded dependency attestation", path);
            }
            Log.Debug("<< in-process restore ({FileName}) ({Elapsed})", Path.GetFileName(path), sw.Elapsed);

            var result = (IReadOnlyDictionary<string, LockFile>)lockFiles;
            lock (Restored)
            {
                Restored[key] = result;
            }

            return result;
        }
        finally
        {
            Gate.Release();
        }
    }

    /// <summary>
    /// Provisions the reference assemblies for the given .NET Framework target versions plus the
    /// web-application targets, so MSBuildWorkspace can evaluate legacy projects on machines
    /// without a .NET Framework targeting pack. Each version is looked for in
    /// <see cref="ReferenceAssembliesEnvironmentVariable"/>, then in the NuGet global package
    /// cache, and is only downloaded when neither has it. Results are cached for the process
    /// lifetime.
    /// </summary>
    public static async Task<NetFrameworkBuildAssets> RestoreNetFrameworkBuildAssetsAsync(
        IEnumerable<string> frameworkVersions, CancellationToken ct)
    {
        await Gate.WaitAsync(ct);
        try
        {
            var cacheDir = Path.Combine(Path.GetTempPath(), "openrewrite-netfx-build-assets");

            if (!_vsToolsPathResolved)
            {
                var vsToolsPath = Path.Combine(cacheDir, WebTargetsPackage, "tools", "VSToolsPath");
                if (!Directory.Exists(vsToolsPath))
                    await NuGetResolver.InstallPackageAsync(
                        WebTargetsPackage, WebTargetsVersion, cacheDir, excludeVersion: true, ct);
                _vsToolsPath = Directory.Exists(vsToolsPath) ? vsToolsPath : null;
                _vsToolsPathResolved = true;
            }

            var roots = new List<string>();
            var missing = new List<string>();
            foreach (var version in frameworkVersions)
            {
                if (!ReferenceAssemblyRoots.TryGetValue(version, out var root))
                {
                    root = await ResolveReferenceAssemblyRootAsync(version, cacheDir, ct);
                    ReferenceAssemblyRoots[version] = root;
                }

                if (root == null)
                    missing.Add(version);
                else if (!roots.Contains(root, StringComparer.OrdinalIgnoreCase))
                    roots.Add(root);
            }

            // Reference assemblies already on the machine cost nothing to offer and cover
            // versions the project scan missed — a TargetFramework that only materializes once
            // MSBuild has evaluated a condition or Directory.Build.props, say. MSBuild ignores
            // a search path that does not hold the version a project asks for.
            foreach (var root in AvailableReferenceAssemblyRoots())
                if (!roots.Contains(root, StringComparer.OrdinalIgnoreCase))
                    roots.Add(root);

            Log.Debug("netfx build assets — VSToolsPath={VSToolsPath}, reference assembly roots=[{Roots}], missing=[{Missing}]",
                _vsToolsPath ?? "(missing)", string.Join(";", roots), string.Join(";", missing));
            return new NetFrameworkBuildAssets(_vsToolsPath, roots, missing);
        }
        finally
        {
            Gate.Release();
        }
    }

    /// <summary>
    /// Locates a <c>TargetFrameworkRootPath</c> holding the reference assemblies for a single
    /// <c>TargetFrameworkVersion</c>, restoring the matching NuGet package when necessary.
    /// </summary>
    private static async Task<string?> ResolveReferenceAssemblyRootAsync(
        string version, string cacheDir, CancellationToken ct)
    {
        foreach (var configured in PreProvisionedReferenceAssemblyRoots())
        {
            if (Directory.Exists(Path.Combine(configured, ".NETFramework", version)))
                return configured;
        }

        if (!ReferenceAssemblyPackages.TryGetValue(version, out var moniker))
        {
            Log.Debug("netfx build assets: no reference assembly package exists for {Version}", version);
            return null;
        }

        var packageId = "Microsoft.NETFramework.ReferenceAssemblies." + moniker;
        foreach (var cacheRoot in SolutionParser.NuGetCacheRoots)
        {
            var packageDir = Path.Combine(cacheRoot, packageId.ToLowerInvariant());
            if (!Directory.Exists(packageDir))
                continue;
            foreach (var installed in Directory.EnumerateDirectories(packageDir))
            {
                var cached = Path.Combine(installed, "build");
                if (Directory.Exists(Path.Combine(cached, ".NETFramework", version)))
                    return cached;
            }
        }

        var buildDir = Path.Combine(cacheDir, packageId, "build");
        if (!Directory.Exists(buildDir))
            await NuGetResolver.InstallPackageAsync(
                packageId, ReferenceAssembliesVersion, cacheDir, excludeVersion: true, ct);

        return Directory.Exists(Path.Combine(buildDir, ".NETFramework", version)) ? buildDir : null;
    }

    private static IEnumerable<string> PreProvisionedReferenceAssemblyRoots()
    {
        var configured = Environment.GetEnvironmentVariable(ReferenceAssembliesEnvironmentVariable);
        if (string.IsNullOrWhiteSpace(configured))
            yield break;

        foreach (var entry in configured.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
            yield return entry;
    }

    /// <summary>
    /// Every reference assembly root already present on the machine: the pre-provisioned
    /// directories and each <c>Microsoft.NETFramework.ReferenceAssemblies.*</c> package in the
    /// NuGet global cache. Nothing is downloaded.
    /// </summary>
    private static IEnumerable<string> AvailableReferenceAssemblyRoots()
    {
        foreach (var configured in PreProvisionedReferenceAssemblyRoots())
            if (Directory.Exists(Path.Combine(configured, ".NETFramework")))
                yield return configured;

        foreach (var cacheRoot in SolutionParser.NuGetCacheRoots)
        {
            if (!Directory.Exists(cacheRoot))
                continue;
            foreach (var packageDir in Directory.EnumerateDirectories(
                         cacheRoot, "microsoft.netframework.referenceassemblies.*"))
            foreach (var installed in Directory.EnumerateDirectories(packageDir))
            {
                var root = Path.Combine(installed, "build");
                if (Directory.Exists(Path.Combine(root, ".NETFramework")))
                    yield return root;
            }
        }
    }
}

/// <summary>
/// Loads .sln or .csproj files via MSBuildWorkspace and parses all user source files
/// in each project with correct references and configuration-derived preprocessor symbols.
/// Generated files (source generator output in obj/) are excluded from the LST —
/// they are only relevant for semantic analysis via the compilation.
/// </summary>
public class SolutionParser
{
    private readonly CSharpParser _parser = new();

    private IReadOnlyDictionary<string, LockFile> _restoredLockFiles =
        new Dictionary<string, LockFile>(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// In-memory NuGet lock files (keyed by absolute project path) from the most recent
    /// <see cref="LoadAsync"/>. Used for MSBuildProject marker attestation without reading
    /// <c>project.assets.json</c> from disk.
    /// </summary>
    public IReadOnlyDictionary<string, LockFile> RestoredLockFiles => _restoredLockFiles;

    /// <summary>
    /// Load a solution or project via MSBuildWorkspace.
    /// Detects .sln/.slnx vs .csproj by extension and calls the appropriate method.
    /// Runs the in-process NuGet restore first so packages are resolved and the standard
    /// restore outputs exist for compilation.
    /// </summary>
    public async Task<Solution> LoadAsync(string path, CancellationToken ct = default)
    {
        Log.Debug("LoadAsync: starting for {Path}", path);

        // Legacy non-SDK projects use packages.config: they need the solution-local packages/
        // folder materialized and get their attestation graph from a synthesized PackageSpec.
        // A solution can mix SDK-style and non-SDK projects, so both paths may run.
        var hasPackagesConfig = HasPackagesConfig(path);

        // MSBuild properties handed to MSBuildWorkspace (and restore-graph evaluation). They
        // point MSBuild at the .NET Framework reference assemblies and web-application targets
        // that are not present on non-Windows machines. Every .NET Framework target version in
        // the tree is provisioned, whether it is declared by a classic project
        // (TargetFrameworkVersion) or an SDK-style one (a net4x/net3x/net2x TargetFramework):
        // without its own reference assemblies a project resolves no references at all, not
        // even mscorlib, and its sources are parsed without type attestation.
        var msbuildProperties = new Dictionary<string, string>();
        var frameworkVersions = DetectNetFrameworkVersions(path);

        if (hasPackagesConfig || frameworkVersions.Count > 0)
        {
            var buildAssets = await SolutionRestore.RestoreNetFrameworkBuildAssetsAsync(frameworkVersions, ct);
            if (buildAssets.VSToolsPath != null)
                msbuildProperties["VSToolsPath"] = buildAssets.VSToolsPath;
            if (buildAssets.ReferenceAssemblyRoots.Count > 0)
            {
                // TargetFrameworkRootPath holds one root; the fallback search paths cover the
                // rest, so a solution mixing target framework versions resolves all of them.
                msbuildProperties["TargetFrameworkRootPath"] = buildAssets.ReferenceAssemblyRoots[0];
                msbuildProperties["TargetFrameworkFallbackSearchPaths"] =
                    string.Join(";", buildAssets.ReferenceAssemblyRoots);
            }
            if (buildAssets.MissingVersions.Count > 0)
                Log.Warning(
                    "Reference assemblies for .NETFramework {Versions} are unavailable, so those projects " +
                    "are parsed without type attestation. Make the " +
                    "Microsoft.NETFramework.ReferenceAssemblies.* packages restorable, or point {EnvVar} " +
                    "at a directory containing .NETFramework/<version> reference assemblies.",
                    string.Join(", ", buildAssets.MissingVersions),
                    SolutionRestore.ReferenceAssembliesEnvironmentVariable);
        }

        _restoredLockFiles = await SolutionRestore.RunAsync(path, hasPackagesConfig, msbuildProperties, ct);

        var sw = Stopwatch.StartNew();
        Log.Debug("MSBuildWorkspace: creating workspace");
        var workspace = msbuildProperties.Count > 0
            ? MSBuildWorkspace.Create(msbuildProperties)
            : MSBuildWorkspace.Create();
        var progress = new Progress<ProjectLoadProgress>(p =>
        {
            Log.Debug("MSBuild progress: {Operation} {FilePath}", p.Operation, Path.GetFileName(p.FilePath));
        });

        Solution solution;
        Log.Debug(">> MSBuildWorkspace.Open ({FileName})", Path.GetFileName(path));
        if (path.EndsWith(".sln", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".slnx", StringComparison.OrdinalIgnoreCase))
            solution = await workspace.OpenSolutionAsync(path, progress, cancellationToken: ct);
        else
            solution = (await workspace.OpenProjectAsync(path, progress, cancellationToken: ct)).Solution;
        Log.Debug("<< MSBuildWorkspace.Open ({FileName}) ({Elapsed})", Path.GetFileName(path), sw.Elapsed);

        // Report any workspace diagnostics
        var diags = workspace.Diagnostics;
        if (diags.Count > 0)
        {
            Log.Debug("MSBuildWorkspace: {DiagCount} diagnostics", diags.Count);
            foreach (var d in diags.Take(10))
                Log.Debug("  MSBuild diagnostic {Kind}: {Message}", d.Kind, d.Message);
            if (diags.Count > 10)
                Log.Debug("  ... and {Remaining} more diagnostics", diags.Count - 10);
        }

        var projectCount = solution.Projects.Count();
        var docCount = solution.Projects.Sum(p => p.Documents.Count());
        Log.Debug("LoadAsync: loaded {ProjectCount} projects, {DocCount} documents", projectCount, docCount);
        return solution;
    }

    /// <summary>
    /// Parse all user source files in a project from a loaded solution.
    /// Uses solution configurations to determine preprocessor symbol permutations.
    /// Generated files (in obj/) are excluded from results — they contribute to
    /// semantic analysis via the compilation but are not included in the LST.
    /// Per-file parse failures are returned as <see cref="ParseError"/> entries
    /// rather than aborting the entire project. When <paramref name="requirePrintEqualsInput"/>
    /// is true, each successfully parsed file is printed and compared against the original
    /// source; mismatches are also returned as <see cref="ParseError"/>.
    /// </summary>
    // Files larger than this are recorded as Quarks rather than parsed into a
    // Roslyn tree/LST. Matches the 1 MB cap in the other RPC engines.
    private const long MaxParseableSizeBytes = 1024 * 1024;

    // Relative paths of source files skipped by the most recent ParseProject call
    // because they exceed MaxParseableSizeBytes; the RPC layer emits these as Quarks.
    public readonly List<string> LastOversizePaths = new();

    public List<SourceFile> ParseProject(
        Solution solution, string projectPath, string rootDir,
        bool requirePrintEqualsInput = true)
    {
        LastOversizePaths.Clear();
        var projectName = Path.GetFileNameWithoutExtension(projectPath);
        Log.Debug("ParseProject: starting {ProjectName}", projectName);

        var project = solution.Projects.FirstOrDefault(p =>
            string.Equals(p.FilePath, projectPath, StringComparison.OrdinalIgnoreCase));

        if (project == null)
            throw new ArgumentException($"Project not found in solution: {projectPath}");

        Compilation? compilation;
        var compileSw = Stopwatch.StartNew();
        Log.Debug(">> GetCompilation ({ProjectName})", projectName);
        compilation = project.GetCompilationAsync().Result;
        compileSw.Stop();
        Log.Debug("<< GetCompilation ({ProjectName}) ({Elapsed})", projectName, compileSw.Elapsed);

        // Get preprocessor symbols from all solution configurations
        var configSymbolSets = GetConfigurationSymbolSets(solution, projectPath);

        var userDocs = project.Documents
            .Where(d => d.FilePath != null && IsUserSource(d, project))
            .ToList();

        // Filter out git-ignored files when inside a git repository
        var ignoredPaths = GetGitIgnoredPaths(rootDir, userDocs.Select(d => d.FilePath!));
        if (ignoredPaths.Count > 0)
        {
            var before = userDocs.Count;
            userDocs = userDocs.Where(d => !ignoredPaths.Contains(d.FilePath!)).ToList();
            Log.Debug("ParseProject: excluded {ExcludedCount} git-ignored files", before - userDocs.Count);
        }
        Log.Debug("ParseProject: {ProjectName} has {UserDocCount} user source files (of {TotalDocCount} total)",
            projectName, userDocs.Count, project.Documents.Count());

        // Create an EditorConfigResolver to detect formatting style from .editorconfig files.
        // The resolver caches results per directory, so files in the same directory share
        // the same CSharpFormatStyle marker instance.
        var editorConfigResolver = new EditorConfigResolver(rootDir);

        var dotNetProject = CreateDotNetProjectMarker(projectPath, projectName);

        // One shared symbol→JavaType cache for the whole project. Roslyn interns symbols
        // per Compilation, so every document in this project resolves a given type to the
        // same JavaType instance — letting the RPC layer (asRef) serialize each type once
        // per project instead of re-serializing it for every referencing file.
        var projectTypeCache = new Dictionary<ISymbol, OpenRewrite.Java.JavaType>(SymbolEqualityComparer.Default);

        var results = new List<SourceFile>();
        var fileIndex = 0;
        var projectSw = Stopwatch.StartNew();
        foreach (var doc in userDocs)
        {
            fileIndex++;

            // Files too large to parse into a Roslyn tree/LST are recorded as Quarks
            // (path only) by the RPC layer; skip the expensive parse entirely.
            long docSize;
            try { docSize = new FileInfo(doc.FilePath!).Length; } catch { docSize = 0; }
            if (docSize > MaxParseableSizeBytes)
            {
                LastOversizePaths.Add(Path.GetRelativePath(rootDir, doc.FilePath!).Replace('\\', '/'));
                continue;
            }

            var source = doc.GetTextAsync().Result?.ToString();
            if (source == null) continue;

            var relativePath = Path.GetRelativePath(rootDir, doc.FilePath!);
            // Normalize path separators to forward slashes for cross-platform consistency
            relativePath = relativePath.Replace('\\', '/');

            // Detect UTF-8 BOM — Roslyn's SourceText.ToString() strips the BOM character,
            // so we check the raw file bytes to preserve the flag for patch fidelity.
            var charsetBomMarked = HasUtf8Bom(doc.FilePath!);

            var fileSw = Stopwatch.StartNew();
            try
            {
                SemanticModel? semanticModel = null;
                if (compilation != null)
                {
                    var syntaxTree = doc.GetSyntaxTreeAsync().Result;
                    if (syntaxTree != null)
                        semanticModel = compilation.GetSemanticModel(syntaxTree);
                }

                CompilationUnit cu;
                if (configSymbolSets.Count > 1)
                {
                    cu = _parser.ParseWithConfigurations(source, relativePath, semanticModel, configSymbolSets,
                        charsetBomMarked, projectTypeCache);
                }
                else
                {
                    cu = _parser.Parse(source, relativePath, semanticModel, charsetBomMarked, projectTypeCache);
                }

                // Attach formatting style marker from .editorconfig
                var formatStyle = editorConfigResolver.Resolve(doc.FilePath!);
                cu = cu.WithMarkers(cu.Markers.Add(formatStyle));

                if (requirePrintEqualsInput)
                {
                    var printed = new CSharpPrinter<int>().Print(cu);
                    if (printed != source)
                    {
                        Log.Debug("  IDEMPOTENCY [{FileIndex}/{TotalFiles}] {RelativePath}",
                            fileIndex, userDocs.Count, relativePath);
                        var diff = DiffUtils.UnifiedDiff(source, printed, relativePath);
                        results.Add(ParseError.Build(relativePath, source,
                            new InvalidOperationException(relativePath + " is not print idempotent. \n" + diff)));
                        fileSw.Stop();
                        continue;
                    }
                }

                cu = cu.WithMarkers(cu.Markers.Add(dotNetProject));
                results.Add(cu);
                fileSw.Stop();

                // Log every file with duration — slow files (>1s) get a warning prefix
                var prefix = fileSw.Elapsed.TotalSeconds > 1.0 ? "SLOW " : "";
                Log.Debug("  {Prefix}[{FileIndex}/{TotalFiles}] {RelativePath} ({ElapsedMs}ms)",
                    prefix, fileIndex, userDocs.Count, relativePath, fileSw.Elapsed.TotalMilliseconds.ToString("F0"));
            }
            catch (Exception ex)
            {
                fileSw.Stop();
                Log.Debug("  ERROR [{FileIndex}/{TotalFiles}] {RelativePath} ({ElapsedMs}ms): {ExType}: {ExMessage}",
                    fileIndex, userDocs.Count, relativePath, fileSw.Elapsed.TotalMilliseconds.ToString("F0"),
                    ex.GetType().Name, ex.Message);
                results.Add(ParseError.Build(relativePath, source, ex));
            }
        }

        projectSw.Stop();
        Log.Debug("ParseProject: {ProjectName} completed {ResultCount} files in {ElapsedSec}s",
            projectName, results.Count, projectSw.Elapsed.TotalSeconds.ToString("F1"));
        return results;
    }

    /// <summary>
    /// Checks whether a file starts with a UTF-8 BOM (byte order mark: EF BB BF).
    /// </summary>
    private static bool HasUtf8Bom(string filePath)
    {
        try
        {
            using var stream = File.OpenRead(filePath);
            Span<byte> buf = stackalloc byte[3];
            return stream.Read(buf) == 3
                   && buf[0] == 0xEF && buf[1] == 0xBB && buf[2] == 0xBF;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Extract unique preprocessor symbol sets from solution configurations.
    /// For each configuration (Debug, Release, etc.), gets the preprocessor symbols
    /// defined for the project under that configuration.
    /// </summary>
    private static List<HashSet<string>> GetConfigurationSymbolSets(Solution solution, string projectPath)
    {
        var symbolSets = new List<HashSet<string>>();
        var seen = new HashSet<string>(); // dedup by joining symbols

        foreach (var project in solution.Projects.Where(p =>
                     string.Equals(p.FilePath, projectPath, StringComparison.OrdinalIgnoreCase)))
        {
            if (project.ParseOptions is Microsoft.CodeAnalysis.CSharp.CSharpParseOptions parseOptions)
            {
                var symbols = new HashSet<string>(parseOptions.PreprocessorSymbolNames);
                var key = string.Join(",", symbols.OrderBy(s => s));
                if (seen.Add(key))
                {
                    symbolSets.Add(symbols);
                }
            }
        }

        // If we couldn't extract any symbol sets, return a single empty set
        if (symbolSets.Count == 0)
        {
            symbolSets.Add(new HashSet<string>());
        }

        return symbolSets;
    }

    /// <summary>
    /// Returns the set of file paths (from <paramref name="candidatePaths"/>) that are
    /// git-ignored according to the repository rooted at or above <paramref name="rootDir"/>.
    /// Returns an empty set when <paramref name="rootDir"/> is not inside a git repository.
    /// </summary>
    private static HashSet<string> GetGitIgnoredPaths(string rootDir, IEnumerable<string> candidatePaths)
    {
        var ignored = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var paths = candidatePaths.ToList();
        if (paths.Count == 0) return ignored;

        try
        {
            var workDir = GitCli.DiscoverWorkTree(rootDir);
            if (workDir == null) return ignored; // Not inside a git repository.
            workDir = PathUtil.Canonicalize(workDir);

            // Evaluate ignore rules against repo-relative paths (forward slashes, as git
            // expects), but report the original candidate string so the caller's membership
            // check matches verbatim. `git check-ignore` is index-aware by default, so a
            // tracked file is never reported as ignored even when a rule would match it.
            var relToOriginal = new Dictionary<string, string>(StringComparer.Ordinal);
            foreach (var path in paths)
            {
                var rel = Path.GetRelativePath(workDir, PathUtil.Canonicalize(path));
                if (rel.StartsWith("..", StringComparison.Ordinal) || Path.IsPathRooted(rel))
                    continue; // Outside the working tree — not subject to its ignore rules.
                rel = rel.Replace('\\', '/');
                relToOriginal[rel] = path;
            }

            if (relToOriginal.Count == 0) return ignored;

            foreach (var rel in GitCli.CheckIgnored(workDir, relToOriginal.Keys))
            {
                if (relToOriginal.TryGetValue(rel, out var original))
                    ignored.Add(original);
            }
        }
        catch (Exception ex)
        {
            Log.Debug("GetGitIgnoredPaths: failed ({ExType}: {ExMessage}), skipping filter",
                ex.GetType().Name, ex.Message);
        }

        return ignored;
    }

    /// <summary>
    /// Determines if a document is a user source file.
    /// Excludes bin/ and obj/ directories entirely.
    /// </summary>
    private static bool IsUserSource(Document doc, Project project)
    {
        if (doc.FilePath == null) return false;

        var projectDir = Path.GetDirectoryName(project.FilePath);
        if (projectDir == null) return true;

        var relativePath = Path.GetRelativePath(projectDir, doc.FilePath);

        // Skip bin/ directory
        if (relativePath.StartsWith("bin" + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase) ||
            relativePath.StartsWith("bin/", StringComparison.OrdinalIgnoreCase))
            return false;

        // Skip obj/ directory entirely — generated files are not included in LST
        if (relativePath.StartsWith("obj" + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase) ||
            relativePath.StartsWith("obj/", StringComparison.OrdinalIgnoreCase))
            return false;

        // Skip source files supplied by NuGet packages. Source-only packages (e.g. *.sources,
        // and any package shipping contentFiles/cs/**) inject .cs files from the global package
        // cache into the compilation. That is third-party code living outside the repository, so
        // it must not be parsed into the LST, transformed by recipes, or emitted into fix patches
        // (which would otherwise target unwritable cache paths and fail to apply).
        if (IsUnderNuGetCache(doc.FilePath))
            return false;

        return true;
    }

    internal static readonly string[] NuGetCacheRoots = BuildNuGetCacheRoots();

    private static string[] BuildNuGetCacheRoots()
    {
        var roots = new List<string>();
        var configured = Environment.GetEnvironmentVariable("NUGET_PACKAGES");
        if (!string.IsNullOrEmpty(configured))
        {
            try { roots.Add(Path.TrimEndingDirectorySeparator(Path.GetFullPath(configured))); }
            catch { /* ignore malformed NUGET_PACKAGES */ }
        }
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        if (!string.IsNullOrEmpty(home))
            roots.Add(Path.Combine(home, ".nuget", "packages"));
        return roots.ToArray();
    }

    /// <summary>
    /// True when the file lives under the NuGet global package cache (so it is package-provided
    /// source, not repository source). Matches only the resolved cache roots (NUGET_PACKAGES env
    /// and the per-user default <c>~/.nuget/packages</c>). A repository may legitimately contain a
    /// local <c>.nuget/packages</c> directory of its own source, so a bare path-segment match is
    /// intentionally avoided.
    /// </summary>
    private static bool IsUnderNuGetCache(string filePath)
    {
        string full;
        try { full = Path.GetFullPath(filePath); }
        catch { return false; }

        foreach (var root in NuGetCacheRoots)
        {
            if (full.StartsWith(root + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase))
                return true;
        }

        return false;
    }

    /// <summary>
    /// Creates a DotNetProject marker by reading TFM and SDK from the .csproj XML.
    /// </summary>
    private static DotNetProject CreateDotNetProjectMarker(string projectPath, string projectName)
    {
        string? sdk = null;
        var tfms = new List<string>();

        try
        {
            var doc = XDocument.Load(projectPath);
            var root = doc.Root;
            if (root != null)
            {
                sdk = root.Attribute("Sdk")?.Value;

                var tfm = root.Elements("PropertyGroup").Elements("TargetFramework").FirstOrDefault()?.Value;
                if (tfm != null)
                    tfms.Add(tfm);

                var tfmList = root.Elements("PropertyGroup").Elements("TargetFrameworks").FirstOrDefault()?.Value;
                if (tfmList != null)
                {
                    foreach (var t in tfmList.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                        if (!tfms.Contains(t))
                            tfms.Add(t);
                }
            }
        }
        catch (Exception ex)
        {
            Log.Debug("Failed to read project metadata from {ProjectPath}: {Error}", projectPath, ex.Message);
        }

        return new DotNetProject(Tree.RandomId(), projectName, tfms, sdk);
    }

    /// <summary>
    /// The MSBuild <c>TargetFrameworkVersion</c> values (highest first) that projects in the
    /// solution/project directory tree target — classic projects declaring
    /// <c>TargetFrameworkVersion</c> and SDK-style ones declaring a .NET Framework
    /// <c>TargetFramework(s)</c> moniker alike. Empty when nothing targets .NET Framework.
    /// </summary>
    internal static IReadOnlyList<string> DetectNetFrameworkVersions(string path)
    {
        var versions = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        try
        {
            var dir = Path.GetDirectoryName(Path.GetFullPath(path));
            if (dir == null)
                return Array.Empty<string>();
            foreach (var projectFile in Directory.EnumerateFiles(dir, "*.*proj", SearchOption.AllDirectories))
            {
                var frameworks = NuGetResolver.ReadTargetFrameworks(projectFile);
                foreach (var framework in frameworks)
                {
                    if (framework.Framework == FrameworkConstants.FrameworkIdentifiers.Net)
                        versions.Add(TargetFrameworkVersionOf(framework));
                }

                // MSBuild defaults a classic project that declares no version to v4.0.
                if (frameworks.Count == 0 && IsClassicProject(projectFile))
                    versions.Add("v4.0");
            }
        }
        catch (Exception ex)
        {
            Log.Debug("DetectNetFrameworkVersions: failed for {Path} ({ExType}: {ExMessage}), assuming none",
                path, ex.GetType().Name, ex.Message);
        }

        return versions.OrderByDescending(v => Version.Parse(v[1..])).ToList();
    }

    /// <summary>
    /// The MSBuild <c>TargetFrameworkVersion</c> spelling of a framework: <c>v4.7.2</c> rather
    /// than the <c>4.7.2.0</c> a parsed moniker carries.
    /// </summary>
    private static string TargetFrameworkVersionOf(NuGetFramework framework)
    {
        var version = framework.Version;
        var fieldCount = version.Revision > 0 ? 4 : version.Build > 0 ? 3 : 2;
        return "v" + version.ToString(fieldCount);
    }

    private static bool IsClassicProject(string projectFile)
    {
        try
        {
            return XDocument.Load(projectFile).Root?.Attribute("Sdk") == null;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Returns true if the solution/project directory tree contains a <c>packages.config</c>,
    /// which marks a legacy non-SDK-style project whose NuGet dependencies are materialized
    /// into the solution-local packages folder and attested via a synthesized restore graph.
    /// </summary>
    private static bool HasPackagesConfig(string path)
    {
        try
        {
            var dir = Path.GetDirectoryName(Path.GetFullPath(path));
            if (dir == null)
                return false;
            return Directory.EnumerateFiles(dir, "packages.config", SearchOption.AllDirectories).Any();
        }
        catch (Exception ex)
        {
            Log.Debug("HasPackagesConfig: failed for {Path} ({ExType}: {ExMessage}), assuming none",
                path, ex.GetType().Name, ex.Message);
            return false;
        }
    }
}
