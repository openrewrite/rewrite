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
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;
import org.intellij.lang.annotations.Language;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @param <Row> The model type for a single row of this extract.
 */
@Getter
@Incubating(since = "7.35.0")
@JsonIgnoreType
public class DataTable<Row> {
    private final String name;
    private final Class<Row> type;

    @Language("markdown")
    private final String displayName;

    @Language("markdown")
    private final String description;

    @Setter
    private boolean enabled = true;

    /**
     * Ignore any row insertions after this cycle. This prevents
     * data table producing recipes from having to keep track of state across
     * multiple cycles to prevent duplicate row entries.
     */
    protected int maxCycle = 1;

    public DataTable(Recipe recipe, Class<Row> type, String name,
                     @Language("markdown") String displayName,
                     @Language("markdown") String description) {
        this.type = type;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        recipe.addDataTable(this);
    }

    public DataTable(Recipe recipe,
                     @Language("markdown") String displayName,
                     @Language("markdown") String description) {
        //noinspection unchecked
        this.type = (Class<Row>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
        this.name = getClass().getName();
        this.displayName = displayName;
        this.description = description;
        recipe.addDataTable(this);
    }

    @SuppressWarnings("unused")
    public TypeReference<List<Row>> getRowsTypeReference() {
        return new TypeReference<List<Row>>() {
        };
    }

    public void insertRow(ExecutionContext ctx, Row row) {
        if (enabled && ctx.getCycle() <= maxCycle) {
            ctx.computeMessage(ExecutionContext.DATA_TABLES, row, ConcurrentHashMap::new, (extract, allDataTables) -> {
                //noinspection unchecked
                List<Row> dataTablesOfType = (List<Row>) allDataTables.computeIfAbsent(this, c -> new ArrayList<>());
                dataTablesOfType.add(row);
                return allDataTables;
            });
        }
    }
}
