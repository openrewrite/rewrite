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
/// Recipes create instances of this class and call <see cref="InsertRow"/>
/// to emit rows that are stored by the configured <see cref="IDataTableStore"/>.
/// </summary>
/// <remarks>
/// Column order is determined by the declaration order of properties on <typeparamref name="TRow"/>
/// annotated with <see cref="ColumnAttribute"/>. This relies on the current .NET runtime behavior
/// of <see cref="Type.GetProperties(BindingFlags)"/> returning properties in declaration order,
/// which matches Python's <c>__dataclass_fields__</c> and JS's decorator registration order.
/// </remarks>
public class DataTable<TRow> where TRow : notnull
{
    public const string DataTableStoreKey = "org.openrewrite.dataTables.store";

    private readonly DataTableDescriptor _descriptor;

    public DataTable(string name, string displayName, string description)
    {
        _descriptor = new DataTableDescriptor(name, displayName, description, BuildColumns());
    }

    public string Name => _descriptor.Name;

    public DataTableDescriptor Descriptor => _descriptor;

    /// <summary>
    /// Insert a row into this data table via the store in the execution context.
    /// If no store exists, an <see cref="InMemoryDataTableStore"/> is created as default.
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
