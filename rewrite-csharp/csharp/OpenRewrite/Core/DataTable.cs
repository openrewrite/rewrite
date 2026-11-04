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
/// A data table for collecting structured data during recipe execution.
/// </summary>
public class DataTable<TRow> where TRow : notnull
{
    public const string DataTableStoreKey = "org.openrewrite.dataTables.store";

    private readonly DataTableDescriptor _descriptor;
    private string? _group;
    private string? _instanceName;


    public DataTable(string name, string displayName, string description)
    {
        _descriptor = new DataTableDescriptor(name, displayName, description, BuildColumns());
    }

    public string Name => _descriptor.Name;

    public DataTableDescriptor Descriptor => _descriptor;

    public string? Group
    {
        get => _group;
        set => _group = value;
    }

    /// <summary>
    /// The instance name. Defaults to the display name from the descriptor.
    /// </summary>
    public string InstanceName
    {
        get => _instanceName ?? _descriptor.DisplayName;
        set => _instanceName = value;
    }

    /// <summary>
    /// Insert a row into this data table via the store in the execution context.
    /// </summary>
    public void InsertRow(ExecutionContext ctx, TRow row)
    {
        var store = ctx.ComputeMessageIfAbsent<IDataTableStore>(DataTableStoreKey, _ => new InMemoryDataTableStore());
        store.InsertRow(this, ctx, row);
    }

    private static IReadOnlyList<ColumnDescriptor> BuildColumns()
    {
        var columns = new List<ColumnDescriptor>();
        foreach (var prop in typeof(TRow).GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            var attr = prop.GetCustomAttribute<ColumnAttribute>();
            if (attr is not null)
            {
                columns.Add(new ColumnDescriptor(prop.Name, attr.DisplayName, attr.Description));
            }
        }

        return columns;
    }
}
