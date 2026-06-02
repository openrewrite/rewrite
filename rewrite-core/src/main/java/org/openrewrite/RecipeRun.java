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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
public class RecipeRun {

    @With
    Changeset changeset;

    DataTableStore dataTableStore;

    public @Nullable DataTable<?> getDataTable(String name) {
        return getDataTable(name, null);
    }

    public @Nullable DataTable<?> getDataTable(String name, @Nullable String group) {
        for (DataTable<?> dataTable : dataTableStore.getDataTables()) {
            if (dataTable.getName().equals(name) && Objects.equals(dataTable.getGroup(), group)) {
                return dataTable;
            }
        }
        return null;
    }

    /**
     * @deprecated Use {@link #getDataTableRows(Class)} for type-safe deserialization.
     */
    @Deprecated
    public <E> List<E> getDataTableRows(String name) {
        return getDataTableRows(name, null);
    }

    /**
     * @deprecated Use {@link #getDataTableRows(Class, String)} for type-safe deserialization.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public <E> List<E> getDataTableRows(String name, @Nullable String group) {
        return (List<E>) dataTableStore.getRows(name, group)
                .collect(Collectors.toList());
    }

    public <E> List<E> getDataTableRows(Class<? extends DataTable<E>> dataTableClass) {
        return getDataTableRows(dataTableClass, null);
    }

    public <E> List<E> getDataTableRows(Class<? extends DataTable<E>> dataTableClass, @Nullable String group) {
        return dataTableStore.getRows(dataTableClass, group)
                .collect(Collectors.toList());
    }
}
