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
using Serilog;

namespace OpenRewrite.CSharp;

/// <summary>
/// Thin wrapper over the host <c>git</c> executable, used in place of an embedded
/// git library so the tool ships no per-platform native binaries. The parser only
/// ever runs against repositories already on disk in a developer/CI environment,
/// where <c>git</c> is invariably on the PATH; every method degrades gracefully
/// (returns "not a repo" / empty) when git is absent or errors, so the caller
/// behaves exactly as it did when the working tree was not a git repository.
/// </summary>
internal static class GitCli
{
    private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(30);

    /// <summary>
    /// Returns the absolute path of the working tree containing <paramref name="startDir"/>,
    /// or <c>null</c> when it is not inside a git repository (or git is unavailable).
    /// Equivalent to <c>git rev-parse --show-toplevel</c>.
    /// </summary>
    public static string? DiscoverWorkTree(string startDir)
    {
        var (exit, stdout, _) = Run(startDir, new[] { "rev-parse", "--show-toplevel" });
        if (exit != 0)
            return null;
        var top = stdout.Trim();
        return string.IsNullOrEmpty(top) ? null : top;
    }

    /// <summary>
    /// Whether the repository has a resolvable <c>HEAD</c> commit. False on an unborn
    /// branch (no commits yet), where there is nothing tracked to restore.
    /// </summary>
    public static bool HasHead(string workDir)
    {
        var (exit, _, _) = Run(workDir, new[] { "rev-parse", "--verify", "--quiet", "HEAD" });
        return exit == 0;
    }

    /// <summary>
    /// Repository-relative paths of tracked files whose final path segment equals
    /// <paramref name="fileName"/>. Mirrors walking the git index filtered by name.
    /// </summary>
    public static IReadOnlyList<string> ListTrackedByName(string workDir, string fileName)
    {
        // -z: NUL-separated, so paths with spaces/newlines survive intact and no quoting is applied.
        var (exit, stdout, _) = Run(workDir, new[] { "ls-files", "-z" });
        if (exit != 0)
            return Array.Empty<string>();
        var result = new List<string>();
        foreach (var rel in stdout.Split('\0', StringSplitOptions.RemoveEmptyEntries))
        {
            if (string.Equals(PosixBaseName(rel), fileName, StringComparison.OrdinalIgnoreCase))
                result.Add(rel);
        }
        return result;
    }

    /// <summary>
    /// Subset of <paramref name="relPaths"/> (repository-relative, forward-slash) that git
    /// considers ignored. Uses <c>git check-ignore</c> in its default (index-aware) mode, so
    /// — exactly like the previous LibGit2Sharp logic — a tracked file is never reported as
    /// ignored even when an ignore rule would otherwise match it.
    /// </summary>
    public static HashSet<string> CheckIgnored(string workDir, IReadOnlyCollection<string> relPaths)
    {
        var ignored = new HashSet<string>(StringComparer.Ordinal);
        if (relPaths.Count == 0)
            return ignored;

        // Feed paths on stdin (NUL-delimited) to avoid command-line length limits and quoting.
        var stdin = string.Join('\0', relPaths) + '\0';
        var (exit, stdout, _) = Run(workDir, new[] { "check-ignore", "--stdin", "-z" }, stdin);

        // git check-ignore exit codes: 0 = one or more paths ignored, 1 = none ignored,
        // 128 = fatal error. Treat anything other than 0 as "nothing ignored".
        if (exit != 0)
            return ignored;

        foreach (var rel in stdout.Split('\0', StringSplitOptions.RemoveEmptyEntries))
            ignored.Add(rel);
        return ignored;
    }

    /// <summary>
    /// Force-restores the committed (HEAD) version of <paramref name="relPath"/> into the
    /// working tree. Equivalent to <c>git checkout --force HEAD -- &lt;path&gt;</c>.
    /// </summary>
    public static bool RestoreFromHead(string workDir, string relPath)
    {
        var (exit, _, _) = Run(workDir, new[] { "checkout", "--force", "HEAD", "--", relPath });
        return exit == 0;
    }

    private static string PosixBaseName(string path)
    {
        var slash = path.LastIndexOf('/');
        return slash < 0 ? path : path[(slash + 1)..];
    }

    private static (int ExitCode, string Stdout, string Stderr) Run(string workDir, IReadOnlyList<string> args, string? stdin = null)
    {
        try
        {
            var psi = new ProcessStartInfo("git")
            {
                WorkingDirectory = workDir,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                RedirectStandardInput = stdin != null,
                UseShellExecute = false,
                CreateNoWindow = true
            };
            foreach (var a in args)
                psi.ArgumentList.Add(a);

            using var process = Process.Start(psi);
            if (process == null)
                return (-1, "", "");

            // Read stdout/stderr concurrently with the write below to avoid pipe-buffer deadlock.
            var stdoutTask = process.StandardOutput.ReadToEndAsync();
            var stderrTask = process.StandardError.ReadToEndAsync();

            if (stdin != null)
            {
                process.StandardInput.Write(stdin);
                process.StandardInput.Close();
            }

            if (!process.WaitForExit((int)Timeout.TotalMilliseconds))
            {
                try { process.Kill(entireProcessTree: true); } catch { /* best effort */ }
                Log.Debug("git {Args}: timed out after {Timeout}", string.Join(' ', args), Timeout);
                return (-1, "", "");
            }

            return (process.ExitCode, stdoutTask.GetAwaiter().GetResult(), stderrTask.GetAwaiter().GetResult());
        }
        catch (Exception ex)
        {
            // git not on PATH, or spawn failure: behave as "not a git repository".
            Log.Debug("git {Args}: failed ({ExType}: {ExMessage})", string.Join(' ', args), ex.GetType().Name, ex.Message);
            return (-1, "", "");
        }
    }
}
