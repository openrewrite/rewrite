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
using LibGit2Sharp;
using Microsoft.Build.Locator;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.MSBuild;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Format;
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Serializes <c>dotnet restore</c> invocations so that only one runs at a time,
/// preventing process explosions when multiple tests load solutions concurrently.
/// Tracks which paths have already been restored to avoid redundant work.
/// </summary>
internal static class DotNetRestore
{
    private static readonly SemaphoreSlim Gate = new(1, 1);
    private static readonly HashSet<string> Restored = new(StringComparer.OrdinalIgnoreCase);
    private static readonly TimeSpan Timeout = TimeSpan.FromMinutes(10);

    internal record RestoreResult(int ExitCode, string Stdout, string Stderr, TimeSpan Elapsed, bool TimedOut);

    public static async Task<RestoreResult> RunAsync(string path, CancellationToken ct)
    {
        var key = Path.GetFullPath(path);

        // Fast path: already restored in this process
        lock (Restored)
        {
            if (Restored.Contains(key))
            {
                Log.Debug("dotnet restore: skipped (already restored) {Path}", path);
                return new RestoreResult(0, "", "", TimeSpan.Zero, false);
            }
        }

        Log.Debug("dotnet restore: waiting for gate {Path}", path);
        await Gate.WaitAsync(ct);
        try
        {
            // Double-check after acquiring the gate
            lock (Restored)
            {
                if (Restored.Contains(key))
                    return new RestoreResult(0, "", "", TimeSpan.Zero, false);
            }

            var result = await RunCoreAsync(path, ct);

            if (result.ExitCode == 0 && !result.TimedOut)
            {
                lock (Restored)
                {
                    Restored.Add(key);
                }
            }

            return result;
        }
        finally
        {
            Gate.Release();
        }
    }

    /// <summary>
    /// Replacement NuGet feeds for defunct dotnet.myget.org sources.
    /// MyGet was shut down; packages migrated to Azure DevOps Artifacts (dnceng).
    /// Semicolons are escaped as %3B because MSBuild /p: treats literal ';' as a property separator.
    /// </summary>
    private static readonly string AdditionalNuGetSources = string.Join("%3B",
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/dotnet-public/nuget/v3/index.json",
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/dotnet-tools/nuget/v3/index.json",
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/myget-legacy/nuget/v3/index.json");

    private static async Task<RestoreResult> RunCoreAsync(string path, CancellationToken ct)
    {
        Log.Debug("dotnet restore: starting for {Path}", path);
        var sw = Stopwatch.StartNew();
        // Relax restore for LST parsing: disable NuGet vulnerability audit
        // (NU1902/NU1903 would fail restore), ignore dead NuGet sources,
        // and add replacement feeds for defunct MyGet sources.
        var psi = new ProcessStartInfo("dotnet")
        {
            WorkingDirectory = Path.GetDirectoryName(path) ?? ".",
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        psi.ArgumentList.Add("restore");
        psi.ArgumentList.Add(path);
        psi.ArgumentList.Add("/p:NuGetAudit=false");
        psi.ArgumentList.Add("/p:RestoreIgnoreFailedSources=true");
        psi.ArgumentList.Add($"/p:RestoreAdditionalProjectSources={AdditionalNuGetSources}");
        psi.ArgumentList.Add("--ignore-failed-sources");
        // Reduce NuGet retry attempts so dead feeds fail fast
        psi.Environment["NUGET_ENHANCED_MAX_NETWORK_TRY_COUNT"] = "1";
        psi.Environment["NUGET_ENHANCED_NETWORK_RETRY_DELAY_MILLISECONDS"] = "100";

        using var process = Process.Start(psi);
        if (process == null)
        {
            Log.Debug("dotnet restore: process failed to start");
            return new RestoreResult(-1, "", "Failed to start dotnet restore process", sw.Elapsed, false);
        }

        Log.Debug("dotnet restore: process started (PID {ProcessId})", process.Id);
        using var restoreCts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        restoreCts.CancelAfter(Timeout);

        try
        {
            // Must read stdout/stderr before WaitForExitAsync to avoid deadlock
            // when the pipe buffer fills up (e.g. many NETSDK1138 warnings for
            // Windows-specific TFMs).
            var stdoutTask = process.StandardOutput.ReadToEndAsync(restoreCts.Token);
            var stderrTask = process.StandardError.ReadToEndAsync(restoreCts.Token);
            await Task.WhenAll(stdoutTask, stderrTask);
            await process.WaitForExitAsync(restoreCts.Token);
            sw.Stop();
            Log.Debug("dotnet restore: completed (exit code {ExitCode})", process.ExitCode);
            return new RestoreResult(process.ExitCode, stdoutTask.Result, stderrTask.Result, sw.Elapsed, false);
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested)
        {
            // Restore timed out but overall operation is still running — kill and continue
            try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
            sw.Stop();
            Log.Debug("dotnet restore: TIMED OUT after {Timeout}, killed process", Timeout);
            return new RestoreResult(-1, "", "", sw.Elapsed, true);
        }
    }
}

/// <summary>
/// Serializes <c>nuget restore</c> invocations (and the one-time restore of the .NET
/// Framework build assets) so only one runs at a time, and caches which paths have
/// already been restored. Unlike <see cref="DotNetRestore"/>, the standalone NuGet CLI
/// can restore legacy <c>packages.config</c> (non-SDK-style) projects, which
/// <c>dotnet restore</c> treats as a no-op. On non-Windows machines the NuGet CLI runs
/// through <c>mono</c>.
/// </summary>
internal static class NuGetRestore
{
    private static readonly SemaphoreSlim Gate = new(1, 1);
    private static readonly HashSet<string> Restored = new(StringComparer.OrdinalIgnoreCase);
    private static readonly TimeSpan Timeout = TimeSpan.FromMinutes(10);

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

    internal record RestoreResult(int ExitCode, string Stdout, string Stderr, TimeSpan Elapsed, bool TimedOut);

    /// <summary>
    /// MSBuild property values pointing at restored .NET Framework build assets. A value is
    /// null when the corresponding package could not be restored.
    /// </summary>
    internal record NetFrameworkBuildAssets(string? VSToolsPath, string? TargetFrameworkRootPath);

    public static async Task<RestoreResult> RunAsync(string path, CancellationToken ct)
    {
        var key = "restore:" + Path.GetFullPath(path);

        lock (Restored)
        {
            if (Restored.Contains(key))
            {
                Log.Debug("nuget restore: skipped (already restored) {Path}", path);
                return new RestoreResult(0, "", "", TimeSpan.Zero, false);
            }
        }

        await Gate.WaitAsync(ct);
        try
        {
            lock (Restored)
            {
                if (Restored.Contains(key))
                    return new RestoreResult(0, "", "", TimeSpan.Zero, false);
            }

            var cli = FindNuGetCli();
            if (cli == null)
            {
                return new RestoreResult(-1, "",
                    "NuGet CLI not found on PATH (need `nuget` on macOS/Linux or `nuget.exe` on Windows)",
                    TimeSpan.Zero, false);
            }

            var args = new List<string>(cli.Value.PrefixArgs) { "restore", path, "-NonInteractive" };
            var workingDir = Path.GetDirectoryName(Path.GetFullPath(path)) ?? ".";
            Log.Debug("nuget restore: starting for {Path}", path);
            var result = await RunProcessAsync(cli.Value.Exe, args, workingDir, ct);

            if (result.ExitCode == 0 && !result.TimedOut)
            {
                lock (Restored)
                {
                    Restored.Add(key);
                }
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
    /// packages into a stable per-machine cache directory, so MSBuildWorkspace can evaluate
    /// legacy projects on non-Windows machines. The result is cached for the process lifetime.
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

            // -ExcludeVersion keeps the installed folder names stable across versions.
            var cacheDir = Path.Combine(Path.GetTempPath(), "openrewrite-netfx-build-assets");
            var vsToolsPath = Path.Combine(cacheDir, WebTargetsPackage, "tools", "VSToolsPath");
            var targetFrameworkRootPath = Path.Combine(cacheDir, ReferenceAssembliesPackage, "build");

            var cli = FindNuGetCli();
            if (cli == null)
            {
                Log.Debug("nuget install: NuGet CLI not found on PATH; skipping .NET Framework build assets");
                _buildAssets = new NetFrameworkBuildAssets(null, null);
                return _buildAssets;
            }

            if (!Directory.Exists(vsToolsPath))
                await NuGetInstallAsync(cli.Value, WebTargetsPackage, WebTargetsVersion, cacheDir, ct);
            if (!Directory.Exists(targetFrameworkRootPath))
                await NuGetInstallAsync(cli.Value, ReferenceAssembliesPackage, ReferenceAssembliesVersion, cacheDir, ct);

            _buildAssets = new NetFrameworkBuildAssets(
                Directory.Exists(vsToolsPath) ? vsToolsPath : null,
                Directory.Exists(targetFrameworkRootPath) ? targetFrameworkRootPath : null);
            Log.Debug("nuget install: .NET Framework build assets — VSToolsPath={VSToolsPath}, TargetFrameworkRootPath={TargetFrameworkRootPath}",
                _buildAssets.VSToolsPath ?? "(missing)", _buildAssets.TargetFrameworkRootPath ?? "(missing)");
            return _buildAssets;
        }
        finally
        {
            Gate.Release();
        }
    }

    private static async Task NuGetInstallAsync((string Exe, string[] PrefixArgs) cli, string package,
        string version, string outputDirectory, CancellationToken ct)
    {
        Directory.CreateDirectory(outputDirectory);
        var args = new List<string>(cli.PrefixArgs)
        {
            "install", package,
            "-Version", version,
            "-OutputDirectory", outputDirectory,
            "-ExcludeVersion",
            "-NonInteractive"
        };
        Log.Debug("nuget install: {Package} {Version} -> {OutputDirectory}", package, version, outputDirectory);
        var result = await RunProcessAsync(cli.Exe, args, outputDirectory, ct);
        if (result.ExitCode != 0)
        {
            Log.Debug("nuget install: {Package} failed (exit code {ExitCode})\n{Stdout}\n{Stderr}",
                package, result.ExitCode, result.Stdout, result.Stderr);
        }
    }

    private static async Task<RestoreResult> RunProcessAsync(string exe, IReadOnlyList<string> args,
        string workingDirectory, CancellationToken ct)
    {
        var sw = Stopwatch.StartNew();
        var psi = new ProcessStartInfo(exe)
        {
            WorkingDirectory = workingDirectory,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        foreach (var a in args)
            psi.ArgumentList.Add(a);

        using var process = Process.Start(psi);
        if (process == null)
        {
            sw.Stop();
            return new RestoreResult(-1, "", $"Failed to start process: {exe}", sw.Elapsed, false);
        }

        using var procCts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        procCts.CancelAfter(Timeout);
        try
        {
            // Read stdout/stderr before WaitForExitAsync to avoid a pipe-buffer deadlock.
            var stdoutTask = process.StandardOutput.ReadToEndAsync(procCts.Token);
            var stderrTask = process.StandardError.ReadToEndAsync(procCts.Token);
            await Task.WhenAll(stdoutTask, stderrTask);
            await process.WaitForExitAsync(procCts.Token);
            sw.Stop();
            return new RestoreResult(process.ExitCode, stdoutTask.Result, stderrTask.Result, sw.Elapsed, false);
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested)
        {
            try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
            sw.Stop();
            return new RestoreResult(-1, "", "", sw.Elapsed, true);
        }
    }

    /// <summary>
    /// Resolves how to invoke the NuGet CLI: <c>nuget.exe</c> on Windows, <c>nuget</c> on
    /// other platforms, falling back to <c>mono nuget.exe</c>. Returns null when no NuGet CLI
    /// is found on the PATH.
    /// </summary>
    private static (string Exe, string[] PrefixArgs)? FindNuGetCli()
    {
        if (OperatingSystem.IsWindows())
        {
            var win = FindOnPath("nuget.exe") ?? FindOnPath("nuget");
            return win == null ? null : (win, Array.Empty<string>());
        }
        var nuget = FindOnPath("nuget");
        if (nuget != null)
            return (nuget, Array.Empty<string>());
        var nugetExe = FindOnPath("nuget.exe");
        if (nugetExe != null)
            return ("mono", new[] { nugetExe });
        return null;
    }

    private static string? FindOnPath(string fileName)
    {
        var pathVar = Environment.GetEnvironmentVariable("PATH");
        if (string.IsNullOrEmpty(pathVar))
            return null;
        foreach (var dir in pathVar.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries))
        {
            try
            {
                var candidate = Path.Combine(dir.Trim(), fileName);
                if (File.Exists(candidate))
                    return candidate;
            }
            catch
            {
                // Skip invalid PATH entries.
            }
        }
        return null;
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
    private static bool _msbuildRegistered;
    private readonly CSharpParser _parser = new();

    /// <summary>
    /// Load a solution or project via MSBuildWorkspace.
    /// Detects .sln/.slnx vs .csproj by extension and calls the appropriate method.
    /// Runs dotnet restore first to ensure NuGet packages are resolved.
    /// </summary>
    public async Task<Solution> LoadAsync(string path, CancellationToken ct = default)
    {
        Log.Debug("LoadAsync: starting for {Path}", path);
        EnsureMSBuildRegistered();

        // Legacy non-SDK projects use packages.config and must be restored with the
        // standalone NuGet CLI; dotnet restore is a no-op for them. A solution can mix
        // SDK-style and non-SDK projects, so when packages.config is present we run both.
        var hasPackagesConfig = HasPackagesConfig(path);

        // Run dotnet restore to ensure NuGet packages are available for MSBuildWorkspace
        Log.Debug(">> dotnet restore ({FileName})", Path.GetFileName(path));
        var restoreResult = await DotNetRestore.RunAsync(path, ct);
        Log.Debug("<< dotnet restore ({FileName}) ({Elapsed})", Path.GetFileName(path), restoreResult.Elapsed);

        if (restoreResult.TimedOut)
        {
            throw new InvalidOperationException(
                $"dotnet restore timed out after {restoreResult.Elapsed} for {path}");
        }

        if (restoreResult.ExitCode != 0)
        {
            var details = string.Join("\n",
                new[] { restoreResult.Stdout, restoreResult.Stderr }
                    .Where(s => !string.IsNullOrWhiteSpace(s)));
            if (hasPackagesConfig)
            {
                // For packages.config projects the authoritative restore is the nuget
                // restore below; a dotnet restore failure here is not fatal.
                Log.Debug("dotnet restore failed (exit code {ExitCode}) for {Path}; continuing with nuget restore\n{Details}",
                    restoreResult.ExitCode, path, details);
            }
            else
            {
                throw new InvalidOperationException(
                    $"dotnet restore failed (exit code {restoreResult.ExitCode}) for {path}\n{details}");
            }
        }
        else
        {
            Log.Debug("dotnet restore succeeded in {Elapsed}", restoreResult.Elapsed);
        }

        // MSBuild properties handed to MSBuildWorkspace. For packages.config projects these
        // point MSBuild at the .NET Framework reference assemblies and web-application
        // targets that are not present on non-Windows machines.
        var msbuildProperties = new Dictionary<string, string>();

        if (hasPackagesConfig)
        {
            Log.Debug(">> nuget restore ({FileName})", Path.GetFileName(path));
            var nugetResult = await NuGetRestore.RunAsync(path, ct);
            Log.Debug("<< nuget restore ({FileName}) ({Elapsed})", Path.GetFileName(path), nugetResult.Elapsed);

            if (nugetResult.TimedOut)
            {
                throw new InvalidOperationException(
                    $"nuget restore timed out after {nugetResult.Elapsed} for {path}");
            }

            if (nugetResult.ExitCode != 0)
            {
                var details = string.Join("\n",
                    new[] { nugetResult.Stdout, nugetResult.Stderr }
                        .Where(s => !string.IsNullOrWhiteSpace(s)));
                throw new InvalidOperationException(
                    $"nuget restore failed (exit code {nugetResult.ExitCode}) for {path}\n{details}");
            }

            // Restore the .NET Framework reference assemblies and web-application targets
            // so MSBuildWorkspace can evaluate legacy projects on non-Windows machines.
            var buildAssets = await NuGetRestore.RestoreNetFrameworkBuildAssetsAsync(ct);
            if (buildAssets.VSToolsPath != null)
                msbuildProperties["VSToolsPath"] = buildAssets.VSToolsPath;
            if (buildAssets.TargetFrameworkRootPath != null)
                msbuildProperties["TargetFrameworkRootPath"] = buildAssets.TargetFrameworkRootPath;
        }

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
            var gitDir = Repository.Discover(rootDir);
            if (gitDir == null) return ignored; // Not inside a git repository.

            using var repo = new Repository(gitDir);
            var workDir = PathUtil.Canonicalize(repo.Info.WorkingDirectory);

            foreach (var path in paths)
            {
                // Evaluate ignore rules against the repo-relative path (forward slashes, as
                // libgit2 expects), but report the original candidate string so the caller's
                // membership check matches verbatim.
                var rel = Path.GetRelativePath(workDir, PathUtil.Canonicalize(path));
                if (rel.StartsWith("..", StringComparison.Ordinal) || Path.IsPathRooted(rel))
                    continue; // Outside the working tree — not subject to its ignore rules.
                rel = rel.Replace('\\', '/');

                // Mirror `git check-ignore`: tracked files are never reported as ignored, even
                // when an ignore rule would otherwise match them.
                if (repo.Index[rel] != null)
                    continue;

                if (repo.Ignore.IsPathIgnored(rel))
                    ignored.Add(path);
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

        return true;
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

    private static void EnsureMSBuildRegistered()
    {
        if (!_msbuildRegistered)
        {
            MSBuildLocator.RegisterDefaults();
            _msbuildRegistered = true;
        }
    }

    /// <summary>
    /// Returns true if the solution/project directory tree contains a <c>packages.config</c>,
    /// which marks a legacy non-SDK-style project whose NuGet dependencies must be restored
    /// with the standalone NuGet CLI rather than <c>dotnet restore</c>.
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
