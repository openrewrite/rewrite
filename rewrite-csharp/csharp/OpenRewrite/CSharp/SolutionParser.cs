using System.Diagnostics;
using Microsoft.Build.Locator;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.MSBuild;

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
    private static readonly TimeSpan Timeout = TimeSpan.FromMinutes(2);

    public static async Task RunAsync(string path, CancellationToken ct)
    {
        var key = Path.GetFullPath(path);

        // Fast path: already restored in this process
        lock (Restored)
        {
            if (Restored.Contains(key)) return;
        }

        await Gate.WaitAsync(ct);
        try
        {
            // Double-check after acquiring the gate
            lock (Restored)
            {
                if (Restored.Contains(key)) return;
            }

            await RunCoreAsync(path, ct);

            lock (Restored)
            {
                Restored.Add(key);
            }
        }
        finally
        {
            Gate.Release();
        }
    }

    private static async Task RunCoreAsync(string path, CancellationToken ct)
    {
        var psi = new ProcessStartInfo("dotnet", $"restore \"{path}\"")
        {
            WorkingDirectory = Path.GetDirectoryName(path) ?? ".",
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        using var process = Process.Start(psi);
        if (process == null) return;

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
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested)
        {
            // Restore timed out but overall operation is still running — kill and continue
            try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
            Console.WriteLine("  dotnet restore timed out after 2 minutes, continuing without full restore");
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
        EnsureMSBuildRegistered();

        // Run dotnet restore to ensure NuGet packages are available for MSBuildWorkspace
        var sw = Stopwatch.StartNew();
        await DotNetRestore.RunAsync(path, ct);
        Console.WriteLine($"  dotnet restore completed in {sw.Elapsed}");

        sw.Restart();
        var workspace = MSBuildWorkspace.Create();
        var logFile = Path.Combine(Path.GetTempPath(), $"msbuild_load_progress_{Environment.ProcessId}_{Thread.CurrentThread.ManagedThreadId}.log");
        var progress = new Progress<ProjectLoadProgress>(p =>
            File.AppendAllText(logFile, $"[{sw.Elapsed:mm\\:ss}] {p.Operation}: {Path.GetFileName(p.FilePath)}\n"));
        File.WriteAllText(logFile, $"Loading: {path}\n");
        Solution solution;
        if (path.EndsWith(".sln", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".slnx", StringComparison.OrdinalIgnoreCase))
            solution = await workspace.OpenSolutionAsync(path, progress, cancellationToken: ct);
        else
            solution = (await workspace.OpenProjectAsync(path, progress, cancellationToken: ct)).Solution;
        Console.WriteLine($"  MSBuildWorkspace.Open completed in {sw.Elapsed}");

        // Report any workspace diagnostics
        var diags = workspace.Diagnostics;
        if (diags.Count > 0)
        {
            Console.WriteLine($"  Workspace diagnostics ({diags.Count}):");
            foreach (var d in diags.Take(5))
                Console.WriteLine($"    {d.Kind}: {d.Message}");
            if (diags.Count > 5)
                Console.WriteLine($"    ... and {diags.Count - 5} more");
        }

        return solution;
    }

    /// <summary>
    /// Parse all user source files in a project from a loaded solution.
    /// Uses solution configurations to determine preprocessor symbol permutations.
    /// Generated files (in obj/) are excluded from results — they contribute to
    /// semantic analysis via the compilation but are not included in the LST.
    /// </summary>
    public List<CompilationUnit> ParseProject(
        Solution solution, string projectPath, string rootDir)
    {
        var project = solution.Projects.FirstOrDefault(p =>
            string.Equals(p.FilePath, projectPath, StringComparison.OrdinalIgnoreCase));

        if (project == null)
            throw new ArgumentException($"Project not found in solution: {projectPath}");

        var compilation = project.GetCompilationAsync().Result;

        // Get preprocessor symbols from all solution configurations
        var configSymbolSets = GetConfigurationSymbolSets(solution, projectPath);

        var results = new List<CompilationUnit>();
        foreach (var doc in project.Documents)
        {
            if (doc.FilePath == null) continue;
            if (!IsUserSource(doc, project)) continue;

            var source = doc.GetTextAsync().Result?.ToString();
            if (source == null) continue;

            var relativePath = Path.GetRelativePath(rootDir, doc.FilePath);
            // Normalize path separators to forward slashes for cross-platform consistency
            relativePath = relativePath.Replace('\\', '/');

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
                cu = _parser.ParseWithConfigurations(source, relativePath, semanticModel, configSymbolSets);
            }
            else
            {
                cu = _parser.Parse(source, relativePath, semanticModel);
            }

            results.Add(cu);
        }

        return results;
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
