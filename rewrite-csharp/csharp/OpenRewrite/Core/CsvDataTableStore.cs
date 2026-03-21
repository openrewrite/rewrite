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

namespace OpenRewrite.Core;

/// <summary>
/// Writes data table rows directly to CSV files (RFC 4180 format).
/// Each data table bucket is written to a separate CSV file using the
/// data table's file-safe key as the filename.
/// </summary>
public class CsvDataTableStore : IDataTableStore
{
    private readonly string _outputDir;
    private readonly HashSet<string> _initializedTables = [];
    private readonly Dictionary<string, int> _rowCounts = new();
    private readonly Dictionary<string, object> _dataTables = new();
    private readonly Dictionary<Type, PropertyInfo[]> _propertyCache = new();

    public CsvDataTableStore(string outputDir)
    {
        _outputDir = outputDir;
        Directory.CreateDirectory(outputDir);
    }

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        var fileKey = FileKey(dataTable);
        var csvPath = Path.Combine(_outputDir, fileKey + ".csv");

        if (!_initializedTables.Contains(fileKey))
        {
            _initializedTables.Add(fileKey);
            _rowCounts[fileKey] = 0;
            _dataTables[fileKey] = dataTable;

            var headers = dataTable.Descriptor.Columns.Select(c => EscapeCsv(c.DisplayName));
            var comments = $"# @name {dataTable.Name}\n# @instanceName {dataTable.InstanceName}\n# @group {dataTable.Group ?? ""}\n";
            File.WriteAllText(csvPath, comments + string.Join(",", headers) + "\n");
        }

        var properties = GetCachedProperties(typeof(TRow), dataTable.Descriptor);
        var values = properties.Select(p => EscapeCsv(p.GetValue(row)));
        File.AppendAllText(csvPath, string.Join(",", values) + "\n");
        _rowCounts[fileKey]++;
    }

    public IEnumerable<object> GetRows(string dataTableName, string? group)
    {
        // CSV store writes to disk; reading back is not supported in this impl
        return [];
    }

    public IReadOnlyList<object> GetDataTables()
    {
        return _dataTables.Values.ToList();
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
