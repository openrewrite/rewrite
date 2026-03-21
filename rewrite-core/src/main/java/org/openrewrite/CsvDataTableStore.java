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

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.openrewrite.internal.RecipeIntrospectionUtils.dataTableDescriptorFromDataTable;

/**
 * A {@link DataTableStore} that writes data table rows directly to CSV files
 * as they are inserted. Uses Univocity for proper CSV escaping (RFC 4180).
 * <p>
 * Supports configurable output stream creation (e.g., for GZIP compression)
 * and static prefix/suffix columns (e.g., repository metadata).
 *
 * <pre>{@code
 * // Plain CSV
 * new CsvDataTableStore(outputDir)
 *
 * // GZIP compressed with repository columns
 * new CsvDataTableStore(outputDir,
 *     path -> new GZIPOutputStream(Files.newOutputStream(path)),
 *     ".csv.gz",
 *     List.of(Map.entry("repositoryOrigin", origin), Map.entry("repositoryPath", path)),
 *     List.of(Map.entry("org1", orgValue)))
 * }</pre>
 */
public class CsvDataTableStore implements DataTableStore, AutoCloseable {

    private final Path outputDir;
    private final Function<Path, OutputStream> outputStreamFactory;
    private final String fileExtension;
    private final Map<String, String> prefixColumns;
    private final Map<String, String> suffixColumns;
    private final ConcurrentHashMap<String, BucketWriter> writers = new ConcurrentHashMap<>();

    /**
     * Create a store that writes plain CSV files.
     */
    public CsvDataTableStore(Path outputDir) {
        this(outputDir, CsvDataTableStore::defaultOutputStream, ".csv",
                Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Create a store with full control over output stream creation, file extension,
     * and additional static columns prepended/appended to each row.
     *
     * @param outputDir           directory to write files into
     * @param outputStreamFactory creates an output stream for each file path (e.g., wrapping with GZIPOutputStream)
     * @param fileExtension       file extension including dot (e.g., ".csv" or ".csv.gz")
     * @param prefixColumns       static columns prepended to each row, in insertion order
     * @param suffixColumns       static columns appended to each row, in insertion order
     */
    public CsvDataTableStore(Path outputDir,
                             Function<Path, OutputStream> outputStreamFactory,
                             String fileExtension,
                             Map<String, String> prefixColumns,
                             Map<String, String> suffixColumns) {
        this.outputDir = outputDir;
        this.outputStreamFactory = outputStreamFactory;
        this.fileExtension = fileExtension;
        this.prefixColumns = prefixColumns;
        this.suffixColumns = suffixColumns;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static OutputStream defaultOutputStream(Path path) {
        try {
            return new FileOutputStream(path.toFile());
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
        String fileKey = fileKey(dataTable);
        BucketWriter writer = writers.computeIfAbsent(fileKey, k -> createBucketWriter(dataTable));
        writer.writeRow(row);
    }

    @Override
    public Stream<?> getRows(String dataTableName, @Nullable String group) {
        return Stream.empty();
    }

    @Override
    public Collection<DataTable<?>> getDataTables() {
        List<DataTable<?>> result = new ArrayList<>(writers.size());
        for (BucketWriter writer : writers.values()) {
            result.add(writer.dataTable);
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void close() {
        for (BucketWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }

    private BucketWriter createBucketWriter(DataTable<?> dataTable) {
        String fileKey = fileKey(dataTable);
        Path path = outputDir.resolve(fileKey + fileExtension);

        DataTableDescriptor descriptor = dataTableDescriptorFromDataTable(dataTable);
        List<String> fieldNames = new ArrayList<>();
        for (ColumnDescriptor col : descriptor.getColumns()) {
            fieldNames.add(col.getName());
        }

        // Build headers: prefix + data table columns + suffix
        List<String> headers = new ArrayList<>();
        headers.addAll(prefixColumns.keySet());
        for (ColumnDescriptor col : descriptor.getColumns()) {
            headers.add(col.getDisplayName());
        }
        headers.addAll(suffixColumns.keySet());

        OutputStream os = outputStreamFactory.apply(path);
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.setHeaderWritingEnabled(true);
        settings.getFormat().setComment('#');
        CsvWriter csvWriter = new CsvWriter(os, settings);

        // Write metadata as comments
        csvWriter.commentRow(" @name " + dataTable.getName());
        csvWriter.commentRow(" @instanceName " + dataTable.getInstanceName());
        csvWriter.commentRow(" @group " + (dataTable.getGroup() != null ? dataTable.getGroup() : ""));
        csvWriter.writeHeaders(headers);

        return new BucketWriter(dataTable, csvWriter, os, fieldNames, headers.size());
    }

    private class BucketWriter {
        final DataTable<?> dataTable;
        private final CsvWriter csvWriter;
        private final OutputStream os;
        private final List<String> fieldNames;
        private final int totalColumns;

        BucketWriter(DataTable<?> dataTable, CsvWriter csvWriter, OutputStream os,
                     List<String> fieldNames, int totalColumns) {
            this.dataTable = dataTable;
            this.csvWriter = csvWriter;
            this.os = os;
            this.fieldNames = fieldNames;
            this.totalColumns = totalColumns;
        }

        synchronized void writeRow(Object row) {
            String[] values = new String[totalColumns];
            int offset = prefixColumns.size();

            // Prefix column values
            int pi = 0;
            for (String val : prefixColumns.values()) {
                values[pi++] = val;
            }

            // Data table column values
            for (int i = 0; i < fieldNames.size(); i++) {
                try {
                    Field field = row.getClass().getDeclaredField(fieldNames.get(i));
                    field.setAccessible(true);
                    Object val = field.get(row);
                    values[offset + i] = val != null ? val.toString() : "";
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    values[offset + i] = "";
                }
            }

            // Suffix column values
            int si = offset + fieldNames.size();
            for (String val : suffixColumns.values()) {
                values[si++] = val;
            }

            csvWriter.writeRow((Object[]) values);
        }

        void close() {
            csvWriter.close();
            try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }

    // =========================================================================
    // Static utilities
    // =========================================================================

    /**
     * Build a filesystem-safe key for a data table. Uses the group name for
     * community tables, or the instance name for private tables.
     */
    public static String fileKey(DataTable<?> dataTable) {
        String group = dataTable.getGroup();
        String suffix = group != null ? group : dataTable.getInstanceName();
        if (suffix.equals(dataTable.getName())) {
            return dataTable.getName();
        }
        return dataTable.getName() + "--" + sanitize(suffix);
    }

    /**
     * Sanitize a string for use in filenames.
     */
    public static String sanitize(String value) {
        String s = value.toLowerCase();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isLetterOrDigit(c) ? c : '-');
        }
        StringBuilder collapsed = new StringBuilder();
        boolean lastWasDash = true;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '-') {
                if (!lastWasDash) {
                    collapsed.append('-');
                }
                lastWasDash = true;
            } else {
                collapsed.append(c);
                lastWasDash = false;
            }
        }
        String prefix = collapsed.toString();
        while (prefix.endsWith("-")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.length() > 30) {
            prefix = prefix.substring(0, 30);
            int lastDash = prefix.lastIndexOf('-');
            if (lastDash > 0) {
                prefix = prefix.substring(0, lastDash);
            }
        }
        String hash = sha256Prefix(value, 4);
        return prefix + "-" + hash;
    }

    /**
     * Read metadata comments from the top of a CSV input stream and return
     * a {@link DataTableDescriptor}. Callers handle decompression if needed.
     *
     * @return a descriptor populated from comments, or null if required comments are missing
     */
    public static @Nullable DataTableDescriptor readDescriptor(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String name = null;
        String instanceName = null;
        String group = null;

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#")) {
                break;
            }
            String comment = line.substring(1).trim();
            if (comment.startsWith("@name ")) {
                name = comment.substring("@name ".length()).trim();
            } else if (comment.startsWith("@instanceName ")) {
                instanceName = comment.substring("@instanceName ".length()).trim();
            } else if (comment.startsWith("@group ")) {
                group = comment.substring("@group ".length()).trim();
                if (group.isEmpty()) {
                    group = null;
                }
            }
        }

        if (name == null || instanceName == null) {
            return null;
        }
        // displayName is set to name (FQN) since the CSV comments don't include it.
        // The full descriptor with proper displayName comes from recipes.csv in downstream resolvers.
        return new DataTableDescriptor(name, name, instanceName, "", group, Collections.emptyList());
    }

    private static String sha256Prefix(String input, int hexChars) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
                if (hex.length() >= hexChars) {
                    break;
                }
            }
            return hex.substring(0, Math.min(hexChars, hex.length()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
