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

namespace OpenRewrite.CSharp.NuGet;

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
    /// <summary>
    /// Replacement NuGet feeds for defunct dotnet.myget.org sources.
    /// MyGet was shut down; packages migrated to Azure DevOps Artifacts (dnceng).
    /// </summary>
    private static readonly string[] AdditionalNuGetSources =
    {
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/dotnet-public/nuget/v3/index.json",
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/dotnet-tools/nuget/v3/index.json",
        "https://pkgs.dev.azure.com/dnceng/public/_packaging/myget-legacy/nuget/v3/index.json"
    };

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

        public override void Log(ILogMessage message) =>
            Serilog.Log.Debug("NuGet: {Message}", message.Message);

        public override Task LogAsync(ILogMessage message)
        {
            Log(message);
            return Task.CompletedTask;
        }
    }

    public static ILogger Logger => SerilogNuGetLogger.Instance;

    public static ISettings LoadSettings(string startDirectory) =>
        Settings.LoadDefaultSettings(startDirectory, null, new XPlatMachineWideSetting());

    #region Restore graph generation (PackageReference projects)

    /// <summary>
    /// Produces the restore dependency graph for a solution or project by running the
    /// <c>GenerateRestoreGraphFile</c> MSBuild target in-process for each project.
    /// Returns null when no project produced a graph (e.g. all projects are packages.config-only).
    /// </summary>
    public static DependencyGraphSpec? CreateDependencyGraphSpec(
        string path,
        IDictionary<string, string>? extraGlobalProperties = null)
    {
        var projects = EnumerateProjects(path).ToList();
        if (projects.Count == 0)
        {
            Log.Debug("NuGetResolver: no MSBuild projects found for {Path}", path);
            return null;
        }

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

    // Serialize graph generation: concurrent SDK msbuild processes contend on obj/ and
    // the NuGet http cache without adding throughput for our one-at-a-time callers.
    private static readonly object BuildGate = new();

    private static readonly TimeSpan GraphGenTimeout = TimeSpan.FromMinutes(5);

    /// <summary>
    /// Runs the <c>GenerateRestoreGraphFile</c> target for a single project via the .NET SDK's
    /// own <c>dotnet msbuild</c> and loads the resulting <see cref="DependencyGraphSpec"/>.
    /// Evaluation deliberately runs in the SDK's process, never ours: loading Microsoft.Build
    /// into this process (MSBuildLocator-style) is fragile — any MSBuild assembly in the app
    /// base defeats the redirection, and the SDK's restore tasks bind their own NuGet assembly
    /// versions. Only *evaluation* happens in the child; dependency resolution and downloads
    /// run in-process via <see cref="RestoreRunner"/>. Returns null on evaluation/target
    /// failure (e.g. a legacy project whose imports cannot be resolved).
    /// </summary>
    private static DependencyGraphSpec? GenerateRestoreGraph(
        string projectPath,
        IDictionary<string, string>? extraGlobalProperties)
    {
        var outputPath = Path.Combine(Path.GetTempPath(),
            "openrewrite-dg-" + Guid.NewGuid().ToString("N")[..8] + ".json");
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
                ["RestoreAdditionalProjectSources"] = string.Join("%3B", AdditionalNuGetSources),
                // Avoid restore-time package imports polluting evaluation
                ["ExcludeRestorePackageImports"] = "true",
            };
            if (extraGlobalProperties != null)
            {
                foreach (var (k, v) in extraGlobalProperties)
                    globalProps[k] = v;
            }

            var psi = new ProcessStartInfo("dotnet")
            {
                WorkingDirectory = Path.GetDirectoryName(projectPath) ?? ".",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            psi.ArgumentList.Add("msbuild");
            psi.ArgumentList.Add(projectPath);
            psi.ArgumentList.Add("-t:GenerateRestoreGraphFile");
            psi.ArgumentList.Add("-nologo");
            psi.ArgumentList.Add("-v:quiet");
            psi.ArgumentList.Add("-nodeReuse:false");
            foreach (var (k, v) in globalProps)
                psi.ArgumentList.Add($"-p:{k}={v}");

            lock (BuildGate)
            {
                using var process = Process.Start(psi);
                if (process == null)
                {
                    Log.Debug("NuGetResolver: failed to start dotnet msbuild for {Project}", projectPath);
                    return null;
                }

                // Read both streams before waiting to avoid pipe-buffer deadlock.
                var stdoutTask = process.StandardOutput.ReadToEndAsync();
                var stderrTask = process.StandardError.ReadToEndAsync();
                if (!process.WaitForExit((int)GraphGenTimeout.TotalMilliseconds))
                {
                    try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
                    Log.Debug("NuGetResolver: GenerateRestoreGraphFile timed out for {Project}", projectPath);
                    return null;
                }

                if (process.ExitCode != 0 || !File.Exists(outputPath))
                {
                    Log.Debug("NuGetResolver: GenerateRestoreGraphFile failed for {Project} (exit {Exit}):\n{Out}\n{Err}",
                        projectPath, process.ExitCode, stdoutTask.Result.Trim(), stderrTask.Result.Trim());
                    return null;
                }
            }

            return DependencyGraphSpec.Load(outputPath);
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
            if (style == ProjectStyle.PackageReference || style == ProjectStyle.DotnetCliTool ||
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
            AllowNoOp = false,
            CacheContext = cacheContext,
            Log = Logger,
            CachingSourceProvider = new CachingSourceProvider(new PackageSourceProvider(settings)),
        };

        var requestProvider = new DependencyGraphSpecRequestProvider(providerCache, restorable, settings);
        var requests = await requestProvider.CreateRequests(restoreArgs);

        var results = await RestoreRunner.RunWithoutCommit(requests, restoreArgs);
        foreach (var pair in results)
        {
            var projectPath = pair.SummaryRequest.Request.Project.RestoreMetadata?.ProjectPath
                              ?? pair.SummaryRequest.Request.Project.FilePath;
            if (!pair.Result.Success)
            {
                Log.Debug("NuGetResolver: restore failed for {Project}", projectPath);
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
            if (projectPath != null && pair.Result.LockFile != null)
                lockFiles[Path.GetFullPath(projectPath)] = pair.Result.LockFile;
        }

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
                        "openrewrite-pcrestore-" + Guid.NewGuid().ToString("N")[..8]),
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
        // Replacement feeds for defunct sources, tried after the configured ones.
        foreach (var url in AdditionalNuGetSources)
            repositories.Add(Repository.Factory.GetCoreV3(url));

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
                    Log.Debug("NuGetResolver: {Package} not available from {Source}: {Error}",
                        identity, repository.PackageSource.Source, ex.Message);
                }
            }

            if (!installed)
                Log.Debug("NuGetResolver: failed to install {Package} from any source", identity);
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
    public static NuGetFramework? ReadLegacyFramework(string projectPath)
    {
        try
        {
            var doc = XDocument.Load(projectPath);
            var ns = doc.Root?.Name.Namespace ?? XNamespace.None;

            var tfv = doc.Descendants(ns + "TargetFrameworkVersion").FirstOrDefault()?.Value?.Trim();
            if (!string.IsNullOrEmpty(tfv))
                return NuGetFramework.Parse($".NETFramework,Version={tfv}");

            var tf = doc.Descendants(ns + "TargetFramework").FirstOrDefault()?.Value?.Trim();
            if (!string.IsNullOrEmpty(tf))
                return NuGetFramework.Parse(tf);

            var tfs = doc.Descendants(ns + "TargetFrameworks").FirstOrDefault()?.Value;
            var first = tfs?.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                .FirstOrDefault();
            if (!string.IsNullOrEmpty(first))
                return NuGetFramework.Parse(first);
        }
        catch (Exception ex)
        {
            Log.Debug("NuGetResolver: failed to read TFM from {Project}: {Error}", projectPath, ex.Message);
        }
        return null;
    }

    #endregion
}
