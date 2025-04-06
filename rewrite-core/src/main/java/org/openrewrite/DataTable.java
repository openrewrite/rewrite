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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.InMemoryDataTableStore;

import java.lang.reflect.ParameterizedType;

/**
 * @param <Row> The model type for a single row of this data table.
 */
@Getter
@JsonIgnoreType
@RequiredArgsConstructor
public class DataTable<Row> {
    @Language("markdown")
    private final @NlsRewrite.DisplayName String displayName;

    @Language("markdown")
    private final @NlsRewrite.Description String description;

    /**
     * @param recipe      The recipe that this data table is associated with.
     * @param type        The model type for a single row of this data table.
     * @param name        The name of this data table.
     * @param displayName The display name of this data table.
     * @param description The description of this data table.
     * @deprecated Use {@link #DataTable(Recipe, String, String)} instead.
     */
    @SuppressWarnings("unused")
    @Deprecated
    public DataTable(Recipe recipe, Class<Row> type, String name,
                     @NlsRewrite.DisplayName @Language("markdown") String displayName,
                     @NlsRewrite.Description @Language("markdown") String description) {
        this(recipe, displayName, description);
    }

    /**
     * Construct a new data table.
     *
     * @param recipe      The recipe that this data table is associated with.
     * @param displayName The display name of this data table.
     * @param description The description of this data table.
     */
    public DataTable(Recipe recipe,
                     @NlsRewrite.DisplayName @Language("markdown") String displayName,
                     @NlsRewrite.Description @Language("markdown") String description) {
        this.displayName = displayName;
        this.description = description;

        // Only null when transferring DataTables over RPC.
        //noinspection ConstantValue
        if (recipe != null) {
            recipe.addDataTable(this);
        }
    }

    public Class<Row> getType() {
        //noinspection unchecked
        return (Class<Row>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    public String getName() {
        return getClass().getName();
    }

    public void insertRow(ExecutionContext ctx, Row row) {
        if (ctx.getCycle() > 1) {
            return;
        }
        ctx.computeMessage(ExecutionContext.DATA_TABLE_STORE, row, InMemoryDataTableStore::new, (extract, dataTableStore) -> dataTableStore)
                .insertRow(this, ctx, row);
    }
}
