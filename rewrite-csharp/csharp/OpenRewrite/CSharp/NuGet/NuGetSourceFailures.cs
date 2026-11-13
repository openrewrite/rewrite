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
using System.Text.RegularExpressions;
using NuGet.Common;
using Serilog;

namespace OpenRewrite.CSharp.NuGet;

/// <summary>
/// Collapses NuGet's per-package, per-project source diagnostics into one grouped summary per
/// package source. An unreachable feed otherwise emits an "Unable to load the service index"
/// line for every package times every project — hundreds of identical lines that bury the
/// diagnostics worth acting on.
/// <para>
/// While a scope is open, every warning or error naming a package source is withheld and
/// replayed as a single per-endpoint summary: one verbatim example with its occurrence count,
/// the affected packages and projects, and whether the source was actually needed to resolve a
/// dependency (a feed nothing depended on being unreachable is noise; one that blocked a package
/// is not). Informational HTTP trace and messages that name no source pass through untouched.
/// </para>
/// </summary>
internal sealed class NuGetSourceFailures : IDisposable
{
    private const int MaxEndpointsReported = 10;
    private const int MaxExamplesPerEndpoint = 5;
    private const int MaxIdsListed = 10;
    private const int MaxIdsTracked = 500;

    private static readonly Regex UrlPattern =
        new(@"https?://[^\s'""<>|,;)\]]+", RegexOptions.Compiled | RegexOptions.IgnoreCase);

    private static readonly Regex PathPattern =
        new(@"(?:[a-zA-Z]:[\\/]|(?<![\w.])[\\/])(?:[^\s'""<>|*?,;()\[\]]*[\\/])+[^\s'""<>|*?,;()\[\]]*",
            RegexOptions.Compiled);

    private static readonly Regex WhitespacePattern = new(@"\s+", RegexOptions.Compiled);

    private static readonly HashSet<NuGetLogCode> UnresolvedCodes =
    [
        NuGetLogCode.NU1100, NuGetLogCode.NU1101, NuGetLogCode.NU1102, NuGetLogCode.NU1103,
    ];

    private static readonly object Sync = new();
    private static NuGetSourceFailures? _current;

    private readonly object _lock = new();
    private readonly string _context;
    private readonly IReadOnlyList<string> _sources;
    private readonly Dictionary<string, Endpoint> _endpoints = new(StringComparer.OrdinalIgnoreCase);
    private readonly SortedSet<string> _unresolved = new(StringComparer.OrdinalIgnoreCase);

    internal NuGetSourceFailures(string context, IReadOnlyList<string> sources)
    {
        _context = context;
        _sources = sources;
    }

    /// <summary>
    /// Opens a collection scope covering one restore. Nested calls join the outermost scope, so
    /// the summary is written once when that scope is disposed. <paramref name="sources"/> are
    /// the configured package sources, used to attribute a resource URL to the feed it belongs
    /// to (a flat-container URL shares only a prefix with the feed's service index).
    /// </summary>
    public static IDisposable Begin(string context, IReadOnlyList<string> sources)
    {
        lock (Sync)
        {
            if (_current != null)
                return NestedScope.Instance;
            _current = new NuGetSourceFailures(context, sources);
            return _current;
        }
    }

    public void Dispose()
    {
        lock (Sync)
        {
            if (!ReferenceEquals(_current, this))
                return;
            _current = null;
        }
        WriteSummary();
    }

    private sealed class NestedScope : IDisposable
    {
        public static readonly NestedScope Instance = new();
        public void Dispose() { }
    }

    private static NuGetSourceFailures? Current
    {
        get { lock (Sync) return _current; }
    }

    /// <summary>
    /// Offers a NuGet log message to the open scope. Returns true when the message was absorbed
    /// into an endpoint group and must not be logged individually.
    /// </summary>
    public static bool TryRecord(ILogMessage message) => Current?.Record(message) == true;

    internal bool Record(ILogMessage message)
    {
        var text = message.Message ?? string.Empty;
        var libraryId = (message as IRestoreLogMessage)?.LibraryId;

        if (UnresolvedCodes.Contains(message.Code) && !string.IsNullOrEmpty(libraryId))
            MarkUnresolved(libraryId);

        if (message.Level < LogLevel.Warning)
            return false;

        var endpoints = new List<string>();
        var template = Collapse(text, libraryId, _sources, endpoints);
        if (endpoints.Count == 0)
            return false;

        var absorbed = false;
        foreach (var endpoint in endpoints)
            absorbed |= Add(endpoint, message.Level, message.Code, text, libraryId, message.ProjectPath, template);
        return absorbed;
    }

    internal bool RecordSourceFailure(string source, string? packageId, string reason)
    {
        if (string.IsNullOrEmpty(source))
            return false;
        return Add(EndpointOf(source), LogLevel.Warning, NuGetLogCode.Undefined,
            $"{packageId} is not available from {source}: {reason}", packageId, projectPath: null);
    }

    internal void MarkUnresolved(string packageId)
    {
        lock (_lock)
        {
            if (_unresolved.Count < MaxIdsTracked)
                _unresolved.Add(packageId);
        }
    }

    /// <summary>
    /// Records a per-source failure raised outside NuGet's own logger (the flat install path
    /// walks repositories itself). Returns true when it was absorbed into an endpoint group.
    /// </summary>
    public static bool TryRecordSourceFailure(string source, string? packageId, string reason) =>
        Current?.RecordSourceFailure(source, packageId, reason) == true;

    /// <summary>Marks a package as unresolvable from any source.</summary>
    public static void RecordUnresolved(string packageId)
    {
        if (!string.IsNullOrEmpty(packageId))
            Current?.MarkUnresolved(packageId);
    }

    private bool Add(string endpoint, LogLevel level, NuGetLogCode code, string text,
        string? libraryId, string? projectPath, string? template = null)
    {
        lock (_lock)
        {
            if (!_endpoints.TryGetValue(endpoint, out var group))
            {
                group = new Endpoint();
                _endpoints[endpoint] = group;
            }
            group.Add(level, code, text, template ?? Collapse(text, libraryId, _sources, null),
                libraryId, projectPath);
            return true;
        }
    }

    private void WriteSummary()
    {
        var lines = BuildSummary();
        if (lines.Count == 0)
            return;
        var actionable = AnythingUnresolved();
        foreach (var line in lines)
        {
            if (actionable)
                Log.Warning("{SourceFailureSummary}", line);
            else
                Log.Debug("{SourceFailureSummary}", line);
        }
    }

    private bool AnythingUnresolved()
    {
        lock (_lock)
            return _unresolved.Count > 0;
    }

    internal IReadOnlyList<string> BuildSummary()
    {
        lock (_lock)
        {
            var lines = new List<string>();
            if (_endpoints.Count == 0)
                return lines;

            var total = _endpoints.Values.Sum(e => e.Total);
            lines.Add($"NuGet: {total} package source diagnostic(s) from {_endpoints.Count} source(s) " +
                      $"while restoring {_context}; grouped by endpoint below");

            var ordered = _endpoints.OrderByDescending(p => p.Value.Total)
                .ThenBy(p => p.Key, StringComparer.OrdinalIgnoreCase).ToList();
            foreach (var (endpoint, group) in ordered.Take(MaxEndpointsReported))
            {
                var blocking = group.Packages.Intersect(_unresolved, StringComparer.OrdinalIgnoreCase)
                    .OrderBy(id => id, StringComparer.OrdinalIgnoreCase).ToList();
                string verdict;
                if (_unresolved.Count == 0)
                    verdict = "unused - every package resolved from another source";
                else if (blocking.Count > 0)
                    verdict = $"REQUIRED - blocked {blocking.Count} package(s): {Join(blocking)}";
                else if (group.Packages.Count == 0)
                    verdict = $"possibly required - {_unresolved.Count} package(s) resolved from no " +
                              $"source: {Join(_unresolved)}";
                else
                    verdict = $"not required - none of the {_unresolved.Count} unresolved package(s) " +
                              "were looked up here";

                lines.Add($"  source {endpoint}: {group.Total} diagnostic(s), {verdict}");
                foreach (var example in group.Examples.Values
                             .OrderByDescending(e => e.Count).ThenBy(e => e.Text, StringComparer.Ordinal)
                             .Take(MaxExamplesPerEndpoint))
                {
                    lines.Add($"    [{example.Level} {example.Code}] x{example.Count}: {example.Text}");
                }
                if (group.Examples.Count > MaxExamplesPerEndpoint)
                    lines.Add($"    ... and {group.Examples.Count - MaxExamplesPerEndpoint} more distinct message(s)");
                if (group.Packages.Count > 0)
                    lines.Add($"    affected packages ({group.Packages.Count}): {Join(group.Packages)}");
                if (group.Projects.Count > 0)
                    lines.Add($"    affected projects ({group.Projects.Count}): " +
                              $"{Join(group.Projects.Select(p => Path.GetFileName(p) ?? p))}");
            }
            if (ordered.Count > MaxEndpointsReported)
                lines.Add($"  ... and {ordered.Count - MaxEndpointsReported} more source(s)");
            return lines;
        }
    }

    private static string Join(IEnumerable<string> ids)
    {
        var list = ids.Take(MaxIdsListed + 1).ToList();
        return list.Count > MaxIdsListed
            ? string.Join(", ", list.Take(MaxIdsListed)) + ", ..."
            : string.Join(", ", list);
    }

    internal static string Collapse(string message, string? libraryId,
        IReadOnlyList<string>? sources, ICollection<string>? endpoints)
    {
        var template = UrlPattern.Replace(message, match =>
        {
            if (endpoints != null)
            {
                var endpoint = EndpointOf(match.Value, sources);
                if (!endpoints.Contains(endpoint, StringComparer.OrdinalIgnoreCase))
                    endpoints.Add(endpoint);
            }
            return "{source}";
        });
        template = PathPattern.Replace(template, "{path}");
        if (!string.IsNullOrEmpty(libraryId))
            template = template.Replace(libraryId, "{package}", StringComparison.OrdinalIgnoreCase);
        return WhitespacePattern.Replace(template, " ").Trim();
    }

    private string EndpointOf(string url) => EndpointOf(url, _sources);

    private static string EndpointOf(string url, IReadOnlyList<string>? sources)
    {
        string? best = null;
        var bestLength = 0;
        foreach (var source in sources ?? [])
        {
            var shared = CommonPrefixLength(url, source);
            if (shared > bestLength && shared >= AuthorityLength(source))
            {
                bestLength = shared;
                best = source;
            }
        }
        if (best != null)
            return best;
        return Uri.TryCreate(url, UriKind.Absolute, out var uri)
            ? uri.GetLeftPart(UriPartial.Authority)
            : url;
    }

    private static int CommonPrefixLength(string a, string b)
    {
        var max = Math.Min(a.Length, b.Length);
        var i = 0;
        while (i < max && char.ToLowerInvariant(a[i]) == char.ToLowerInvariant(b[i]))
            i++;
        return i;
    }

    private static int AuthorityLength(string source) =>
        Uri.TryCreate(source, UriKind.Absolute, out var uri) && uri.IsAbsoluteUri && !uri.IsFile
            ? uri.GetLeftPart(UriPartial.Authority).Length
            : int.MaxValue;

    private sealed class Endpoint
    {
        public int Total { get; private set; }
        public Dictionary<string, Example> Examples { get; } = new(StringComparer.Ordinal);
        public SortedSet<string> Packages { get; } = new(StringComparer.OrdinalIgnoreCase);
        public SortedSet<string> Projects { get; } = new(StringComparer.OrdinalIgnoreCase);

        public void Add(LogLevel level, NuGetLogCode code, string text, string template,
            string? libraryId, string? projectPath)
        {
            Total++;
            if (Examples.TryGetValue(template, out var example))
            {
                example.Count++;
                if (level > example.Level)
                {
                    example.Level = level;
                    example.Code = code;
                }
            }
            else if (Examples.Count < MaxIdsTracked)
            {
                Examples[template] = new Example { Level = level, Code = code, Text = text, Count = 1 };
            }
            if (!string.IsNullOrEmpty(libraryId) && Packages.Count < MaxIdsTracked)
                Packages.Add(libraryId);
            if (!string.IsNullOrEmpty(projectPath) && Projects.Count < MaxIdsTracked)
                Projects.Add(projectPath);
        }
    }

    private sealed class Example
    {
        public LogLevel Level { get; set; }
        public NuGetLogCode Code { get; set; }
        public required string Text { get; init; }
        public int Count { get; set; }
    }
}
