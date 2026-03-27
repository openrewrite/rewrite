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
using Microsoft.Build.Locator;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.MSBuild;
using OpenRewrite.Core;
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
            throw new InvalidOperationException(
                $"dotnet restore failed (exit code {restoreResult.ExitCode}) for {path}\n{details}");
        }

        Log.Debug("dotnet restore succeeded in {Elapsed}", restoreResult.Elapsed);

        var sw = Stopwatch.StartNew();
        Log.Debug("MSBuildWorkspace: creating workspace");
        var workspace = MSBuildWorkspace.Create();
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
                        charsetBomMarked);
                }
                else
                {
                    cu = _parser.Parse(source, relativePath, semanticModel, charsetBomMarked);
                }

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
    /// Returns an empty set when git is not available or <paramref name="rootDir"/> is not
    /// inside a git repository.
    /// </summary>
    private static HashSet<string> GetGitIgnoredPaths(string rootDir, IEnumerable<string> candidatePaths)
    {
        var ignored = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var paths = candidatePaths.ToList();
        if (paths.Count == 0) return ignored;

        try
        {
            // Check if rootDir is inside a git repo
            var checkPsi = new ProcessStartInfo("git", "rev-parse --git-dir")
            {
                WorkingDirectory = rootDir,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            using var checkProcess = Process.Start(checkPsi);
            if (checkProcess == null) return ignored;
            checkProcess.WaitForExit(5_000);
            if (checkProcess.ExitCode != 0) return ignored;

            // Use git check-ignore --stdin to batch-check all candidate paths
            var psi = new ProcessStartInfo("git", "check-ignore --stdin")
            {
                WorkingDirectory = rootDir,
                RedirectStandardInput = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            using var process = Process.Start(psi);
            if (process == null) return ignored;

            // Write all paths to stdin, one per line
            foreach (var path in paths)
                process.StandardInput.WriteLine(path);
            process.StandardInput.Close();

            // Read ignored paths from stdout
            var output = process.StandardOutput.ReadToEnd();
            process.WaitForExit(10_000);

            foreach (var line in output.Split('\n', StringSplitOptions.RemoveEmptyEntries))
            {
                var trimmed = line.TrimEnd('\r');
                if (!string.IsNullOrEmpty(trimmed))
                {
                    // git check-ignore outputs paths relative to the working directory;
                    // resolve them to full paths for comparison
                    var fullPath = Path.GetFullPath(trimmed, rootDir);
                    ignored.Add(fullPath);
                }
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

    private static void EnsureMSBuildRegistered()
    {
        if (!_msbuildRegistered)
        {
            MSBuildLocator.RegisterDefaults();
            _msbuildRegistered = true;
        }
    }
}
