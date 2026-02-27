using System.Diagnostics;
using Microsoft.Build.Locator;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.MSBuild;

namespace OpenRewrite.Tests;

/// <summary>
/// Diagnostic tests that load the hanging working-set solutions directly via MSBuildWorkspace
/// to determine if the hang is in MSBuild workspace loading or in the OpenRewrite parsing layer.
/// </summary>
public class MsBuildWorkspaceLoadTests
{
    private const string WorkingSetRoot = @"C:\Projects\moderneinc\moderne-cli\working-set-csharp";

    private static bool _msbuildRegistered;

    private static void EnsureMSBuildRegistered()
    {
        if (!_msbuildRegistered)
        {
            MSBuildLocator.RegisterDefaults();
            _msbuildRegistered = true;
        }
    }

    // -- Previously hanging solutions --

    [Fact]
    public async Task LoadILSpy()
    {
        await LoadAndDiagnose(
            Path.Combine(WorkingSetRoot, @"Developer Tool\icsharpcode\ILSpy\ILSpy.sln"),
            TimeSpan.FromMinutes(20));
    }

    [Fact]
    public async Task LoadFlowLauncher()
    {
        await LoadAndDiagnose(
            Path.Combine(WorkingSetRoot, @"Desktop App\Flow-Launcher\Flow.Launcher\Flow.Launcher.sln"),
            TimeSpan.FromMinutes(20));
    }

    [Fact]
    public async Task LoadBulkCrapUninstaller()
    {
        await LoadAndDiagnose(
            Path.Combine(WorkingSetRoot, @"Desktop App\Klocman\Bulk-Crap-Uninstaller\source\BulkCrapUninstaller.sln"),
            TimeSpan.FromMinutes(20));
    }

    // -- Control: solutions that parse fine via RPC --

    [Fact]
    public async Task LoadBitwarden()
    {
        await LoadAndDiagnose(
            Path.Combine(WorkingSetRoot, @"Web API\bitwarden\server\bitwarden-server.sln"),
            TimeSpan.FromMinutes(5));
    }

    [Fact]
    public async Task LoadDapper()
    {
        await LoadAndDiagnose(
            Path.Combine(WorkingSetRoot, @"Library\DapperLib\Dapper\Dapper.sln"),
            TimeSpan.FromMinutes(2));
    }

    /// <summary>
    /// Progress logger that reports which project is being loaded.
    /// </summary>
    private class ConsoleProgressLogger(Stopwatch sw) : IProgress<ProjectLoadProgress>
    {
        public void Report(ProjectLoadProgress value)
        {
            Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}]   Loading project: {value.FilePath} ({value.Operation} - {value.TargetFramework})");
        }
    }

    /// <summary>
    /// Loads a solution via MSBuildWorkspace with diagnostics and a timeout.
    /// Reports workspace diagnostics, project count, and per-project document counts.
    /// </summary>
    private static async Task LoadAndDiagnose(string solutionPath, TimeSpan timeout)
    {
        if (!File.Exists(solutionPath))
        {
            Console.WriteLine($"SKIP: Solution not found: {solutionPath}");
            return;
        }

        EnsureMSBuildRegistered();

        var sw = Stopwatch.StartNew();

        Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Loading: {solutionPath}");

        // Run dotnet restore first (with pipe reading to avoid deadlock)
        Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Running dotnet restore...");
        var restorePsi = new ProcessStartInfo("dotnet", $"restore \"{solutionPath}\"")
        {
            WorkingDirectory = Path.GetDirectoryName(solutionPath) ?? ".",
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        using (var restoreProcess = Process.Start(restorePsi)!)
        {
            var stdoutTask = restoreProcess.StandardOutput.ReadToEndAsync();
            var stderrTask = restoreProcess.StandardError.ReadToEndAsync();
            await Task.WhenAll(stdoutTask, stderrTask);
            await restoreProcess.WaitForExitAsync();
            var stdout = stdoutTask.Result;
            var stderr = stderrTask.Result;
            Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] dotnet restore exited with code {restoreProcess.ExitCode}");
            if (!string.IsNullOrWhiteSpace(stderr))
            {
                Console.WriteLine($"  stderr ({stderr.Length} chars): {stderr[..Math.Min(stderr.Length, 500)]}");
            }
        }

        // Create workspace with diagnostic handler
        Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Creating MSBuildWorkspace...");
        var workspace = MSBuildWorkspace.Create();
        var failureCount = 0;
        workspace.RegisterWorkspaceFailedHandler(args =>
        {
            failureCount++;
            if (failureCount <= 20)
                Console.WriteLine($"  [WorkspaceFailed #{failureCount}] {args.Diagnostic.Kind}: {args.Diagnostic.Message[..Math.Min(args.Diagnostic.Message.Length, 200)]}");
        });

        // Load with timeout and progress logging
        Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Opening solution via MSBuildWorkspace...");
        using var cts = new CancellationTokenSource(timeout);
        var progress = new ConsoleProgressLogger(sw);

        try
        {
            Solution solution;
            if (solutionPath.EndsWith(".csproj", StringComparison.OrdinalIgnoreCase))
            {
                var project = await workspace.OpenProjectAsync(solutionPath, progress, cts.Token);
                solution = project.Solution;
            }
            else
            {
                solution = await workspace.OpenSolutionAsync(solutionPath, progress, cts.Token);
            }

            Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Solution loaded successfully!");
            Console.WriteLine($"  Projects: {solution.Projects.Count()}");
            if (failureCount > 20)
                Console.WriteLine($"  (suppressed {failureCount - 20} additional workspace failures)");

            foreach (var project in solution.Projects)
            {
                var docCount = project.Documents.Count();
                Console.WriteLine($"  - {project.Name}: {docCount} documents, TFM={project.ParseOptions}");
            }

            // Try getting compilation for the first project to verify semantic loading works
            var firstProject = solution.Projects.FirstOrDefault();
            if (firstProject != null)
            {
                Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Getting compilation for {firstProject.Name}...");
                var compilation = await firstProject.GetCompilationAsync(cts.Token);
                if (compilation != null)
                {
                    var errors = compilation.GetDiagnostics()
                        .Where(d => d.Severity == DiagnosticSeverity.Error)
                        .Take(5)
                        .ToList();
                    Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Compilation obtained. Errors: {errors.Count}");
                    foreach (var err in errors)
                        Console.WriteLine($"    {err.Id}: {err.GetMessage()[..Math.Min(err.GetMessage().Length, 150)]}");
                }
            }

            Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] Done. Total elapsed: {sw.Elapsed}");
            Assert.True(solution.Projects.Any(), "Solution should contain at least one project");
        }
        catch (OperationCanceledException)
        {
            Console.WriteLine($"[{sw.Elapsed:mm\\:ss\\.fff}] TIMED OUT after {timeout}!");
            Console.WriteLine($"  Workspace failures before timeout: {failureCount}");
            Assert.Fail($"MSBuildWorkspace loading timed out after {timeout}");
        }
    }
}
