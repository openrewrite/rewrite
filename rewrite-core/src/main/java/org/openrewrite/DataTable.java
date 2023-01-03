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
import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @param <Row> The model type for a single row of this extract.
 */
@Value
@Incubating(since = "7.35.0")
public class DataTable<Row> {
    Class<Row> type;

    @Language("markdown")
    String displayName;

    @Language("markdown")
    String description;

    @SuppressWarnings("UnusedReturnValue")
    public DataTable<Row> insertRow(ExecutionContext ctx, Row row) {
        ctx.computeMessage(ExecutionContext.DATA_TABLES, row, HashMap::new, (extract, allDataTables) -> {
            //noinspection unchecked
            List<Row> dataTablesOfType = (List<Row>) allDataTables.computeIfAbsent(DataTable.this, c -> new ArrayList<>());
            dataTablesOfType.add(row);
            return allDataTables;
        });
        return this;
    }
}
