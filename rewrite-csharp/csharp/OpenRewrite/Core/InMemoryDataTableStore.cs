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

namespace OpenRewrite.Core;

/// <summary>
/// Stores data table rows in memory, keyed by (name, scope).
/// </summary>
public class InMemoryDataTableStore : IDataTableStore
{
    private readonly ConcurrentDictionary<string, Bucket> _buckets = new();

    private sealed class Bucket(object dataTable)
    {
        public object DataTable { get; } = dataTable;
        public List<object> Rows { get; } = [];
    }

    private static string BucketKey(string name, string scope) => $"{name}\0{scope}";

    public void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull
    {
        var suffix = dataTable.Group ?? dataTable.InstanceName;
        var key = BucketKey(dataTable.Name, suffix);
        var bucket = _buckets.GetOrAdd(key, _ => new Bucket(dataTable));
        lock (bucket.Rows)
        {
            bucket.Rows.Add(row);
        }
    }

    public IEnumerable<object> GetRows(string dataTableName, string? group)
    {
        if (group != null)
        {
            var key = BucketKey(dataTableName, group);
            if (!_buckets.TryGetValue(key, out var bucket))
                return [];
            List<object> snapshot;
            lock (bucket.Rows) { snapshot = [..bucket.Rows]; }
            return snapshot;
        }
        // For ungrouped, find by name with no group
        foreach (var bucket in _buckets.Values)
        {
            if (bucket.DataTable is DataTable<object> dt && dt.Name == dataTableName && dt.Group == null)
            {
                List<object> snapshot;
                lock (bucket.Rows) { snapshot = [..bucket.Rows]; }
                return snapshot;
            }
        }
        return [];
    }

    public IReadOnlyList<object> GetDataTables()
    {
        return _buckets.Values.Select(b => b.DataTable).ToList();
    }
}
