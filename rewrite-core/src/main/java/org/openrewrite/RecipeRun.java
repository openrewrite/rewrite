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
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.internal.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.RecipeIntrospectionUtils.dataTableDescriptorFromDataTable;

@Value
public class RecipeRun {

    @With
    Changeset changeset;

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

    public void exportDatatablesToCsv(String filePath, ExecutionContext ctx) {
        for (Map.Entry<DataTable<?>, List<?>> entry : dataTables.entrySet()) {
            DataTable<?> dataTable = entry.getKey();
            List<?> rows = entry.getValue();
            File csv = new File(filePath + dataTable.getName() + ".csv");
            try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(csv, false))) {
                DataTableDescriptor descriptor = dataTableDescriptorFromDataTable(dataTable);
                List<String> fieldNames = new ArrayList<>();
                List<String> fieldTitles = new ArrayList<>();
                List<String> fieldDescriptions = new ArrayList<>();

                for (ColumnDescriptor columnDescriptor : descriptor.getColumns()) {
                    fieldNames.add(columnDescriptor.getName());
                    fieldTitles.add(String.format("\"%s\"", columnDescriptor.getDisplayName()));
                    fieldDescriptions.add(String.format("\"%s\"", columnDescriptor.getDescription()));
                }

                printWriter.println(String.join(",", fieldTitles));
                printWriter.println(String.join(",", fieldDescriptions));
                printRowData(printWriter, rows, fieldNames, ctx);
            } catch (FileNotFoundException e) {
                ctx.getOnError().accept(e);
            }
        }
    }

    private void printRowData(PrintWriter printWriter, List<?> rows, List<String> fieldNames, ExecutionContext ctx) {
        for (Object row : rows) {
            List<String> rowValues = new ArrayList<>();
            for (String fieldName : fieldNames) {
                try {
                    Field field = row.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    //Assume every column value is printable with toString
                    rowValues.add(String.format("\"%s\"", field.get(row).toString()));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    ctx.getOnError().accept(e);
                }
            }
            printWriter.println(String.join(",", rowValues));
        }
    }
}
