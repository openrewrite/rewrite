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
import org.jspecify.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
     * Construct a new data table.
     *
     * @param recipe      The recipe that this data table is associated with.
     * @param displayName The display name of this data table.
     * @param description The description of this data table.
     */
    public DataTable(@Nullable Recipe recipe,
                     @NlsRewrite.DisplayName @Language("markdown") String displayName,
                     @NlsRewrite.Description @Language("markdown") String description) {
        this.displayName = displayName;
        this.description = description;

        // Only null when transferring DataTables over RPC.
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
        if (!allowWritingInThisCycle(ctx)) {
            return;
        }
        ctx.computeMessage(ExecutionContext.DATA_TABLES, row, ConcurrentHashMap::new, (extract, allDataTables) -> {
            //noinspection unchecked
            List<Row> dataTablesOfType = (List<Row>) allDataTables.computeIfAbsent(this, c -> new ArrayList<>());
            dataTablesOfType.add(row);
            return allDataTables;
        });
    }

    /**
     * This method is used to decide weather to ignore any row insertions in the current cycle.
     * This prevents (by default) data table producing recipes from having to keep track of state across
     * multiple cycles to prevent duplicate row entries.
     *
     * @param ctx the execution context
     * @return weather to allow writing in this cycle
     */
    protected boolean allowWritingInThisCycle(ExecutionContext ctx) {
        return ctx.getCycle() <= 1;
    }
}
