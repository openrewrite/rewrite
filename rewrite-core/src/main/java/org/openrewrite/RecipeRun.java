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
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.openrewrite.internal.RecipeIntrospectionUtils.dataTableDescriptorFromDataTable;

@Value
public class RecipeRun {

    @With
    Changeset changeset;

    @With
    Map<DataTable<?>, List<?>> dataTables;

    public @Nullable DataTable<?> getDataTable(String name) {
        for (DataTable<?> dataTable : dataTables.keySet()) {
            if (dataTable.getName().equals(name)) {
                return dataTable;
            }
        }
        return null;
    }

    public <E> List<E> getDataTableRows(String name) {
        List<E> results = new ArrayList<>();
        for (Map.Entry<DataTable<?>, List<?>> dataTableAndRows : dataTables.entrySet()) {
            if (dataTableAndRows.getKey().getName().equals(name)) {
                //noinspection unchecked
                results.addAll ((List<E>) dataTableAndRows.getValue());
            }
        }
        return results;
    }

    @SuppressWarnings("unused")
    public void exportDatatablesToCsv(Path filePath, ExecutionContext ctx) {
        try {
            Files.createDirectories(filePath);
        } catch (IOException e) {
            ctx.getOnError().accept(e);
        }
        for (Map.Entry<DataTable<?>, List<?>> entry : dataTables.entrySet()) {
            DataTable<?> dataTable = entry.getKey();
            List<?> rows = entry.getValue();
            File csv = filePath.resolve(dataTable.getName() + ".csv").toFile();
            try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(csv, false))) {
                exportCsv(ctx, dataTable, printWriter::println, rows);
            } catch (FileNotFoundException e) {
                ctx.getOnError().accept(e);
            }
        }
    }

    public static void exportCsv(final ExecutionContext ctx, final DataTable<?> dataTable, final Consumer<String> output,
            final List<?> rows) {
        DataTableDescriptor descriptor = dataTableDescriptorFromDataTable(dataTable);
        List<String> fieldNames = new ArrayList<>();
        List<String> fieldTitles = new ArrayList<>();
        List<String> fieldDescriptions = new ArrayList<>();

        for (ColumnDescriptor columnDescriptor : descriptor.getColumns()) {
            fieldNames.add(columnDescriptor.getName());
            fieldTitles.add(formatForCsv(columnDescriptor.getDisplayName()));
            fieldDescriptions.add(formatForCsv(columnDescriptor.getDescription()));
        }

        output.accept(String.join(",", fieldTitles));
        output.accept(String.join(",", fieldDescriptions));
        exportRowData(output, rows, fieldNames, ctx);
    }

    private static void exportRowData(Consumer<String> output, List<?> rows, List<String> fieldNames,
            ExecutionContext ctx) {
        for (Object row : rows) {
            List<String> rowValues = new ArrayList<>();
            for (String fieldName : fieldNames) {
                try {
                    Field field = row.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    rowValues.add(formatForCsv(field.get(row)));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    ctx.getOnError().accept(e);
                }
            }
            output.accept(String.join(",", rowValues));
        }
    }

    private static String formatForCsv(@Nullable Object data) {
        if (data != null) {
            // Assume every column value is printable with toString
            return String.format("\"%s\"", data.toString().replace("\"", "\"\""));
        } else {
            return "\"\"";
        }
    }
}
