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

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes data table rows directly to CSV files (RFC 4180 format).
 * Each data table is written to a separate CSV file named after the
 * data table's fully qualified class name.
 * <p>
 * Note: The CSV reader ({@link #readIntoContext}) does not support multiline
 * quoted fields. Values containing newlines are correctly escaped on write
 * but will not round-trip through read. This is acceptable because data table
 * column values rarely contain newlines in practice.
 */
public class CsvDataTableStore {

    private final Path outputDir;
    private boolean acceptRows;
    private final Set<String> initializedTables = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, PrintWriter> writers = new ConcurrentHashMap<>();

    public CsvDataTableStore(Path outputDir) {
        this.outputDir = outputDir;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void acceptRows(boolean accept) {
        this.acceptRows = accept;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public <Row> void insertRow(DataTable<Row> dataTable, Row row) {
        if (!acceptRows) {
            return;
        }

        String tableName = dataTable.getName();
        PrintWriter writer = writers.computeIfAbsent(tableName, name -> {
            Path csvPath = outputDir.resolve(name + ".csv");
            try {
                PrintWriter pw = new PrintWriter(new FileOutputStream(csvPath.toFile(), true));
                if (initializedTables.add(name)) {
                    List<String> headers = new ArrayList<>();
                    for (Field field : dataTable.getType().getDeclaredFields()) {
                        Column column = field.getAnnotation(Column.class);
                        if (column != null) {
                            headers.add(escapeCsv(column.displayName()));
                        }
                    }
                    pw.println(String.join(",", headers));
                }
                return pw;
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        });

        List<String> values = new ArrayList<>();
        for (Field field : row.getClass().getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                field.setAccessible(true);
                try {
                    values.add(escapeCsv(field.get(row)));
                } catch (IllegalAccessException e) {
                    values.add("\"\"");
                }
            }
        }
        synchronized (writer) {
            writer.println(String.join(",", values));
            writer.flush();
        }
    }

    /**
     * Closes all open writers. Should be called when the store is no longer needed.
     */
    public void close() {
        for (PrintWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }

    /**
     * Reads all CSV files from the output directory and inserts the rows
     * into the given ExecutionContext's DATA_TABLES map. Each CSV file
     * is named {DataTableClassName}.csv and the rows are reconstructed
     * via reflection. Files are deleted after reading to prevent duplicate
     * imports when multiple RpcRecipes share the same output directory.
     */
    public static void readIntoContext(Path outputDir, ExecutionContext ctx) {
        File[] csvFiles = outputDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles == null) {
            return;
        }

        for (File csvFile : csvFiles) {
            String dataTableClassName = csvFile.getName().replace(".csv", "");
            try {
                readCsvIntoContext(csvFile, dataTableClassName, ctx);
            } catch (Exception e) {
                // Skip data tables we can't reconstruct (e.g., class not on classpath)
            } finally {
                //noinspection ResultOfMethodCallIgnored
                csvFile.delete();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void readCsvIntoContext(File csvFile, String dataTableClassName, ExecutionContext ctx) throws Exception {
        Class<?> dtClass = Class.forName(dataTableClassName);
        // DataTable subclasses have a constructor(Recipe)
        Constructor<?> dtCtor = dtClass.getConstructor(Recipe.class);
        DataTable<?> dataTable = (DataTable<?>) dtCtor.newInstance((Recipe) null);
        Class<?> rowClass = dataTable.getType();
        String dtName = dataTable.getName();

        // Get fields with @Column annotation in declaration order
        List<Field> columnFields = new ArrayList<>();
        for (Field field : rowClass.getDeclaredFields()) {
            if (field.getAnnotation(Column.class) != null) {
                columnFields.add(field);
            }
        }

        // Find the all-args constructor (Lombok @Value generates one)
        Constructor<?> rowCtor = findAllArgsConstructor(rowClass, columnFields);
        if (rowCtor == null) {
            return;
        }

        List<Object> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // skip header
            if (headerLine == null) {
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = parseCsvLine(line);
                if (values.size() != columnFields.size()) {
                    continue;
                }

                Object[] args = new Object[columnFields.size()];
                for (int i = 0; i < columnFields.size(); i++) {
                    args[i] = convertValue(values.get(i), columnFields.get(i).getType());
                }
                rows.add(rowCtor.newInstance(args));
            }
        }

        if (!rows.isEmpty()) {
            // Look up existing DataTable key by name to avoid identity-key duplication
            ctx.computeMessage(ExecutionContext.DATA_TABLES, null, ConcurrentHashMap::new, (ignored, allDataTables) -> {
                DataTable<?> existingKey = null;
                for (Object key : allDataTables.keySet()) {
                    if (key instanceof DataTable && ((DataTable<?>) key).getName().equals(dtName)) {
                        existingKey = (DataTable<?>) key;
                        break;
                    }
                }
                if (existingKey != null) {
                    ((List) allDataTables.get(existingKey)).addAll(rows);
                } else {
                    allDataTables.put(dataTable, new ArrayList<>(rows));
                }
                return allDataTables;
            });
        }
    }

    private static @Nullable Constructor<?> findAllArgsConstructor(Class<?> rowClass, List<Field> columnFields) {
        Class<?>[] paramTypes = columnFields.stream()
                .map(Field::getType)
                .toArray(Class<?>[]::new);
        try {
            return rowClass.getConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            // Try getDeclaredConstructor for private constructors
            try {
                Constructor<?> ctor = rowClass.getDeclaredConstructor(paramTypes);
                ctor.setAccessible(true);
                return ctor;
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    private static Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        return value;
    }

    /**
     * Parse a single CSV line following RFC 4180 rules.
     * Note: does not handle multiline quoted fields (values with embedded newlines).
     */
    static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        values.add(current.toString());
        return values;
    }

    static String escapeCsv(@Nullable Object value) {
        if (value == null) {
            return "\"\"";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
