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
using System.IO.Compression;
using System.Reflection;
using System.Text;

namespace OpenRewrite.Core;

/// <summary>
/// Writes data table rows directly to CSV files (RFC 4180 format), one file per data-table
/// bucket keyed by the data table's file-safe key.
/// <para/>
/// Mirrors the Java <c>org.openrewrite.CsvDataTableStore</c> so that data tables produced by
/// C#-authored recipes (running as an RPC peer) land in the same <c>datatables/</c> directory,
/// in the same format, as the orchestrator's own tables. Supports optional GZIP compression
/// (when the file extension ends in <c>.gz</c>) and static prefix/suffix columns (e.g. repository
/// and organization metadata). Each row is appended as an independent stream member; for GZIP
/// this produces a multi-member archive, which <c>GZIPInputStream</c> reads transparently — so no
/// end-of-run flush/close handshake is required across the RPC boundary.
/// </summary>
public class CsvDataTableStore : IDataTableStore
{
    private readonly string _outputDir;
    private readonly string _fileExtension;
    private readonly IReadOnlyList<KeyValuePair<string, string>> _prefixColumns;
    private readonly IReadOnlyList<KeyValuePair<string, string>> _suffixColumns;
    private readonly bool _gzip;
    private readonly HashSet<string> _initializedTables = [];
    private readonly Dictionary<string, int> _rowCounts = new();
    private readonly Dictionary<string, object> _dataTables = new();
    private readonly Dictionary<Type, PropertyInfo[]> _propertyCache = new();
    private readonly object _lock = new();

    /// <summary>
    /// Create a store that writes plain (uncompressed) CSV files with no prefix/suffix columns.
    /// </summary>
    public CsvDataTableStore(string outputDir)
        : this(outputDir, ".csv", [], [])
    {
    }

    /// <summary>
    /// Create a store matching an orchestrator-configured layout: GZIP when
    /// <paramref name="fileExtension"/> ends in <c>.gz</c>, with static columns prepended and
    /// appended to every row (and to the header).
    /// </summary>
    public CsvDataTableStore(
        string outputDir,
        string fileExtension,
        IReadOnlyList<KeyValuePair<string, string>> prefixColumns,
        IReadOnlyList<KeyValuePair<string, string>> suffixColumns)
    {
        _outputDir = outputDir;
        _fileExtension = fileExtension;
        _prefixColumns = prefixColumns;
        _suffixColumns = suffixColumns;
        _gzip = fileExtension.EndsWith(".gz", StringComparison.OrdinalIgnoreCase);
        Directory.CreateDirectory(outputDir);
    }

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        var fileKey = FileKey(dataTable);
        var csvPath = Path.Combine(_outputDir, fileKey + _fileExtension);

        lock (_lock)
        {
            if (_initializedTables.Add(fileKey))
            {
                _rowCounts[fileKey] = 0;
                _dataTables[fileKey] = dataTable;

                var headers = _prefixColumns.Select(c => c.Key)
                    .Concat(dataTable.Descriptor.Columns.Select(c => c.DisplayName))
                    .Concat(_suffixColumns.Select(c => c.Key))
                    .Select(EscapeCsv);
                var comments = $"# @name {dataTable.Name}\n# @instanceName {dataTable.InstanceName}\n# @group {dataTable.Group ?? ""}\n";
                AppendMember(csvPath, comments + string.Join(",", headers) + "\n");
            }

            var properties = GetCachedProperties(typeof(TRow), dataTable.Descriptor);
            var values = _prefixColumns.Select(c => (object?)c.Value)
                .Concat(properties.Select(p => p.GetValue(row)))
                .Concat(_suffixColumns.Select(c => (object?)c.Value))
                .Select(EscapeCsv);
            AppendMember(csvPath, string.Join(",", values) + "\n");
            _rowCounts[fileKey]++;
        }
    }

    private void AppendMember(string path, string text)
    {
        var bytes = Encoding.UTF8.GetBytes(text);
        using var fs = new FileStream(path, FileMode.Append, FileAccess.Write, FileShare.Read);
        if (_gzip)
        {
            using var gz = new GZipStream(fs, CompressionMode.Compress);
            gz.Write(bytes, 0, bytes.Length);
        }
        else
        {
            fs.Write(bytes, 0, bytes.Length);
        }
    }

    public IEnumerable<object> GetRows(string dataTableName, string? group)
    {
        // CSV store writes to disk; reading back is not supported in this impl
        return [];
    }

    public IReadOnlyList<object> GetDataTables()
    {
        lock (_lock)
        {
            return _dataTables.Values.ToList();
        }
    }

    /// <summary>
    /// Number of rows written for each data table.
    /// </summary>
    public IReadOnlyDictionary<string, int> RowCounts => _rowCounts;

    /// <summary>
    /// File-safe keys of all data tables that have been written to.
    /// </summary>
    public IReadOnlyList<string> TableKeys => _initializedTables.ToList();

    private PropertyInfo[] GetCachedProperties(Type rowType, DataTableDescriptor descriptor)
    {
        if (!_propertyCache.TryGetValue(rowType, out var properties))
        {
            properties = descriptor.Columns
                .Select(c => rowType.GetProperty(c.Name, BindingFlags.Public | BindingFlags.Instance)!)
                .ToArray();
            _propertyCache[rowType] = properties;
        }

        return properties;
    }

    internal static string EscapeCsv(object? value)
    {
        if (value is null) return "\"\"";
        var s = value.ToString() ?? "";
        if (s.Contains(',') || s.Contains('"') || s.Contains('\n') || s.Contains('\r'))
            return "\"" + s.Replace("\"", "\"\"") + "\"";
        return s;
    }

    public static string FileKey<TRow>(DataTable<TRow> dataTable) where TRow : notnull
    {
        var suffix = dataTable.Group ?? dataTable.InstanceName;
        return suffix == dataTable.Name ? dataTable.Name : $"{dataTable.Name}--{Sanitize(suffix)}";
    }

    public static string Sanitize(string value)
    {
        var s = value.ToLower();
        var sb = new StringBuilder(s.Length);
        foreach (var c in s)
            sb.Append(char.IsLetterOrDigit(c) ? c : '-');
        var collapsed = new StringBuilder();
        var lastWasDash = true;
        foreach (var c in sb.ToString())
        {
            if (c == '-')
            {
                if (!lastWasDash) collapsed.Append('-');
                lastWasDash = true;
            }
            else
            {
                collapsed.Append(c);
                lastWasDash = false;
            }
        }
        var prefix = collapsed.ToString().TrimEnd('-');
        if (prefix.Length > 30)
        {
            prefix = prefix[..30];
            var lastDash = prefix.LastIndexOf('-');
            if (lastDash > 0) prefix = prefix[..lastDash];
        }
        var hash = Sha256Prefix(value, 4);
        return $"{prefix}-{hash}";
    }

    private static string Sha256Prefix(string input, int hexChars)
    {
        var hash = System.Security.Cryptography.SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(input));
        var hex = Convert.ToHexString(hash).ToLower();
        return hex[..Math.Min(hexChars, hex.Length)];
    }
}
