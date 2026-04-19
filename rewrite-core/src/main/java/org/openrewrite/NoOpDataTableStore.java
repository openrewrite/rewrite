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
import java.util.Collections;
import java.util.stream.Stream;

/**
 * A {@link DataTableStore} that silently drops all inserts and returns empty results.
 */
final class NoOpDataTableStore implements DataTableStore {
    static final NoOpDataTableStore INSTANCE = new NoOpDataTableStore();

    private NoOpDataTableStore() {
    }

    @Override
    public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
    }

    @Override
    public Stream<?> getRows(String dataTableName, @Nullable String group) {
        return Stream.empty();
    }

    @Override
    public Collection<DataTable<?>> getDataTables() {
        return Collections.emptyList();
    }
}
