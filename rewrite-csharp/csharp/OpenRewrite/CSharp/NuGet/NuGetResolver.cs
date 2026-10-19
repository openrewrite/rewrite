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
using System.Collections.Immutable;
using System.Diagnostics;
using System.Xml.Linq;
using Microsoft.Build.Construction;
using Microsoft.Build.Evaluation;
using Microsoft.Build.Execution;
using NuGet.Commands;
using NuGet.Common;
using NuGet.Configuration;
using NuGet.Frameworks;
using NuGet.LibraryModel;
using NuGet.Packaging;
using NuGet.Packaging.Core;
using NuGet.Packaging.Signing;
using NuGet.ProjectModel;
using NuGet.Protocol;
using NuGet.Protocol.Core.Types;
using NuGet.Versioning;
using Serilog;
using ILogger = NuGet.Common.ILogger;

using OpenRewrite.Core;

namespace OpenRewrite.CSharp.NuGet;

/// <summary>
/// Points in-process MSBuild at the .NET SDK by setting <c>MSBUILD_EXE_PATH</c>, which is all the
/// engine needs to resolve <c>Microsoft.NET.Sdk</c> and the rest of the SDK's targets. Child
/// <c>dotnet</c> processes must not inherit it — see <see cref="ScrubFrom"/>.
/// <para>
/// The SDK's workload resolver reads the process working directory on every evaluation, even
/// when the project path is known, and fails the evaluation of every SDK-style project once that
/// directory has been deleted. <see cref="EnsureWorkingDirectory"/> moves the process to a
/// directory that exists before each evaluation.
/// </para>
/// <para>
/// An in-process build moves the process working directory to the project's directory and swaps
/// the process environment for the length of the build. Every in-process evaluation or build, and
/// anything that starts a child process inheriting the working directory it cannot choose itself
/// (MSBuildWorkspace's BuildHost), runs inside <see cref="EnterOperatingEnvironment"/>, so none of
/// them sees another's borrowed directory or environment.
/// </para>
/// <para>
/// <c>MSBuildLocator.Register*</c> is NOT needed with current MSBuild libraries and must not be
/// reintroduced. Reference <c>Microsoft.Build</c>, <c>Microsoft.Build.Tasks.Core</c> and
/// <c>Microsoft.Build.Utilities.Core</c>, and name the SDK here.
/// </para>
/// </summary>
internal static class MSBuildEnvironment
{
    private static readonly object Lock = new();
    private static readonly SemaphoreSlim OperatingEnvironmentGate = new(1, 1);
    private static bool _configured;

    public static IDisposable EnterOperatingEnvironment()
    {
        OperatingEnvironmentGate.Wait();
        return Entered();
    }

    public static async Task<IDisposable> EnterOperatingEnvironmentAsync(CancellationToken ct)
    {
        await OperatingEnvironmentGate.WaitAsync(ct);
        return Entered();
    }

    private static IDisposable Entered()
    {
        try
        {
            EnsureWorkingDirectory();
        }
        catch
        {
            OperatingEnvironmentGate.Release();
            throw;
        }
        return new OperatingEnvironmentLease();
    }

    private sealed class OperatingEnvironmentLease : IDisposable
    {
        private int _released;

        public void Dispose()
        {
            if (Interlocked.Exchange(ref _released, 1) == 0)
                OperatingEnvironmentGate.Release();
        }
    }

    public static void Ensure()
    {
        lock (Lock)
        {
            if (_configured)
                return;
            _configured = true;

            if (!string.IsNullOrEmpty(Environment.GetEnvironmentVariable("MSBUILD_EXE_PATH")))
                return;

            var msbuild = FindSdkMSBuild();
            if (msbuild == null)
            {
                Log.Debug("MSBuildEnvironment: no .NET SDK found; MSBuild evaluation may fail");
                return;
            }

            Environment.SetEnvironmentVariable("MSBUILD_EXE_PATH", msbuild);
            Log.Debug("MSBuildEnvironment: MSBUILD_EXE_PATH={Path}", msbuild);
        }
    }

    private static void EnsureWorkingDirectory()
    {
        try
        {
            if (Directory.Exists(Directory.GetCurrentDirectory()))
                return;
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
        }

        var fallback = Path.GetTempPath();
        Log.Debug("MSBuildEnvironment: working directory no longer exists, moving to {Path}", fallback);
        Directory.SetCurrentDirectory(fallback);
    }

    /// <summary>
    /// Removes the variable this class set from a child process's environment. A child
    /// <c>dotnet</c> initializes its own MSBuild and fails with "The type initializer for
    /// 'Microsoft.Build.Execution.BuildParameters' threw an exception" if it inherits ours.
    /// </summary>
    public static void ScrubFrom(System.Diagnostics.ProcessStartInfo psi)
    {
        psi.Environment.Remove("MSBUILD_EXE_PATH");
    }

    private static string? FindSdkMSBuild()
    {
        foreach (var root in CandidateDotnetRoots())
        {
            var sdkRoot = Path.Combine(root, "sdk");
            if (!Directory.Exists(sdkRoot))
                continue;
            var best = Directory.EnumerateDirectories(sdkRoot)
                .Select(d => (Dir: d, File: Path.Combine(d, "MSBuild.dll")))
                .Where(x => File.Exists(x.File))
                .OrderByDescending(x => ParseVersion(Path.GetFileName(x.Dir)))
                .Select(x => x.File)
                .FirstOrDefault();
            if (best != null)
                return best;
        }
        return null;
    }

    private static IEnumerable<string> CandidateDotnetRoots()
    {
        var host = Environment.GetEnvironmentVariable("DOTNET_HOST_PATH");
        if (!string.IsNullOrEmpty(host) && File.Exists(host))
            yield return Path.GetDirectoryName(host)!;

        var dotnetRoot = Environment.GetEnvironmentVariable("DOTNET_ROOT");
        if (!string.IsNullOrEmpty(dotnetRoot))
            yield return dotnetRoot;

        var runtime = Path.GetDirectoryName(typeof(object).Assembly.Location);
        if (runtime != null)
        {
            var shared = Path.GetDirectoryName(Path.GetDirectoryName(runtime));
            var root = shared == null ? null : Path.GetDirectoryName(shared);
            if (root != null)
                yield return root;
        }
    }

    private static Version ParseVersion(string name)
    {
        var core = name.Split('-')[0];
        return Version.TryParse(core, out var v) ? v : new Version(0, 0);
    }
}

/// <summary>
/// In-process NuGet engine replacing all child-process package operations
/// (<c>dotnet restore</c>, <c>nuget restore</c>, <c>nuget install</c>) and all
/// <c>obj/project.assets.json</c> file reads.
///
/// Restore graphs are obtained one of two ways:
/// <list type="bullet">
///   <item>PackageReference projects: the MSBuild <c>GenerateRestoreGraphFile</c> target is
///         executed in-process (full fidelity: imports, conditions, CPM, PackageDownload,
///         FrameworkReference) to produce a <see cref="DependencyGraphSpec"/>, which is then
///         restored via <see cref="RestoreRunner"/>. The in-memory <see cref="LockFile"/> from
///         each <see cref="RestoreResult"/> feeds MSBuildProject marker attestation; when
///         <c>commit</c> is requested the standard restore outputs (assets file, props/targets)
///         are also written for Roslyn compilation.</item>
///   <item>Legacy <c>packages.config</c> projects: a <see cref="PackageSpec"/> is synthesized
///         from the packages.config entries (exact-pinned, PackageReference-style) and restored
///         without commit, yielding a LockFile-grade dependency graph for attestation that
///         packages.config projects never had before.</item>
/// </list>
/// </summary>
public static class NuGetResolver
{
    static NuGetResolver()
    {
        // Fail fast on dead feeds (previously passed as env vars to the dotnet child process).
        // Only set when the user hasn't configured them explicitly.
        SetEnvIfAbsent("NUGET_ENHANCED_MAX_NETWORK_TRY_COUNT", "1");
        SetEnvIfAbsent("NUGET_ENHANCED_NETWORK_RETRY_DELAY_MILLISECONDS", "100");
    }

    private static void SetEnvIfAbsent(string name, string value)
    {
        if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable(name)))
            Environment.SetEnvironmentVariable(name, value);
    }

    /// <summary>NuGet.Common logger bridging to Serilog at Debug level.</summary>
    private sealed class SerilogNuGetLogger : LoggerBase
    {
        public static readonly SerilogNuGetLogger Instance = new();

        public override void Log(ILogMessage message)
        {
            if (NuGetSourceFailures.TryRecord(message))
                return;
            Serilog.Log.Debug("NuGet: {Message}", message.Message);
        }

        public override Task LogAsync(ILogMessage message)
        {
            Log(message);
            return Task.CompletedTask;
        }
    }

    private sealed class SerilogMSBuildLogger : Microsoft.Build.Framework.ILogger
    {
        private readonly HashSet<string> _errorCodes = new(StringComparer.OrdinalIgnoreCase);
        private readonly List<string> _errors = new();

        public bool ReportedMissingWorkloads
        {
            get
            {
                lock (_errorCodes)
                    return _errorCodes.Contains("NETSDK1147") || _errorCodes.Contains("NETSDK1178");
            }
        }

        public bool ReportedTargetFrameworkNeedingNewerSdk
        {
            get
            {
                lock (_errorCodes)
                    return _errorCodes.Contains("NETSDK1045");
            }
        }

        /// <summary>The distinct MSBuild errors raised, most useful first.</summary>
        public IReadOnlyList<string> Errors
        {
            get
            {
                lock (_errors)
                    return _errors.ToList();
            }
        }

        public Microsoft.Build.Framework.LoggerVerbosity Verbosity { get; set; } =
            Microsoft.Build.Framework.LoggerVerbosity.Quiet;

        public string? Parameters { get; set; }

        public void Initialize(Microsoft.Build.Framework.IEventSource eventSource)
        {
            eventSource.ErrorRaised += (_, e) =>
            {
                lock (_errorCodes)
                {
                    if (!string.IsNullOrEmpty(e.Code))
                        _errorCodes.Add(e.Code);
                }
                lock (_errors)
                {
                    var text = string.IsNullOrEmpty(e.Code) ? e.Message : e.Code + ": " + e.Message;
                    if (_errors.Count < 5 && !_errors.Contains(text))
                        _errors.Add(text);
                }
                Log.Debug("MSBuild error {Code} at {File}({Line}): {Message}",
                    e.Code, e.File, e.LineNumber, e.Message);
            };
            eventSource.WarningRaised += (_, e) =>
                Log.Debug("MSBuild warning {Code}: {Message}", e.Code, e.Message);
        }

        public void Shutdown()
        {
        }
    }

    public static ILogger Logger => SerilogNuGetLogger.Instance;

    public static ISettings LoadSettings(string startDirectory) =>
        Settings.LoadDefaultSettings(startDirectory, null, new XPlatMachineWideSetting());

    /// <summary>
    /// The enabled package source URLs configured for <paramref name="startDirectory"/>, used to
    /// attribute a failing resource URL back to the feed it came from. Empty when settings
    /// cannot be read — grouping then falls back to the URL authority.
    /// </summary>
    public static IReadOnlyList<string> EnabledSourceUrls(string startDirectory)
    {
        try
        {
            return SettingsUtility.GetEnabledSources(LoadSettings(startDirectory))
                .Select(s => s.Source)
                .Where(s => !string.IsNullOrEmpty(s))
                .ToList();
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: failed to read package sources from {Dir}: {Error}",
                startDirectory, ex.Message);
            return [];
        }
    }

    #region Restore graph generation (PackageReference projects)

    /// <summary>
    /// Produces the restore dependency graph for a solution or project via the
    /// <c>GenerateRestoreGraphFile</c> MSBuild target, one child evaluation for the whole
    /// solution, falling back to per-project evaluation when the solution as a whole fails.
    /// Returns null when no project produced a graph.
    /// </summary>
    public static DependencyGraphSpec? CreateDependencyGraphSpec(
        string path,
        IDictionary<string, string>? extraGlobalProperties = null)
    {
        var entry = Path.GetFullPath(path);

        var solutionGraph = GenerateRestoreGraph(entry, extraGlobalProperties);
        if (solutionGraph != null && solutionGraph.Projects.Count > 0)
            return solutionGraph;

        var projects = EnumerateProjects(entry).ToList();
        if (projects.Count == 0)
        {
            Log.Debug("NuGetResolver: no MSBuild projects found for {Path}", path);
            return null;
        }

        if (projects.Count == 1 && string.Equals(projects[0], entry, StringComparison.OrdinalIgnoreCase))
            return null;

        Log.Debug("NuGetResolver: solution-level restore graph unavailable for {Path}; " +
                  "falling back to per-project evaluation ({Count} projects)", path, projects.Count);

        var merged = new DependencyGraphSpec();
        var any = false;
        foreach (var projectPath in projects)
        {
            var dgSpec = GenerateRestoreGraph(projectPath, extraGlobalProperties);
            if (dgSpec == null)
                continue;

            foreach (var project in dgSpec.Projects)
            {
                if (merged.GetProjectSpec(project.RestoreMetadata?.ProjectUniqueName) == null)
                    merged.AddProject(project);
            }
            foreach (var restore in dgSpec.Restore)
                merged.AddRestore(restore);
            any = true;
        }

        return any ? merged : null;
    }

    /// <summary>
    /// Enumerates MSBuild project files for an entry path: the project itself, the projects of
    /// a .sln (via <see cref="SolutionFile"/>), or of a .slnx (XML &lt;Project Path="..."/&gt;).
    /// </summary>
    public static IEnumerable<string> EnumerateProjects(string path)
    {
        if (path.EndsWith(".sln", StringComparison.OrdinalIgnoreCase))
        {
            SolutionFile solution;
            try
            {
                solution = SolutionFile.Parse(Path.GetFullPath(path));
            }
            catch (Exception ex)
            {
                Log.Debug("NuGetResolver: failed to parse solution {Path}: {Error}", path, ex.Message);
                yield break;
            }
            foreach (var p in solution.ProjectsInOrder)
            {
                if (p.ProjectType == SolutionProjectType.KnownToBeMSBuildFormat && File.Exists(p.AbsolutePath))
                    yield return Path.GetFullPath(p.AbsolutePath);
            }
        }
        else if (path.EndsWith(".slnx", StringComparison.OrdinalIgnoreCase))
        {
            var dir = Path.GetDirectoryName(Path.GetFullPath(path))!;
            XDocument doc;
            try
            {
                doc = XDocument.Load(path);
            }
            catch (Exception ex)
            {
                Log.Debug("NuGetResolver: failed to parse slnx {Path}: {Error}", path, ex.Message);
                yield break;
            }
            foreach (var project in doc.Descendants("Project"))
            {
                var rel = project.Attribute("Path")?.Value;
                if (rel == null)
                    continue;
                var abs = Path.GetFullPath(Path.Combine(dir, rel.Replace('\\', Path.DirectorySeparatorChar)));
                if (File.Exists(abs))
                    yield return abs;
            }
        }
        else
        {
            yield return Path.GetFullPath(path);
        }
    }

    /// <summary>
    /// Sets <c>EnableWindowsTargeting=true</c> on non-Windows hosts, where the SDK otherwise
    /// fails every project with a Windows target platform (<c>net10.0-windows</c>, WPF/WinForms)
    /// with <c>NETSDK1100</c>. Analysis never runs the produced binaries, so cross-targeting is
    /// always safe here. Skipped when the variable is set in the environment: MSBuild already
    /// seeds that as a property and an explicit choice must win over this default.
    /// </summary>
    public static void ApplyWindowsTargetingDefault(IDictionary<string, string> properties)
    {
        if (OperatingSystem.IsWindows() ||
            !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("EnableWindowsTargeting")))
            return;
        properties["EnableWindowsTargeting"] = "true";
    }

    /// <summary>
    /// Sets <c>AndroidUseManagedDesignTimeResourceGenerator=true</c> on non-Windows hosts, as the
    /// Android SDK already does on Windows. Otherwise design-time builds of <c>net*-android</c>
    /// projects compile resources with <c>aapt2</c> and fail against any JDK newer than the
    /// workload supports (JDK 25 against a workload that accepts up to 21, say), leaving the
    /// project without references. Skipped when the variable is set in the environment, so an
    /// explicit choice wins.
    /// </summary>
    public static void ApplyAndroidDesignTimeDefault(IDictionary<string, string> properties)
    {
        if (OperatingSystem.IsWindows() ||
            !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("AndroidUseManagedDesignTimeResourceGenerator")))
            return;
        properties["AndroidUseManagedDesignTimeResourceGenerator"] = "true";
    }

    /// <summary>
    /// Sets <c>CheckEolWorkloads=false</c>, so projects on an out-of-support workload
    /// (<c>net8.0-android</c> on a .NET 10 SDK, say) still evaluate instead of failing with
    /// <c>NETSDK1202</c>. Those are the codebases most in need of an upgrade, and they have to
    /// parse first. Skipped when the variable is set in the environment, so an explicit choice wins.
    /// </summary>
    public static void ApplyOutOfSupportWorkloadsDefault(IDictionary<string, string> properties)
    {
        if (!string.IsNullOrEmpty(Environment.GetEnvironmentVariable("CheckEolWorkloads")))
            return;
        properties["CheckEolWorkloads"] = "false";
    }

    /// <summary>The workloads a project needs but the SDK does not have installed.</summary>
    /// <param name="TargetFramework">The target framework that needs them.</param>
    /// <param name="Workloads">Installable workload IDs that provide the missing packs.</param>
    /// <param name="Packs">The missing workload packs, including those no workload for this
    /// operating system provides.</param>
    public sealed record MissingWorkloads(
        string TargetFramework,
        IReadOnlyList<string> Workloads,
        IReadOnlyList<string> Packs);

    /// <summary>
    /// Evaluates <paramref name="projectPath"/> for <paramref name="targetFramework"/> and reports
    /// the workloads it needs that are not installed, or null when none are missing.
    /// </summary>
    public static MissingWorkloads? FindMissingWorkloads(
        string projectPath,
        string? targetFramework,
        IDictionary<string, string>? extraGlobalProperties)
    {
        MSBuildEnvironment.Ensure();
        var globalProps = DesignTimeProperties(targetFramework, extraGlobalProperties);

        try
        {
            using (MSBuildEnvironment.EnterOperatingEnvironment())
            {
                using var projectCollection = new ProjectCollection(globalProps);
                var instance = new ProjectInstance(projectPath, globalProps, null, projectCollection);
                var packs = instance.GetItems("MissingWorkloadPack")
                    .Select(i => i.EvaluatedInclude)
                    .Distinct(StringComparer.OrdinalIgnoreCase)
                    .ToList();
                if (packs.Count == 0)
                    return null;

                instance.Build(new[] { "GetSuggestedWorkloads" }, null, out var outputs);
                var workloads = outputs.TryGetValue("GetSuggestedWorkloads", out var suggested)
                    ? suggested.Items.Select(i => i.ItemSpec).Distinct(StringComparer.OrdinalIgnoreCase).ToList()
                    : new List<string>();
                return new MissingWorkloads(instance.GetPropertyValue("TargetFramework"), workloads, packs);
            }
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: could not check {Project} ({TargetFramework}) for missing workloads: {Error}",
                projectPath, targetFramework, ex.Message);
            return null;
        }
    }

    /// <summary>A target framework newer than the installed .NET SDK can build.</summary>
    /// <param name="TargetFramework">The target framework alias.</param>
    /// <param name="SdkVersion">The installed .NET SDK version.</param>
    public sealed record UnsupportedTargetFramework(string TargetFramework, string SdkVersion);

    /// <summary>
    /// Evaluates <paramref name="projectPath"/> for <paramref name="targetFramework"/> and reports
    /// it when the target framework needs a newer .NET SDK than the installed one (NETSDK1045),
    /// or null when the SDK supports it.
    /// </summary>
    public static UnsupportedTargetFramework? FindTargetFrameworkNeedingNewerSdk(
        string projectPath,
        string? targetFramework,
        IDictionary<string, string>? extraGlobalProperties)
    {
        MSBuildEnvironment.Ensure();
        var globalProps = DesignTimeProperties(targetFramework, extraGlobalProperties);

        try
        {
            using (MSBuildEnvironment.EnterOperatingEnvironment())
            {
                using var projectCollection = new ProjectCollection(globalProps);
                var instance = new ProjectInstance(projectPath, globalProps, null, projectCollection);
                if (targetFramework == null && instance.GetPropertyValue("TargetFramework").Length == 0 &&
                    instance.GetPropertyValue("TargetFrameworks")
                        .Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries) is [var only])
                {
                    globalProps["TargetFramework"] = only;
                    instance = new ProjectInstance(projectPath, globalProps, null, projectCollection);
                }
                var maximum = instance.GetPropertyValue("TargetFrameworkIdentifier") switch
                {
                    ".NETCoreApp" => instance.GetPropertyValue("NETCoreAppMaximumVersion"),
                    ".NETStandard" => instance.GetPropertyValue("NETStandardMaximumVersion"),
                    _ => ""
                };
                if (!Version.TryParse(maximum, out var maximumVersion) ||
                    !Version.TryParse(instance.GetPropertyValue("_TargetFrameworkVersionWithoutV"), out var version) ||
                    Normalized(version) <= Normalized(maximumVersion))
                    return null;
                return new UnsupportedTargetFramework(
                    instance.GetPropertyValue("TargetFramework"),
                    instance.GetPropertyValue("NETCoreSdkVersion"));
            }
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: could not check whether the SDK supports {Project} ({TargetFramework}): {Error}",
                projectPath, targetFramework, ex.Message);
            return null;
        }
    }

    private static Dictionary<string, string> DesignTimeProperties(
        string? targetFramework,
        IDictionary<string, string>? extraGlobalProperties)
    {
        var globalProps = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["DesignTimeBuild"] = "true",
        };
        ApplyWindowsTargetingDefault(globalProps);
        ApplyAndroidDesignTimeDefault(globalProps);
        ApplyOutOfSupportWorkloadsDefault(globalProps);
        if (extraGlobalProperties != null)
        {
            foreach (var (k, v) in extraGlobalProperties)
                globalProps[k] = v;
        }
        if (targetFramework != null)
            globalProps["TargetFramework"] = targetFramework;
        return globalProps;
    }

    private static readonly Dictionary<string, DependencyGraphSpec?> GraphCache =
        new(StringComparer.Ordinal);

    private static long _graphGenerationMs;
    private static long _restoreExecutionMs;
    private static readonly System.Collections.Concurrent.ConcurrentDictionary<string, int> EvaluationCounts =
        new(StringComparer.OrdinalIgnoreCase);

    private static long _noOpRestores;

    /// <summary>Restores NuGet short-circuited because the project was already up to date.</summary>
    public static long NoOpRestores => Interlocked.Read(ref _noOpRestores);

    /// <summary>Child MSBuild evaluations run for paths under <paramref name="directory"/>.</summary>
    public static long GraphEvaluationsUnder(string directory)
    {
        var prefix = Path.GetFullPath(directory);
        return EvaluationCounts
            .Where(kv => kv.Key.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
            .Sum(kv => (long)kv.Value);
    }

    /// <summary>Wall-clock milliseconds spent generating restore graphs in this process.</summary>
    public static long GraphGenerationMs => Interlocked.Read(ref _graphGenerationMs);

    /// <summary>Wall-clock milliseconds spent resolving and downloading packages in this process.</summary>
    public static long RestoreExecutionMs => Interlocked.Read(ref _restoreExecutionMs);

    private static string GraphCacheKey(string path, IDictionary<string, string>? extraGlobalProperties)
    {
        if (extraGlobalProperties == null || extraGlobalProperties.Count == 0)
            return path;
        var props = extraGlobalProperties
            .OrderBy(kv => kv.Key, StringComparer.OrdinalIgnoreCase)
            .Select(kv => kv.Key + "=" + kv.Value);
        return path + " " + string.Join(" ", props);
    }

    private static DependencyGraphSpec? GenerateRestoreGraph(
        string projectPath,
        IDictionary<string, string>? extraGlobalProperties)
    {
        var cacheKey = GraphCacheKey(projectPath, extraGlobalProperties);
        lock (GraphCache)
        {
            if (GraphCache.TryGetValue(cacheKey, out var cached))
            {
                Log.Debug("NuGetResolver: restore graph cache hit for {Project}", projectPath);
                return cached;
            }
        }

        var sw = Stopwatch.StartNew();
        var graph = GenerateRestoreGraphUncached(projectPath, extraGlobalProperties);
        Interlocked.Add(ref _graphGenerationMs, (long)sw.Elapsed.TotalMilliseconds);

        lock (GraphCache)
        {
            GraphCache[cacheKey] = graph;
        }
        return graph;
    }

    private static DependencyGraphSpec? GenerateRestoreGraphUncached(
        string projectPath,
        IDictionary<string, string>? extraGlobalProperties)
    {
        MSBuildEnvironment.Ensure();
        EvaluationCounts.AddOrUpdate(projectPath, 1, (_, n) => n + 1);
        var outputPath = Path.Combine(Path.GetTempPath(),
            "openrewrite-dg-" + Tree.RandomId().ToString("N")[..8] + ".json");
        try
        {
            var globalProps = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
            {
                ["RestoreGraphOutputPath"] = outputPath,
                // Do NOT set RestoreRecursive explicitly: as a global property it forces the
                // full project-path walk (which does not drop missing project references) into
                // the graph-entry task, turning skippable missing P2P refs into MSB3202 errors.
                // NuGet.targets' internal default already discovers the closure per entry.
                ["NuGetAudit"] = "false",
                ["RestoreIgnoreFailedSources"] = "true",
                // Avoid restore-time package imports polluting evaluation
                ["ExcludeRestorePackageImports"] = "true",
            };
            ApplyWindowsTargetingDefault(globalProps);
            ApplyAndroidDesignTimeDefault(globalProps);
            ApplyOutOfSupportWorkloadsDefault(globalProps);
            if (extraGlobalProperties != null)
            {
                foreach (var (k, v) in extraGlobalProperties)
                    globalProps[k] = v;
            }

            var buildLogger = new SerilogMSBuildLogger();
            var result = BuildRestoreGraph(projectPath, globalProps, buildLogger);
            Version? netCoreAppMaximum = null;
            while (result.OverallResult != BuildResultCode.Success)
            {
                if (buildLogger.ReportedMissingWorkloads && !globalProps.ContainsKey("DesignTimeBuild"))
                {
                    Log.Debug("NuGetResolver: {Project} targets platforms without an installed workload; " +
                              "regenerating its restore graph as a design-time build", projectPath);
                    globalProps["DesignTimeBuild"] = "true";
                }
                else if (buildLogger.ReportedTargetFrameworkNeedingNewerSdk && netCoreAppMaximum == null)
                {
                    Log.Debug("NuGetResolver: {Project} targets frameworks newer than the installed SDK supports; " +
                              "regenerating its restore graph without them", projectPath);
                    netCoreAppMaximum = NetCoreAppMaximumVersion(projectPath, globalProps);
                    if (netCoreAppMaximum == null)
                        break;
                    globalProps["NETCoreAppMaximumVersion"] = UnboundedFrameworkVersion;
                    globalProps["NETStandardMaximumVersion"] = UnboundedFrameworkVersion;
                }
                else
                {
                    break;
                }
                buildLogger = new SerilogMSBuildLogger();
                result = BuildRestoreGraph(projectPath, globalProps, buildLogger);
            }

            if (result.OverallResult != BuildResultCode.Success || !File.Exists(outputPath))
            {
                Log.Warning("Restore graph generation failed for {Project}: {Errors}",
                    projectPath,
                    buildLogger.Errors.Count > 0
                        ? string.Join(" | ", buildLogger.Errors)
                        : result.Exception?.Message ?? "(no MSBuild error reported)");
                return null;
            }

            var dgSpec = WithoutUnresolvedPlatforms(DependencyGraphSpec.Load(outputPath));
            return netCoreAppMaximum == null ? dgSpec : WithoutFrameworksNewerThan(dgSpec, netCoreAppMaximum);
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: restore graph generation failed for {Project}: {Error}",
                projectPath, ex.Message);
            return null;
        }
        finally
        {
            try { File.Delete(outputPath); } catch { /* best effort */ }
        }
    }

    private static BuildResult BuildRestoreGraph(
        string projectPath,
        Dictionary<string, string> globalProps,
        SerilogMSBuildLogger buildLogger)
    {
        using (MSBuildEnvironment.EnterOperatingEnvironment())
        {
            using var projectCollection = new ProjectCollection(globalProps);
            var parameters = new BuildParameters(projectCollection)
            {
                DisableInProcNode = false,
                EnableNodeReuse = false,
                MaxNodeCount = 1,
                Loggers = new Microsoft.Build.Framework.ILogger[] { buildLogger },
            };
            var requestData = new BuildRequestData(
                projectPath,
                globalProps.ToDictionary(p => p.Key, p => (string?)p.Value, StringComparer.OrdinalIgnoreCase),
                null,
                new[] { "GenerateRestoreGraphFile" },
                null);
            return BuildManager.DefaultBuildManager.Build(parameters, requestData);
        }
    }

    private static readonly System.Collections.Concurrent.ConcurrentDictionary<string, IReadOnlySet<string>>
        UnresolvedPlatformsByProject = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// Target framework aliases of <paramref name="projectPath"/> that restore left out because
    /// their platform (<c>net10.0-ios</c>, say) resolved no version, which is what happens when the
    /// workload that provides the platform is not installed.
    /// </summary>
    public static IReadOnlySet<string> UnresolvedPlatformTargetFrameworks(string projectPath) =>
        UnresolvedPlatformsByProject.TryGetValue(Path.GetFullPath(projectPath), out var frameworks)
            ? frameworks
            : new HashSet<string>();

    private const string UnboundedFrameworkVersion = "1000000.0";

    private static Version? NetCoreAppMaximumVersion(string projectPath, Dictionary<string, string> globalProps)
    {
        using (MSBuildEnvironment.EnterOperatingEnvironment())
        {
            using var projectCollection = new ProjectCollection(globalProps);
            var instance = new ProjectInstance(projectPath, globalProps, null, projectCollection);
            return Version.TryParse(instance.GetPropertyValue("NETCoreAppMaximumVersion"), out var maximum)
                ? maximum
                : null;
        }
    }

    private static DependencyGraphSpec WithoutFrameworksNewerThan(DependencyGraphSpec dgSpec, Version netCoreAppMaximum)
    {
        foreach (var spec in dgSpec.Projects)
        {
            var unsupported = spec.TargetFrameworks
                .Where(tf => tf.FrameworkName.Framework switch
                {
                    FrameworkConstants.FrameworkIdentifiers.NetCoreApp =>
                        Normalized(tf.FrameworkName.Version) > Normalized(netCoreAppMaximum),
                    FrameworkConstants.FrameworkIdentifiers.NetStandard =>
                        Normalized(tf.FrameworkName.Version) > Normalized(FrameworkConstants.CommonFrameworks.NetStandard21.Version),
                    _ => false
                })
                .ToList();
            RemoveTargetFrameworks(spec, unsupported);
            if (unsupported.Count > 0)
                Log.Debug("NuGetResolver: not restoring {Frameworks} of {Project}: the installed SDK does not support them",
                    string.Join(", ", unsupported.Select(f => f.TargetAlias)), spec.FilePath);
        }
        return dgSpec;
    }

    private static Version Normalized(Version version) =>
        new(version.Major, version.Minor, Math.Max(version.Build, 0), Math.Max(version.Revision, 0));

    private static void RemoveTargetFrameworks(PackageSpec spec, IEnumerable<TargetFrameworkInformation> frameworks)
    {
        foreach (var framework in frameworks)
        {
            spec.TargetFrameworks.Remove(framework);
            var metadata = spec.RestoreMetadata;
            if (metadata == null)
                continue;
            metadata.OriginalTargetFrameworks.Remove(framework.TargetAlias);
            foreach (var info in metadata.TargetFrameworks.Where(i => i.TargetAlias == framework.TargetAlias).ToList())
                metadata.TargetFrameworks.Remove(info);
        }
    }

    private static DependencyGraphSpec WithoutUnresolvedPlatforms(DependencyGraphSpec dgSpec)
    {
        foreach (var spec in dgSpec.Projects)
        {
            var unresolved = spec.TargetFrameworks
                .Where(tf => tf.FrameworkName.HasPlatform &&
                             tf.FrameworkName.PlatformVersion == FrameworkConstants.EmptyVersion)
                .ToList();
            RemoveTargetFrameworks(spec, unresolved);
            if (unresolved.Count > 0 && (spec.RestoreMetadata?.ProjectPath ?? spec.FilePath) is { } projectPath)
                UnresolvedPlatformsByProject[Path.GetFullPath(projectPath)] = unresolved
                    .Select(f => f.TargetAlias)
                    .ToHashSet(StringComparer.OrdinalIgnoreCase);
            if (unresolved.Count > 0)
                Log.Debug("NuGetResolver: not restoring {Frameworks} of {Project}: no installed workload provides the platform",
                    string.Join(", ", unresolved.Select(f => f.TargetAlias)), spec.FilePath);
        }
        return dgSpec;
    }


    #endregion

    #region Restore execution

    /// <summary>
    /// Restores all PackageReference-style projects in the graph. Returns the in-memory
    /// <see cref="LockFile"/> per project path. When <paramref name="commit"/> is true the
    /// standard restore outputs (project.assets.json, nuget props/targets) are also written so
    /// MSBuildWorkspace can compile the projects.
    /// </summary>
    public static async Task<Dictionary<string, LockFile>> RestoreAsync(
        DependencyGraphSpec dgSpec, bool commit, CancellationToken ct)
    {
        var lockFiles = new Dictionary<string, LockFile>(StringComparer.OrdinalIgnoreCase);

        // Only PackageReference-style projects restore through RestoreRunner.
        var restorable = new DependencyGraphSpec();
        var anyRestorable = false;
        foreach (var project in dgSpec.Projects)
        {
            var style = project.RestoreMetadata?.ProjectStyle;
            if (project.TargetFrameworks.Count == 0)
            {
                Log.Debug("NuGetResolver: skipping restore of {Project}: none of its target frameworks can be restored",
                    project.RestoreMetadata?.ProjectUniqueName);
            }
            else if (style == ProjectStyle.PackageReference || style == ProjectStyle.DotnetCliTool ||
                style == ProjectStyle.Standalone)
            {
                restorable.AddProject(project);
                restorable.AddRestore(project.RestoreMetadata!.ProjectUniqueName);
                anyRestorable = true;
            }
            else if (style == ProjectStyle.ProjectJson || style == ProjectStyle.Unknown ||
                     style == ProjectStyle.PackagesConfig)
            {
                Log.Debug("NuGetResolver: skipping RestoreRunner for {Style} project {Project}",
                    style, project.RestoreMetadata?.ProjectUniqueName);
                // Referenced projects still need to be in the graph for P2P edges.
                restorable.AddProject(project);
            }
        }

        if (!anyRestorable)
            return lockFiles;

        var settingsRoot = restorable.Projects
            .Select(p => Path.GetDirectoryName(p.RestoreMetadata?.ProjectPath ?? p.FilePath))
            .FirstOrDefault(d => d != null) ?? Directory.GetCurrentDirectory();
        var settings = LoadSettings(settingsRoot!);

        using var cacheContext = new SourceCacheContext { IgnoreFailedSources = true };
        var providerCache = new RestoreCommandProvidersCache();
        var restoreArgs = new RestoreArgs
        {
            AllowNoOp = true,
            CacheContext = cacheContext,
            Log = Logger,
            CachingSourceProvider = new CachingSourceProvider(new PackageSourceProvider(settings)),
        };

        var requestProvider = new DependencyGraphSpecRequestProvider(providerCache, restorable, settings);
        var requests = await requestProvider.CreateRequests(restoreArgs);

        var sw = Stopwatch.StartNew();
        var results = await RestoreRunner.RunWithoutCommit(requests, restoreArgs);
        Interlocked.Add(ref _restoreExecutionMs, (long)sw.Elapsed.TotalMilliseconds);

        var noOpCount = 0;
        foreach (var pair in results)
        {
            var projectPath = pair.SummaryRequest.Request.Project.RestoreMetadata?.ProjectPath
                              ?? pair.SummaryRequest.Request.Project.FilePath;
            if (!pair.Result.Success)
            {
                Log.Debug("NuGetResolver: restore failed for {Project}", projectPath);
            }
            if (pair.Result is NoOpRestoreResult)
            {
                noOpCount++;
                Interlocked.Increment(ref _noOpRestores);
            }
            if (commit)
            {
                try
                {
                    await pair.Result.CommitAsync(Logger, ct);
                }
                catch (Exception ex)
                {
                    Log.Debug("NuGetResolver: commit failed for {Project}: {Error}", projectPath, ex.Message);
                }
            }
            if (pair.Result.LockFile != null)
                PackageCompileAssets.RecordPackageFolders(pair.Result.LockFile);
            if (projectPath != null && pair.Result.LockFile != null)
                lockFiles[Path.GetFullPath(projectPath)] = pair.Result.LockFile;
        }

        Log.Debug("NuGetResolver: restored {Count} projects ({NoOp} up to date)", results.Count, noOpCount);
        return lockFiles;
    }

    #endregion

    #region packages.config attestation graph

    /// <summary>
    /// Synthesizes a PackageReference-style <see cref="PackageSpec"/> from packages.config
    /// entries (exact-pinned versions) and restores it without commit, producing a
    /// LockFile-grade dependency graph for a legacy project. This is how packages.config
    /// projects — which have no project.assets.json — get full dependency attestation.
    /// </summary>
    public static async Task<LockFile?> RestorePackagesConfigGraphAsync(
        string projectPath,
        string packagesConfigPath,
        NuGetFramework? framework,
        CancellationToken ct)
    {
        try
        {
            var entries = ReadPackagesConfig(packagesConfigPath);
            if (entries.Count == 0)
                return null;

            framework ??= entries
                .Select(e => e.TargetFramework)
                .FirstOrDefault(f => f != null && !f.IsUnsupported && !f.IsAny)
                ?? NuGetFramework.Parse("net48");

            var settings = LoadSettings(Path.GetDirectoryName(Path.GetFullPath(projectPath))!);

            var alias = framework.GetShortFolderName();
            var tfi = new TargetFrameworkInformation
            {
                FrameworkName = framework,
                TargetAlias = alias,
                // NuGet 7.x: dependencies are declared per target framework (the project-level
                // PackageSpec.Dependencies list no longer exists).
                Dependencies = entries.Select(e => new LibraryDependency
                {
                    LibraryRange = new LibraryRange(
                        e.PackageIdentity.Id,
                        new VersionRange(e.PackageIdentity.Version),
                        LibraryDependencyTarget.Package),
                }).ToImmutableArray(),
            };
            var packageSpec = new PackageSpec(new List<TargetFrameworkInformation> { tfi })
            {
                Name = Path.GetFileNameWithoutExtension(projectPath),
                FilePath = projectPath,
                RestoreMetadata = new ProjectRestoreMetadata
                {
                    ProjectPath = projectPath,
                    ProjectName = Path.GetFileNameWithoutExtension(projectPath),
                    ProjectUniqueName = projectPath,
                    ProjectStyle = ProjectStyle.PackageReference,
                    OutputPath = Path.Combine(Path.GetTempPath(),
                        "openrewrite-pcrestore-" + Tree.RandomId().ToString("N")[..8]),
                    OriginalTargetFrameworks = new List<string> { alias },
                    ConfigFilePaths = settings.GetConfigFilePaths(),
                    PackagesPath = SettingsUtility.GetGlobalPackagesFolder(settings),
                    Sources = SettingsUtility.GetEnabledSources(settings).ToList(),
                    FallbackFolders = SettingsUtility.GetFallbackPackageFolders(settings).ToList(),
                },
            };
            packageSpec.RestoreMetadata.TargetFrameworks.Add(
                new ProjectRestoreMetadataFrameworkInfo(framework) { TargetAlias = alias });

            var dgSpec = new DependencyGraphSpec();
            dgSpec.AddProject(packageSpec);
            dgSpec.AddRestore(packageSpec.RestoreMetadata.ProjectUniqueName);

            var lockFiles = await RestoreAsync(dgSpec, commit: false, ct);
            return lockFiles.TryGetValue(Path.GetFullPath(projectPath), out var lockFile) ? lockFile : null;
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: packages.config graph restore failed for {Project}: {Error}",
                projectPath, ex.Message);
            return null;
        }
    }

    /// <summary>Reads packages.config entries (id, version, targetFramework, developmentDependency).</summary>
    public static IReadOnlyList<global::NuGet.Packaging.PackageReference> ReadPackagesConfig(string packagesConfigPath)
    {
        using var stream = File.OpenRead(packagesConfigPath);
        var reader = new PackagesConfigReader(stream);
        return reader.GetPackages(allowDuplicatePackageIds: true).ToList();
    }

    #endregion

    #region packages.config folder restore + flat installs (nuget.exe replacement)

    /// <summary>
    /// Materializes the solution-local <c>packages/</c> folder (NuGet v2 side-by-side layout,
    /// <c>Id.Version/</c>) for legacy packages.config projects, replacing <c>nuget restore</c>.
    /// The folder location honors <c>repositoryPath</c> from nuget.config, defaulting to
    /// <c>&lt;solutionDir&gt;/packages</c>.
    /// </summary>
    public static async Task InstallPackagesConfigPackagesAsync(
        string solutionOrProjectDir,
        IEnumerable<string> packagesConfigPaths,
        CancellationToken ct)
    {
        var settings = LoadSettings(solutionOrProjectDir);
        var repositoryPath = SettingsUtility.GetRepositoryPath(settings);
        if (string.IsNullOrEmpty(repositoryPath))
            repositoryPath = Path.Combine(solutionOrProjectDir, "packages");
        repositoryPath = Path.GetFullPath(repositoryPath);

        var identities = new HashSet<PackageIdentity>();
        foreach (var configPath in packagesConfigPaths)
        {
            try
            {
                foreach (var entry in ReadPackagesConfig(configPath))
                    identities.Add(entry.PackageIdentity);
            }
            catch (Exception ex)
            {
                Log.Debug("NuGetResolver: failed to read {Path}: {Error}", configPath, ex.Message);
            }
        }

        if (identities.Count == 0)
            return;

        var pathResolver = new PackagePathResolver(repositoryPath);
        await InstallPackagesAsync(identities, pathResolver, settings, ct);
    }

    /// <summary>
    /// Installs a single package into <paramref name="outputDirectory"/>, optionally without the
    /// version in the folder name (the <c>nuget install -ExcludeVersion</c> layout used for the
    /// .NET Framework build assets). Returns true when the package is present afterwards.
    /// </summary>
    public static async Task<bool> InstallPackageAsync(
        string packageId, string version, string outputDirectory, bool excludeVersion, CancellationToken ct)
    {
        try
        {
            Directory.CreateDirectory(outputDirectory);
            var settings = LoadSettings(outputDirectory);
            var identity = new PackageIdentity(packageId, NuGetVersion.Parse(version));
            var pathResolver = new PackagePathResolver(outputDirectory, useSideBySidePaths: !excludeVersion);
            await InstallPackagesAsync(new[] { identity }, pathResolver, settings, ct);
            return pathResolver.GetInstalledPath(identity) != null
                   || Directory.Exists(Path.Combine(outputDirectory, packageId));
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: install of {Package} {Version} failed: {Error}",
                packageId, version, ex.Message);
            return false;
        }
    }

    private static async Task InstallPackagesAsync(
        IEnumerable<PackageIdentity> identities,
        PackagePathResolver pathResolver,
        ISettings settings,
        CancellationToken ct)
    {
        using var cacheContext = new SourceCacheContext { IgnoreFailedSources = true };
        var globalPackagesFolder = SettingsUtility.GetGlobalPackagesFolder(settings);
        var downloadContext = new PackageDownloadContext(cacheContext);
        var extractionContext = new PackageExtractionContext(
            PackageSaveMode.Defaultv2,
            XmlDocFileSaveMode.None,
            ClientPolicyContext.GetClientPolicy(settings, Logger),
            Logger);

        var sourceProvider = new PackageSourceProvider(settings);
        var repositories = sourceProvider.LoadPackageSources()
            .Where(s => s.IsEnabled)
            .Select(s => Repository.Factory.GetCoreV3(s))
            .ToList();

        foreach (var identity in identities)
        {
            ct.ThrowIfCancellationRequested();
            if (pathResolver.GetInstalledPath(identity) != null)
                continue;

            var installed = false;
            foreach (var repository in repositories)
            {
                try
                {
                    var downloadResource = await repository.GetResourceAsync<DownloadResource>(ct);
                    if (downloadResource == null)
                        continue;
                    using var result = await downloadResource.GetDownloadResourceResultAsync(
                        identity, downloadContext, globalPackagesFolder, Logger, ct);
                    if (result.Status != DownloadResourceResultStatus.Available)
                        continue;

                    result.PackageStream.Seek(0, SeekOrigin.Begin);
                    await PackageExtractor.ExtractPackageAsync(
                        result.PackageSource, result.PackageStream, pathResolver, extractionContext, ct);
                    installed = true;
                    break;
                }
                catch (Exception ex) when (ex is not OperationCanceledException)
                {
                    if (!NuGetSourceFailures.TryRecordSourceFailure(
                            repository.PackageSource.Source, identity.Id, ex.Message))
                    {
                        Log.Debug("NuGetResolver: {Package} not available from {Source}: {Error}",
                            identity, repository.PackageSource.Source, ex.Message);
                    }
                }
            }

            if (!installed)
            {
                NuGetSourceFailures.RecordUnresolved(identity.Id);
                Log.Debug("NuGetResolver: failed to install {Package} from any source", identity);
            }
        }
    }

    #endregion

    #region Single-project attestation entry point

    /// <summary>
    /// Resolves the LockFile for a single project on disk: packages.config projects go through
    /// the synthesized-spec path; everything else through in-process restore-graph generation.
    /// Returns null when no graph could be produced.
    /// </summary>
    public static async Task<LockFile?> ResolveProjectLockFileAsync(
        string projectPath,
        IDictionary<string, string>? extraGlobalProperties,
        CancellationToken ct)
    {
        var projectDir = Path.GetDirectoryName(Path.GetFullPath(projectPath))!;
        using var sourceFailures = NuGetSourceFailures.Begin(
            Path.GetFileName(projectPath), EnabledSourceUrls(projectDir));
        var packagesConfig = Path.Combine(projectDir, "packages.config");
        if (File.Exists(packagesConfig))
        {
            return await RestorePackagesConfigGraphAsync(
                projectPath, packagesConfig, ReadLegacyFramework(projectPath), ct);
        }

        var dgSpec = CreateDependencyGraphSpec(projectPath, extraGlobalProperties);
        if (dgSpec == null)
            return null;
        var lockFiles = await RestoreAsync(dgSpec, commit: false, ct);
        return lockFiles.TryGetValue(Path.GetFullPath(projectPath), out var lockFile) ? lockFile : null;
    }

    /// <summary>
    /// Reads the target framework from a legacy csproj's <c>TargetFrameworkVersion</c>
    /// (e.g. <c>v4.7.2</c>), or SDK-style <c>TargetFramework(s)</c> as fallback.
    /// </summary>
    public static NuGetFramework? ReadLegacyFramework(string projectPath) =>
        ReadTargetFrameworks(projectPath).FirstOrDefault();

    /// <summary>
    /// Every target framework a project declares, from a legacy
    /// <c>TargetFrameworkVersion</c> (e.g. <c>v4.7.2</c>) or an SDK-style
    /// <c>TargetFramework(s)</c> alike. Declarations are read straight from the project XML, so
    /// frameworks that only appear once MSBuild has evaluated conditions or imported
    /// <c>Directory.Build.props</c> are not seen.
    /// </summary>
    public static IReadOnlyList<NuGetFramework> ReadTargetFrameworks(string projectPath)
    {
        var frameworks = new List<NuGetFramework>();
        try
        {
            var doc = XDocument.Load(projectPath);
            var ns = doc.Root?.Name.Namespace ?? XNamespace.None;

            foreach (var tfv in doc.Descendants(ns + "TargetFrameworkVersion"))
                Add($".NETFramework,Version={tfv.Value.Trim()}");

            foreach (var tf in doc.Descendants(ns + "TargetFramework"))
                Add(tf.Value.Trim());

            foreach (var tfs in doc.Descendants(ns + "TargetFrameworks"))
            foreach (var tf in tfs.Value.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
                Add(tf);
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: failed to read TFM from {Project}: {Error}", projectPath, ex.Message);
        }
        return frameworks;

        void Add(string moniker)
        {
            if (string.IsNullOrEmpty(moniker))
                return;
            var framework = NuGetFramework.Parse(moniker);
            if (!framework.IsUnsupported && !frameworks.Contains(framework))
                frameworks.Add(framework);
        }
    }

    #endregion
}
