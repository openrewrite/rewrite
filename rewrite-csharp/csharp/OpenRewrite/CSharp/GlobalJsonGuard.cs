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
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using OpenRewrite.Core;
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Temporarily strips the <c>sdk</c> section from <c>global.json</c> files for the duration of a
/// solution parse so that <c>dotnet restore</c> and MSBuildWorkspace resolve the latest installed
/// .NET SDK instead of the version the repository pins.
/// <para>
/// We always want to ingest source with the newest SDK (the C# compiler is backwards
/// compatible), but a repo that pins an uninstalled SDK band — or pins with
/// <c>rollForward: disable</c>/<c>latestPatch</c> — makes <c>hostfxr_resolve_sdk2</c> fail
/// outright, aborting the whole build. There is no environment variable or CLI flag that
/// overrides <c>global.json</c> SDK resolution, and the resolution keys off the project's
/// directory (not the process working directory), so the only reliable fix is to rewrite the
/// file while we load the workspace and put it back afterwards.
/// </para>
/// <para>
/// Only the <c>sdk</c> section is removed: <c>msbuild-sdks</c> (which pins versionless project
/// SDKs such as <c>Microsoft.DotNet.Arcade.Sdk</c> or <c>MSTest.Sdk</c>) and every other section
/// are kept so those projects still evaluate. Files without an <c>sdk</c> section are left
/// untouched; files that cannot be parsed as JSON are removed instead.
/// </para>
/// <para>
/// The original bytes are held in memory and rewritten verbatim on <see cref="Dispose"/> so the
/// working tree is left byte-for-byte unchanged. Rewrites are reference-counted per file so
/// that concurrent parses sharing a repo-root <c>global.json</c> (e.g. a repo with multiple
/// solutions) coordinate correctly: the file is restored only when the last guard releases it.
/// </para>
/// <para>
/// As a crash-safety net, <see cref="Dispose"/> also asks git to restore any tracked
/// <c>global.json</c> that is missing from the working tree or still carries the neutralization
/// marker — this recovers files a previously crashed run changed and never restored. Files
/// currently held by an active guard are skipped so recovery never fights an in-flight
/// neutralization.
/// </para>
/// </summary>
internal sealed class GlobalJsonGuard : IDisposable
{
    private const string FileName = "global.json";

    internal const string Marker = "openrewrite-neutralized-sdk";

    private static readonly JsonDocumentOptions ReadOptions = new()
    {
        CommentHandling = JsonCommentHandling.Skip,
        AllowTrailingCommas = true
    };

    private static readonly JsonSerializerOptions WriteOptions = new() { WriteIndented = true };

    private static readonly object Sync = new();

    /// <summary>Per-file state shared across overlapping guards, keyed by full path.</summary>
    private sealed class Entry
    {
        public int RefCount;
        public byte[] Bytes = Array.Empty<byte>();
        public byte[]? Neutralized;
    }

    private static readonly Dictionary<string, Entry> Active = new(StringComparer.OrdinalIgnoreCase);

    private readonly string _rootDir;
    private readonly List<string> _heldPaths = new();
    private bool _disposed;

    private GlobalJsonGuard(string rootDir) => _rootDir = rootDir;

    /// <summary>
    /// Strips the <c>sdk</c> section from every <c>global.json</c> under <paramref name="rootDir"/>
    /// (excluding bin/obj), returning a guard that restores the original files when disposed.
    /// </summary>
    public static GlobalJsonGuard Neutralize(string rootDir)
    {
        var guard = new GlobalJsonGuard(rootDir);
        foreach (var file in FindGlobalJsonFiles(rootDir))
        {
            try
            {
                lock (Sync)
                {
                    if (Active.TryGetValue(file, out var entry))
                    {
                        entry.RefCount++;
                        guard._heldPaths.Add(file);
                        continue;
                    }

                    if (!File.Exists(file))
                        continue;

                    var bytes = File.ReadAllBytes(file);
                    if (!TryNeutralize(bytes, out var neutralized))
                        continue;

                    if (neutralized == null)
                    {
                        File.Delete(file);
                        Log.Debug("global.json: removed unparseable {File} for the duration of parsing", file);
                    }
                    else
                    {
                        File.WriteAllBytes(file, neutralized);
                        Log.Debug("global.json: removed the sdk pin from {File} for the duration of parsing", file);
                    }
                    Active[file] = new Entry { RefCount = 1, Bytes = bytes, Neutralized = neutralized };
                    guard._heldPaths.Add(file);
                }
            }
            catch (Exception ex)
            {
                Log.Debug("global.json: failed to neutralize {File} ({ExType}: {ExMessage}); leaving in place",
                    file, ex.GetType().Name, ex.Message);
            }
        }
        return guard;
    }

    /// <summary>
    /// Computes the content written while parsing: the original JSON without its <c>sdk</c>
    /// section, plus <see cref="Marker"/>. Returns <c>false</c> when the file has no <c>sdk</c>
    /// section and so needs no change, and <c>true</c> with a <c>null</c> result when the file
    /// cannot be rewritten (not a JSON object, malformed, duplicate keys) and must be removed instead.
    /// </summary>
    internal static bool TryNeutralize(byte[] bytes, out byte[]? neutralized)
    {
        neutralized = null;
        try
        {
            if (JsonNode.Parse(StripBom(bytes), documentOptions: ReadOptions) is not JsonObject root)
                return true;
            if (!root.Remove("sdk"))
                return false;
            root[Marker] = true;
            neutralized = Encoding.UTF8.GetBytes(root.ToJsonString(WriteOptions) + "\n");
            return true;
        }
        catch (Exception ex) when (ex is JsonException or ArgumentException)
        {
            neutralized = null;
            return true;
        }
    }

    private static ReadOnlySpan<byte> StripBom(byte[] bytes) =>
        bytes.AsSpan().StartsWith(Encoding.UTF8.Preamble) ? bytes.AsSpan(Encoding.UTF8.Preamble.Length) : bytes;

    /// <summary>
    /// Restores every file this guard changed (writing the original bytes back verbatim once the
    /// last sharing guard releases it), then recovers any tracked <c>global.json</c> a prior
    /// crashed run left missing or neutralized from git.
    /// </summary>
    public void Dispose()
    {
        if (_disposed)
            return;
        _disposed = true;

        foreach (var file in _heldPaths)
        {
            try
            {
                lock (Sync)
                {
                    if (!Active.TryGetValue(file, out var entry))
                        continue;

                    entry.RefCount--;
                    if (entry.RefCount > 0)
                        continue;

                    Active.Remove(file);

                    if (IsStillNeutralized(file, entry))
                    {
                        File.WriteAllBytes(file, entry.Bytes);
                        Log.Debug("global.json: restored {File}", file);
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Debug("global.json: failed to restore {File} ({ExType}: {ExMessage})",
                    file, ex.GetType().Name, ex.Message);
            }
        }
        _heldPaths.Clear();

        RecoverFromGit(_rootDir);
    }

    private static bool IsStillNeutralized(string file, Entry entry)
    {
        if (!File.Exists(file))
            return true;
        return entry.Neutralized != null && File.ReadAllBytes(file).AsSpan().SequenceEqual(entry.Neutralized);
    }

    private static bool CarriesMarker(string file)
    {
        try
        {
            return File.ReadAllText(file).Contains(Marker, StringComparison.Ordinal);
        }
        catch (IOException)
        {
            return false;
        }
    }

    /// <summary>
    /// Restores <c>global.json</c> files that git tracks but that are missing from the working
    /// tree or still carry <see cref="Marker"/>, and that are not currently held by an active
    /// guard. This recovers files a previously crashed run changed and never restored. No-op when
    /// <paramref name="rootDir"/> is not inside a git repository.
    /// </summary>
    private static void RecoverFromGit(string rootDir)
    {
        try
        {
            var workDir = GitCli.DiscoverWorkTree(rootDir);
            if (workDir == null)
                return; // Not inside a git repository — nothing to recover.

            if (!GitCli.HasHead(workDir))
                return; // Unborn branch (no commits) — nothing is tracked yet.

            var normRoot = RealPath(rootDir);

            // List the tracked global.json files (mirrors `git ls-files` filtered to
            // global.json) rather than scanning the whole working tree: there are only ever a
            // handful, and we touch disk solely to check whether each was left changed.
            foreach (var rel in GitCli.ListTrackedByName(workDir, FileName))
            {
                var full = RealPath(Path.Combine(workDir, rel));

                // Only recover files under the directory we were asked to guard.
                if (!IsUnder(full, normRoot))
                    continue;

                if (File.Exists(full) && !CarriesMarker(full))
                    continue;

                lock (Sync)
                {
                    if (Active.ContainsKey(full))
                        continue; // An active guard changed this on purpose — don't fight it.
                }

                // Force-restore the committed version into the working tree.
                if (GitCli.RestoreFromHead(workDir, rel))
                    Log.Debug("global.json: recovered {Path} from git (left changed by a prior run)", full);
            }
        }
        catch (Exception ex)
        {
            Log.Debug("global.json: git recovery failed for {RootDir} ({ExType}: {ExMessage})",
                rootDir, ex.GetType().Name, ex.Message);
        }
    }

    private static bool IsUnder(string path, string dir)
    {
        var rel = Path.GetRelativePath(dir, path);
        return rel != ".." && !rel.StartsWith(".." + Path.DirectorySeparatorChar, StringComparison.Ordinal)
                           && !Path.IsPathRooted(rel);
    }

    private static string RealPath(string path) => PathUtil.Canonicalize(path);

    private static IEnumerable<string> FindGlobalJsonFiles(string rootDir)
    {
        if (!Directory.Exists(rootDir))
            return Array.Empty<string>();
        try
        {
            return Directory.EnumerateFiles(rootDir, FileName, SearchOption.AllDirectories)
                .Where(f => !IsInBinOrObj(f, rootDir))
                .Select(RealPath)
                .ToList();
        }
        catch (Exception ex)
        {
            Log.Debug("global.json: enumeration failed under {RootDir} ({ExType}: {ExMessage})",
                rootDir, ex.GetType().Name, ex.Message);
            return Array.Empty<string>();
        }
    }

    private static bool IsInBinOrObj(string file, string rootDir)
    {
        var rel = Path.GetRelativePath(rootDir, file);
        return rel.Split(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar)
            .Any(seg => seg.Equals("bin", StringComparison.OrdinalIgnoreCase) ||
                        seg.Equals("obj", StringComparison.OrdinalIgnoreCase));
    }
}
