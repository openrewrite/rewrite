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
    /// targets import and <c>TargetFrameworkRootPath</c> resolves the reference assemblies.
    /// </summary>
    private const string WebTargetsPackage = "MSBuild.Microsoft.VisualStudio.Web_WebApplication.Targets";
    private const string WebTargetsVersion = "12.0.2";
    private const string ReferenceAssembliesPackage = "Microsoft.NETFramework.ReferenceAssemblies.net48";
    private const string ReferenceAssembliesVersion = "1.0.3";

    private static NetFrameworkBuildAssets? _buildAssets;

    /// <summary>
    /// MSBuild property values pointing at restored .NET Framework build assets. A value is
    /// null when the corresponding package could not be restored.
    /// </summary>
    internal record NetFrameworkBuildAssets(string? VSToolsPath, string? TargetFrameworkRootPath);

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
    /// Restores the .NET Framework reference assemblies and web-application targets as NuGet
    /// packages into a stable per-machine cache directory (flat, version-less layout), so
    /// MSBuildWorkspace can evaluate legacy projects on non-Windows machines. The result is
    /// cached for the process lifetime.
    /// </summary>
    public static async Task<NetFrameworkBuildAssets> RestoreNetFrameworkBuildAssetsAsync(CancellationToken ct)
    {
        if (_buildAssets != null)
            return _buildAssets;

        await Gate.WaitAsync(ct);
        try
        {
            if (_buildAssets != null)
                return _buildAssets;

            var cacheDir = Path.Combine(Path.GetTempPath(), "openrewrite-netfx-build-assets");
            var vsToolsPath = Path.Combine(cacheDir, WebTargetsPackage, "tools", "VSToolsPath");
            var targetFrameworkRootPath = Path.Combine(cacheDir, ReferenceAssembliesPackage, "build");

            if (!Directory.Exists(vsToolsPath))
                await NuGetResolver.InstallPackageAsync(
                    WebTargetsPackage, WebTargetsVersion, cacheDir, excludeVersion: true, ct);
            if (!Directory.Exists(targetFrameworkRootPath))
                await NuGetResolver.InstallPackageAsync(
                    ReferenceAssembliesPackage, ReferenceAssembliesVersion, cacheDir, excludeVersion: true, ct);

            _buildAssets = new NetFrameworkBuildAssets(
                Directory.Exists(vsToolsPath) ? vsToolsPath : null,
                Directory.Exists(targetFrameworkRootPath) ? targetFrameworkRootPath : null);
            Log.Debug("netfx build assets — VSToolsPath={VSToolsPath}, TargetFrameworkRootPath={TargetFrameworkRootPath}",
                _buildAssets.VSToolsPath ?? "(missing)", _buildAssets.TargetFrameworkRootPath ?? "(missing)");
            return _buildAssets;
        }
        finally
        {
            Gate.Release();
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

        // MSBuild properties handed to MSBuildWorkspace (and restore-graph evaluation). For
        // legacy (non-SDK) projects these point MSBuild at the .NET Framework reference
        // assemblies and web-application targets that are not present on non-Windows machines.
        // Gated on non-SDK project presence (not packages.config): a converted project that
        // uses PackageReference in a classic csproj still needs them to evaluate/compile.
        var msbuildProperties = new Dictionary<string, string>();

        if (hasPackagesConfig || HasNonSdkProject(path))
        {
            var buildAssets = await SolutionRestore.RestoreNetFrameworkBuildAssetsAsync(ct);
            if (buildAssets.VSToolsPath != null)
                msbuildProperties["VSToolsPath"] = buildAssets.VSToolsPath;
            if (buildAssets.TargetFrameworkRootPath != null)
                msbuildProperties["TargetFrameworkRootPath"] = buildAssets.TargetFrameworkRootPath;
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
    public List<SourceFile> ParseProject(
        Solution solution, string projectPath, string rootDir,
        bool requirePrintEqualsInput = true)
    {
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

    private static readonly string[] NuGetCacheRoots = BuildNuGetCacheRoots();

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

        return new DotNetProject(Guid.NewGuid(), projectName, tfms, sdk);
    }

    /// <summary>
    /// Returns true if the solution/project directory tree contains a classic (non-SDK-style)
    /// project file — one whose root Project element has no Sdk attribute. Such projects need
    /// the .NET Framework build assets to evaluate on non-Windows machines, whether or not
    /// they still use packages.config.
    /// </summary>
    private static bool HasNonSdkProject(string path)
    {
        try
        {
            var dir = Path.GetDirectoryName(Path.GetFullPath(path));
            if (dir == null)
                return false;
            foreach (var projectFile in Directory.EnumerateFiles(dir, "*.csproj", SearchOption.AllDirectories))
            {
                try
                {
                    var root = XDocument.Load(projectFile).Root;
                    if (root != null && root.Attribute("Sdk") == null)
                        return true;
                }
                catch
                {
                    // Unparseable project file — ignore.
                }
            }
        }
        catch (Exception ex)
        {
            Log.Debug("HasNonSdkProject: failed for {Path} ({ExType}: {ExMessage}), assuming none",
                path, ex.GetType().Name, ex.Message);
        }
        return false;
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
