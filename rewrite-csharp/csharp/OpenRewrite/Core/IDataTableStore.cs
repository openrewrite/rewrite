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
/// Interface for storing data table rows emitted during recipe execution.
/// </summary>
public interface IDataTableStore
{
    /// <summary>
    /// Insert a row into the specified data table.
    /// </summary>
    void InsertRow<TRow>(DataTable<TRow> dataTable, ExecutionContext ctx, TRow row) where TRow : notnull;

    /// <summary>
    /// Enable or disable row acceptance.
    /// </summary>
    void AcceptRows(bool accept);
}
