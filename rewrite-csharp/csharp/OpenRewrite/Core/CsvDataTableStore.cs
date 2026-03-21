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
/// Each data table is written to a separate CSV file named after the data table.
/// </summary>
public class CsvDataTableStore : IDataTableStore
{
    private readonly string _outputDir;
    private bool _acceptRows;
    private readonly HashSet<string> _initializedTables = [];
    private readonly Dictionary<string, int> _rowCounts = new();
    private readonly Dictionary<Type, PropertyInfo[]> _propertyCache = new();

    public CsvDataTableStore(string outputDir)
    {
        _outputDir = outputDir;
        Directory.CreateDirectory(outputDir);
    }

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        if (!_acceptRows) return;

        var descriptor = dataTable.Descriptor;
        var tableName = descriptor.Name;
        var csvPath = Path.Combine(_outputDir, tableName + ".csv");

        if (!_initializedTables.Contains(tableName))
        {
            _initializedTables.Add(tableName);
            _rowCounts[tableName] = 0;

            var headers = descriptor.Columns.Select(c => EscapeCsv(c.DisplayName));
            File.WriteAllText(csvPath, string.Join(",", headers) + "\n");
        }

        var properties = GetCachedProperties(typeof(TRow), descriptor);
        var values = properties.Select(p => EscapeCsv(p.GetValue(row)));
        File.AppendAllText(csvPath, string.Join(",", values) + "\n");
        _rowCounts[tableName]++;
    }

    public void AcceptRows(bool accept) => _acceptRows = accept;

    /// <summary>
    /// Number of rows written for each data table.
    /// </summary>
    public IReadOnlyDictionary<string, int> RowCounts => _rowCounts;

    /// <summary>
    /// Names of all data tables that have been written to.
    /// </summary>
    public IReadOnlyList<string> TableNames => _initializedTables.ToList();

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
}
