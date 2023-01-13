/*
 * Copyright 2022 the original author or authors.
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

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Value
public class RecipeRun {
    RecipeRunStats stats;

    @With
    List<Result> results;

    @With
    Map<DataTable<?>, List<?>> dataTables;

    @Nullable
    public DataTable<?> getDataTable(String name) {
        for (DataTable<?> dataTable : dataTables.keySet()) {
            if (dataTable.getName().equals(name)) {
                return dataTable;
            }
        }
        return null;
    }

    @Nullable
    public <E> List<E> getDataTableRows(String name) {
        for (Map.Entry<DataTable<?>, List<?>> dataTableAndRows : dataTables.entrySet()) {
            if (dataTableAndRows.getKey().getName().equals(name)) {
                //noinspection unchecked
                return (List<E>) dataTableAndRows.getValue();
            }
        }
        return emptyList();
    }
}
