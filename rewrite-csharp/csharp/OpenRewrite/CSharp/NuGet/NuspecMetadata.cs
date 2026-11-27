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
using System.Collections.Concurrent;
using NuGet.Packaging;
using NuGet.Versioning;
using Serilog;

namespace OpenRewrite.CSharp.NuGet;

/// <summary>
/// Where a restored package says its source lives: the <c>&lt;repository&gt;</c> element and the
/// <c>&lt;projectUrl&gt;</c> of its <c>.nuspec</c>. A value the nuspec leaves out or leaves empty is null.
/// </summary>
internal sealed record NuspecMetadata(
    string? RepositoryUrl,
    string? RepositoryType,
    string? RepositoryCommit,
    string? ProjectUrl)
{
    // A package version's nuspec never changes once restored and every project of a solution
    // references mostly the same packages, so each file is read once and kept by its path.
    private static readonly ConcurrentDictionary<string, Lazy<NuspecMetadata?>> Cache = new(StringComparer.Ordinal);
    private static readonly ConcurrentDictionary<string, int> ReadCounts = new(StringComparer.Ordinal);

    /// <summary>Nuspec files read from paths under <paramref name="directory"/>.</summary>
    public static long ReadsUnder(string directory)
    {
        var prefix = Path.GetFullPath(directory);
        return ReadCounts
            .Where(kv => kv.Key.StartsWith(prefix, StringComparison.Ordinal))
            .Sum(kv => (long)kv.Value);
    }

    /// <summary>
    /// The metadata of the package's nuspec in the first of <paramref name="packageFolders"/> that
    /// holds one, or null when none does or the nuspec cannot be read.
    /// </summary>
    public static NuspecMetadata? Find(string id, NuGetVersion version, IEnumerable<string> packageFolders)
    {
        foreach (var folder in packageFolders)
        {
            if (string.IsNullOrEmpty(folder))
                continue;
            var path = Path.GetFullPath(new VersionFolderPathResolver(folder).GetManifestFilePath(id, version));
            if (Cache.TryGetValue(path, out var cached))
                return cached.Value;
            if (File.Exists(path))
                return Cache.GetOrAdd(path, p => new Lazy<NuspecMetadata?>(() => Read(p))).Value;
        }
        return null;
    }

    private static NuspecMetadata? Read(string path)
    {
        ReadCounts.AddOrUpdate(path, 1, (_, count) => count + 1);
        try
        {
            var nuspec = new NuspecReader(path);
            var repository = nuspec.GetRepositoryMetadata();
            return new NuspecMetadata(
                NullIfEmpty(repository?.Url),
                NullIfEmpty(repository?.Type),
                NullIfEmpty(repository?.Commit),
                NullIfEmpty(nuspec.GetProjectUrl()));
        }
        catch (Exception ex)
        {
            Log.Debug("NuspecMetadata: could not read {Path}: {Error}", path, ex.Message);
            return null;
        }
    }

    private static string? NullIfEmpty(string? value) => string.IsNullOrWhiteSpace(value) ? null : value.Trim();
}
