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
using System.Reflection;
using System.Text;

namespace OpenRewrite.Core;

/// <summary>
/// Writes data table rows directly to CSV files (RFC 4180 format).
/// Each data table bucket is written to a separate CSV file using the
/// data table's file-safe key as the filename.
/// </summary>
public class CsvDataTableStore : IDataTableStore
{
    private readonly string _outputDir;
    private readonly IReadOnlyDictionary<string, string> _prefixColumns;
    private readonly IReadOnlyDictionary<string, string> _suffixColumns;
    private readonly Dictionary<string, int> _rowCounts = new();
    private readonly Dictionary<string, object> _dataTables = new();
    private readonly Dictionary<Type, PropertyInfo[]> _propertyCache = new();
    private readonly object _lock = new();

    public CsvDataTableStore(string outputDir)
        : this(outputDir, new Dictionary<string, string>(), new Dictionary<string, string>())
    {
    }

    /// <summary>
    /// Raw (uncompressed) CSV so complete-line appends from multiple writers sharing one
    /// file per table interleave into one valid file.
    /// </summary>
    public CsvDataTableStore(string outputDir,
        IReadOnlyDictionary<string, string> prefixColumns,
        IReadOnlyDictionary<string, string> suffixColumns)
    {
        _outputDir = outputDir;
        _prefixColumns = prefixColumns;
        _suffixColumns = suffixColumns;
        Directory.CreateDirectory(outputDir);
    }

    public IReadOnlyDictionary<string, string> PrefixColumns => _prefixColumns;

    public IReadOnlyDictionary<string, string> SuffixColumns => _suffixColumns;

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        var fileKey = FileKey(dataTable);
        var csvPath = Path.Combine(_outputDir, fileKey + ".csv");

        lock (_lock)
        {
            _dataTables[fileKey] = dataTable;
            var properties = GetCachedProperties(typeof(TRow), dataTable.Descriptor);

            // Header decided by file existence (not in-memory state) and keyed on field NAMES,
            // so writers sharing one file with the Java host write it once and stay aligned.
            if (!File.Exists(csvPath))
            {
                var headers = _prefixColumns.Keys
                    .Concat(dataTable.Descriptor.Columns.Select(c => c.Name))
                    .Concat(_suffixColumns.Keys)
                    .Select(EscapeCsv);
                var comments = $"# @name {dataTable.Name}\n# @instanceName {dataTable.InstanceName}\n# @group {dataTable.Group ?? ""}\n";
                File.WriteAllText(csvPath, comments + string.Join(",", headers) + "\n");
            }

            var values = _prefixColumns.Values
                .Select(v => (object?)v)
                .Concat(properties.Select(p => p.GetValue(row)))
                .Concat(_suffixColumns.Values.Select(v => (object?)v))
                .Select(EscapeCsv);
            File.AppendAllText(csvPath, string.Join(",", values) + "\n");
            _rowCounts[fileKey] = _rowCounts.GetValueOrDefault(fileKey) + 1;
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
    public IReadOnlyDictionary<string, int> RowCounts
    {
        get
        {
            lock (_lock)
            {
                return new Dictionary<string, int>(_rowCounts);
            }
        }
    }

    /// <summary>
    /// File-safe keys of all data tables that have been written to.
    /// </summary>
    public IReadOnlyList<string> TableKeys
    {
        get
        {
            lock (_lock)
            {
                return _dataTables.Keys.ToList();
            }
        }
    }

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
        // Must mirror the Java host's fileKey exactly, or a table shared by a Java and a C# recipe resolves to two files.
        var group = dataTable.Group;
        if (group != null)
        {
            return group == dataTable.Name ? dataTable.Name : $"{dataTable.Name}--{Sanitize(group)}";
        }
        return dataTable.InstanceName == dataTable.Descriptor.DisplayName
            ? dataTable.Name
            : $"{dataTable.Name}--{Sanitize(dataTable.InstanceName)}";
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
