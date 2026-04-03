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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Default {@link DataTableStore} that holds all rows in memory.
 * <p>
 * Suitable for tests, Maven/Gradle plugin execution, and any context
 * where rows need to be read back after recipe execution.
 * <p>
 * Thread-safe: uses {@link ConcurrentHashMap} for bucket storage and
 * synchronized lists for per-bucket row accumulation.
 */
public class InMemoryDataTableStore implements DataTableStore {

    /**
     * A bucket holding the representative DataTable instance and its accumulated rows.
     */
    private static class Bucket {
        final DataTable<?> dataTable;
        final List<Object> rows = Collections.synchronizedList(new ArrayList<>());

        Bucket(DataTable<?> dataTable) {
            this.dataTable = dataTable;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static String bucketKey(DataTable<?> dataTable) {
        String group = dataTable.getGroup();
        return dataTable.getName() + "\0" + (group != null ? group : dataTable.getInstanceName());
    }

    @Override
    public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
        String key = bucketKey(dataTable);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(dataTable));
        bucket.rows.add(row);
    }

    @Override
    public Stream<?> getRows(String dataTableName, @Nullable String group) {
        List<Object> allRows = new ArrayList<>();
        for (Bucket bucket : buckets.values()) {
            if (bucket.dataTable.getName().equals(dataTableName) &&
                java.util.Objects.equals(bucket.dataTable.getGroup(), group)) {
                synchronized (bucket.rows) {
                    allRows.addAll(bucket.rows);
                }
            }
        }
        return allRows.stream();
    }

    @Override
    public Collection<DataTable<?>> getDataTables() {
        List<DataTable<?>> result = new ArrayList<>(buckets.size());
        for (Bucket bucket : buckets.values()) {
            result.add(bucket.dataTable);
        }
        return Collections.unmodifiableCollection(result);
    }
}
