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
namespace OpenRewrite.Core;

/// <summary>
/// Stores data table rows in memory.
/// </summary>
public class InMemoryDataTableStore : IDataTableStore
{
    private bool _acceptRows;
    private readonly Dictionary<string, DataTableDescriptor> _dataTables = new();
    private readonly Dictionary<string, List<object>> _rows = new();

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        if (!_acceptRows) return;

        var name = dataTable.Name;
        _dataTables[name] = dataTable.Descriptor;

        if (!_rows.TryGetValue(name, out var list))
        {
            list = [];
            _rows[name] = list;
        }

        list.Add(row);
    }

    public void AcceptRows(bool accept) => _acceptRows = accept;

    /// <summary>
    /// All stored rows keyed by data table name.
    /// </summary>
    public IReadOnlyDictionary<string, IReadOnlyList<object>> Rows =>
        _rows.ToDictionary(kv => kv.Key, kv => (IReadOnlyList<object>)kv.Value);

    /// <summary>
    /// Descriptors for all data tables that have rows.
    /// </summary>
    public IReadOnlyDictionary<string, DataTableDescriptor> DataTables => _dataTables;
}
