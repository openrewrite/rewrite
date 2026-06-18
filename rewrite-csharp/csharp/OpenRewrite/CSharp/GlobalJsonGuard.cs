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
using OpenRewrite.Core;
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Temporarily removes <c>global.json</c> files for the duration of a solution parse so that
/// <c>dotnet restore</c> and MSBuildWorkspace resolve the latest installed .NET SDK instead of
/// the version the repository pins.
/// <para>
/// We always want to ingest source with the newest SDK (the C# compiler is backwards
/// compatible), but a repo that pins an uninstalled SDK band — or pins with
/// <c>rollForward: disable</c>/<c>latestPatch</c> — makes <c>hostfxr_resolve_sdk2</c> fail
/// outright, aborting the whole build. There is no environment variable or CLI flag that
/// overrides <c>global.json</c> SDK resolution, and the resolution keys off the project's
/// directory (not the process working directory), so the only reliable fix is to remove the
/// file while we load the workspace and put it back afterwards.
/// </para>
/// <para>
/// The original bytes are held in memory and rewritten verbatim on <see cref="Dispose"/> so the
/// working tree is left byte-for-byte unchanged. Removals are reference-counted per file so
/// that concurrent parses sharing a repo-root <c>global.json</c> (e.g. a repo with multiple
/// solutions) coordinate correctly: the file is restored only when the last guard releases it.
/// </para>
/// <para>
/// As a crash-safety net, <see cref="Dispose"/> also asks git to restore any <c>global.json</c>
/// that is tracked in the repository but missing from the working tree — this recovers files a
/// previously crashed run deleted and never restored. Files currently held by an active guard
/// are skipped so recovery never fights an in-flight neutralization.
/// </para>
/// </summary>
internal sealed class GlobalJsonGuard : IDisposable
{
    private const string FileName = "global.json";

    private static readonly object Sync = new();

    /// <summary>Per-file state shared across overlapping guards, keyed by full path.</summary>
    private sealed class Entry
    {
        public int RefCount;
        public byte[] Bytes = Array.Empty<byte>();
    }

    private static readonly Dictionary<string, Entry> Active = new(StringComparer.OrdinalIgnoreCase);

    private readonly string _rootDir;
    private readonly List<string> _heldPaths = new();
    private bool _disposed;

    private GlobalJsonGuard(string rootDir) => _rootDir = rootDir;

    /// <summary>
    /// Removes every <c>global.json</c> under <paramref name="rootDir"/> (excluding bin/obj),
    /// returning a guard that restores them when disposed.
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
                        // Already removed by another in-flight guard — just share it.
                        entry.RefCount++;
                        guard._heldPaths.Add(file);
                        continue;
                    }

                    if (!File.Exists(file))
                        continue; // Absent (e.g. left over from a prior crash) — git recovery handles it.

                    var bytes = File.ReadAllBytes(file);
                    File.Delete(file);
                    Active[file] = new Entry { RefCount = 1, Bytes = bytes };
                    guard._heldPaths.Add(file);
                    Log.Debug("global.json: removed {File} for the duration of parsing", file);
                }
            }
            catch (Exception ex)
            {
                Log.Debug("global.json: failed to remove {File} ({ExType}: {ExMessage}); leaving in place",
                    file, ex.GetType().Name, ex.Message);
            }
        }
        return guard;
    }

    /// <summary>
    /// Restores every file this guard removed (writing the original bytes back verbatim once the
    /// last sharing guard releases it), then recovers any tracked-but-missing
    /// <c>global.json</c> from git.
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
                        continue; // Another guard still needs it gone.

                    Active.Remove(file);

                    // Only rewrite if still absent so we never clobber a file that something
                    // else (e.g. git recovery) already put back.
                    if (!File.Exists(file))
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

        RecoverDeletedFromGit(_rootDir);
    }

    /// <summary>
    /// Restores <c>global.json</c> files that git tracks but that are missing from the working
    /// tree, and that are not currently held by an active guard. This recovers files a previously
    /// crashed run deleted and never restored. No-op when <paramref name="rootDir"/> is not inside
    /// a git repository.
    /// </summary>
    private static void RecoverDeletedFromGit(string rootDir)
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
            // handful, and we touch disk solely to check whether each is missing.
            foreach (var rel in GitCli.ListTrackedByName(workDir, FileName))
            {
                var full = RealPath(Path.Combine(workDir, rel));

                // Only recover files under the directory we were asked to guard.
                if (!IsUnder(full, normRoot))
                    continue;

                if (File.Exists(full))
                    continue; // Present — not a leftover deletion.

                lock (Sync)
                {
                    if (Active.ContainsKey(full))
                        continue; // An active guard removed this on purpose — don't fight it.
                }

                // Force-restore the committed version into the working tree.
                if (GitCli.RestoreFromHead(workDir, rel))
                    Log.Debug("global.json: recovered {Path} from git (left deleted by a prior run)", full);
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
