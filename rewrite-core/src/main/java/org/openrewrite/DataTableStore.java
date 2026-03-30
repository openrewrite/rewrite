/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Abstraction for storing data table rows emitted during recipe execution.
 * <p>
 * Different environments provide different implementations:
 * <ul>
 *     <li>{@link InMemoryDataTableStore} — default for tests and general use</li>
 *     <li>{@link CsvDataTableStore} — writes directly to CSV files</li>
 * </ul>
 * <p>
 * Each data table bucket is identified by the data table's class name and
 * its {@link DataTable#getGroup() group} (for community tables) or
 * {@link DataTable#getInstanceName()} (for private tables).
 */
public interface DataTableStore {

    /**
     * A store that silently drops all inserts and returns empty results.
     */
    static DataTableStore noop() {
        return NoOpDataTableStore.INSTANCE;
    }

    /**
     * Insert a row into the specified data table.
     * The store uses the data table's group and instance name to determine
     * the storage bucket.
     *
     * @param dataTable the data table definition
     * @param ctx       the execution context
     * @param row       the row to insert
     * @param <Row>     the row type
     */
    <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row);

    /**
     * Stream rows for a specific table by class name and group.
     *
     * @param dataTableName the fully qualified class name of the data table
     * @param group         the group identifying the bucket, or null for ungrouped
     * @return a stream of rows, or an empty stream if no rows exist
     */
    Stream<?> getRows(String dataTableName, @Nullable String group);

    /**
     * Get the set of {@link DataTable} instances that have received rows.
     *
     * @return the data tables that have been written to
     */
    Collection<DataTable<?>> getDataTables();
}
