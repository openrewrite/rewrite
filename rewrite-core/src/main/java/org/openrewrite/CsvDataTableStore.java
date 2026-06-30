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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.errorprone.annotations.MustBeClosed;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openrewrite.internal.RecipeIntrospectionUtils.dataTableDescriptorFromDataTable;

/**
 * A {@link DataTableStore} that writes data table rows directly to CSV files
 * as they are inserted. Uses Univocity for proper CSV escaping (RFC 4180).
 * <p>
 * Supports configurable output stream creation (e.g., for GZIP compression)
 * and static prefix/suffix columns (e.g., repository metadata).
 *
 * <p>
 * When {@link #getRows} is called, open writers for the requested table are
 * <em>closed</em> (not merely flushed) so that compression trailers such as
 * the GZIP footer are written, producing a fully valid file on disk.  If
 * {@link #insertRow} is called again for the same table, a new writer is
 * created in append mode. For this reason the {@code outputStreamFactory}
 * <strong>must</strong> open the stream with {@link java.nio.file.StandardOpenOption#CREATE
 * CREATE} and {@link java.nio.file.StandardOpenOption#APPEND APPEND} semantics
 * so that data written before the close is preserved. For compressed streams
 * this produces a multi-member archive (e.g., concatenated GZIP members),
 * which {@link java.util.zip.GZIPInputStream} handles transparently.
 *
 * <pre>{@code
 * // Plain CSV
 * new CsvDataTableStore(outputDir)
 *
 * // GZIP compressed with repository columns (write-only)
 * new CsvDataTableStore(outputDir,
 *     path -> new GZIPOutputStream(Files.newOutputStream(path,
 *         StandardOpenOption.CREATE, StandardOpenOption.APPEND)),
 *     ".csv.gz",
 *     Map.of("repositoryOrigin", origin, "repositoryPath", path),
 *     Map.of("org1", orgValue))
 *
 * // GZIP compressed with read-back support
 * new CsvDataTableStore(outputDir,
 *     path -> new GZIPOutputStream(Files.newOutputStream(path,
 *         StandardOpenOption.CREATE, StandardOpenOption.APPEND)),
 *     path -> new GZIPInputStream(Files.newInputStream(path)),
 *     ".csv.gz",
 *     Map.of("repositoryOrigin", origin, "repositoryPath", path),
 *     Map.of("org1", orgValue))
 * }</pre>
 */
public class CsvDataTableStore implements DataTableStore, AutoCloseable {

    private final Path outputDir;
    private final Function<Path, OutputStream> outputStreamFactory;
    private final Function<Path, InputStream> inputStreamFactory;
    private final String fileExtension;
    private final Map<String, String> prefixColumns;
    private final Map<String, String> suffixColumns;
    private static final ObjectMapper ROW_MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ConcurrentHashMap<String, BucketWriter> writers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RowMetadata> rowMetadata = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DataTable<?>> knownTables = new ConcurrentHashMap<>();

    /**
     * Create a store that writes plain CSV files.
     */
    public CsvDataTableStore(Path outputDir) {
        this(outputDir, CsvDataTableStore::defaultOutputStream, CsvDataTableStore::defaultInputStream,
                ".csv", Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Raw CSV has no cross-record state, so several writers can safely append to one shared file.
     */
    public CsvDataTableStore(Path outputDir,
                             Map<String, String> prefixColumns,
                             Map<String, String> suffixColumns) {
        this(outputDir, CsvDataTableStore::defaultOutputStream, CsvDataTableStore::defaultInputStream,
                ".csv", prefixColumns, suffixColumns);
    }

    /**
     * Create a store with control over output stream creation, file extension,
     * and additional static columns prepended/appended to each row.
     * <p>
     * {@link #getRows} will always return empty results from this constructor.
     * Use the six-argument constructor to provide a matching input stream factory
     * if read-back is needed.
     *
     * @param outputDir           directory to write files into
     * @param outputStreamFactory creates an output stream for each file path (e.g., wrapping with GZIPOutputStream)
     * @param fileExtension       file extension including dot (e.g., ".csv" or ".csv.gz")
     * @param prefixColumns       static columns prepended to each row, in insertion order
     * @param suffixColumns       static columns appended to each row, in insertion order
     * @deprecated Use the six-argument constructor that accepts an {@code inputStreamFactory}
     */
    @Deprecated
    public CsvDataTableStore(Path outputDir,
                             Function<Path, OutputStream> outputStreamFactory,
                             String fileExtension,
                             Map<String, String> prefixColumns,
                             Map<String, String> suffixColumns) {
        this(outputDir, outputStreamFactory, path -> new ByteArrayInputStream(new byte[0]),
                fileExtension, prefixColumns, suffixColumns);
    }

    /**
     * Create a store with full control over output and input stream creation, file extension,
     * and additional static columns prepended/appended to each row.
     *
     * @param outputDir           directory to write files into
     * @param outputStreamFactory creates an output stream for each file path. <strong>Must</strong> use
     *                            {@link java.nio.file.StandardOpenOption#CREATE CREATE} and
     *                            {@link java.nio.file.StandardOpenOption#APPEND APPEND} so that
     *                            rows written before a mid-run {@link #getRows} call are preserved
     *                            when the writer is re-opened for subsequent inserts.
     * @param inputStreamFactory  creates an input stream for each file path (e.g., wrapping with GZIPInputStream)
     * @param fileExtension       file extension including dot (e.g., ".csv" or ".csv.gz")
     * @param prefixColumns       static columns prepended to each row, in insertion order
     * @param suffixColumns       static columns appended to each row, in insertion order
     */
    public CsvDataTableStore(Path outputDir,
                             Function<Path, OutputStream> outputStreamFactory,
                             Function<Path, InputStream> inputStreamFactory,
                             String fileExtension,
                             Map<String, String> prefixColumns,
                             Map<String, String> suffixColumns) {
        this.outputDir = outputDir;
        this.outputStreamFactory = outputStreamFactory;
        this.inputStreamFactory = inputStreamFactory;
        this.fileExtension = fileExtension;
        this.prefixColumns = prefixColumns;
        this.suffixColumns = suffixColumns;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public Map<String, String> getPrefixColumns() {
        return prefixColumns;
    }

    public Map<String, String> getSuffixColumns() {
        return suffixColumns;
    }

    private static OutputStream defaultOutputStream(Path path) {
        try {
            return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream defaultInputStream(Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
        String metaKey = metaKey(dataTable.getName(), dataTable.getGroup());
        rowMetadata.computeIfAbsent(metaKey, k -> RowMetadata.of(dataTable.getType()));
        knownTables.putIfAbsent(fileKey(dataTable), dataTable);
        String fileKey = fileKey(dataTable);
        BucketWriter writer = writers.computeIfAbsent(fileKey, k -> createBucketWriter(dataTable));
        writer.writeRow(row);
    }

    @Deprecated
    @MustBeClosed
    @Override
    public Stream<?> getRows(String dataTableName, @Nullable String group) {
        RowMetadata meta = rowMetadata.get(metaKey(dataTableName, group));
        return readRows(dataTableName, group, meta);
    }

    @MustBeClosed
    @SuppressWarnings("unchecked")
    @Override
    public <Row> Stream<Row> getRows(Class<? extends DataTable<Row>> dataTableClass, @Nullable String group) {
        Class<Row> rowType = (Class<Row>) ((ParameterizedType) dataTableClass.getGenericSuperclass())
                .getActualTypeArguments()[0];
        return readRows(dataTableClass.getName(), group, RowMetadata.of(rowType));
    }

    @SuppressWarnings("unchecked")
    private <T> Stream<T> readRows(String dataTableName, @Nullable String group, @Nullable RowMetadata meta) {
        // Close (not just flush) matching writers so that compression trailers
        // (e.g., GZIP footer) are written, making the files fully readable.
        // Removed writers will be lazily re-created in append mode on the next insertRow().
        Iterator<Map.Entry<String, BucketWriter>> it = writers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BucketWriter> entry = it.next();
            BucketWriter writer = entry.getValue();
            if (writer.dataTable.getName().equals(dataTableName) &&
                Objects.equals(writer.dataTable.getGroup(), group)) {
                writer.close();
                it.remove();
            }
        }

        //noinspection DataFlowIssue
        File[] files = outputDir.toFile().listFiles((dir, name) -> name.endsWith(fileExtension));
        if (files == null) {
            return Stream.empty();
        }

        // Build set of file paths with still-open writers (other tables).
        // These files have incomplete compression trailers and cannot be read.
        Set<Path> activeWriterPaths = new HashSet<>();
        for (Map.Entry<String, BucketWriter> entry : writers.entrySet()) {
            activeWriterPaths.add(outputDir.resolve(entry.getKey() + fileExtension));
        }

        // Select the files belonging to this table by reading only their small comment
        // header, then parse their rows lazily so a whole table is never held in memory.
        List<Path> matchingFiles = new ArrayList<>();
        for (File file : files) {
            if (activeWriterPaths.contains(file.toPath())) {
                continue;
            }
            try (InputStream is = inputStreamFactory.apply(file.toPath())) {
                DataTableDescriptor descriptor = readDescriptor(is);
                if (descriptor != null &&
                    descriptor.getName().equals(dataTableName) &&
                    Objects.equals(descriptor.getGroup(), group)) {
                    matchingFiles.add(file.toPath());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        RowSpliterator rows = new RowSpliterator(matchingFiles, meta);
        return (Stream<T>) StreamSupport.stream(rows, false).onClose(rows::close);
    }

    /**
     * Streams rows from the matching files one at a time, keeping a single file open as it
     * goes. The file is closed as soon as its last row is read, so a fully drained stream
     * (the way every caller consumes one) releases its handle without any explicit close.
     * Closing the stream early (try-with-resources) also releases the open file.
     */
    private final class RowSpliterator extends Spliterators.AbstractSpliterator<Object> {
        private final Iterator<Path> paths;
        private final @Nullable RowMetadata meta;
        private final int prefixCount = prefixColumns.size();
        private final int suffixCount = suffixColumns.size();

        private @Nullable InputStream is;
        private @Nullable CsvParser parser;

        RowSpliterator(List<Path> matchingFiles, @Nullable RowMetadata meta) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL);
            this.paths = matchingFiles.iterator();
            this.meta = meta;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Object> action) {
            while (true) {
                if (parser == null) {
                    if (!paths.hasNext()) {
                        return false;
                    }
                    open(paths.next());
                }
                String[] row = parser.parseNext();
                if (row == null) {
                    close();
                    continue;
                }
                // Strip prefix and suffix columns, returning only data table columns
                int dataCount = row.length - prefixCount - suffixCount;
                String[] dataRow;
                if (dataCount <= 0) {
                    dataRow = row;
                } else {
                    dataRow = new String[dataCount];
                    System.arraycopy(row, prefixCount, dataRow, 0, dataCount);
                }
                action.accept(meta != null ? meta.toRow(dataRow) : dataRow);
                return true;
            }
        }

        // Assign the stream before creating the parser so a setup failure still leaves
        // the open stream visible to close().
        private void open(Path path) {
            is = inputStreamFactory.apply(path);
            CsvParserSettings settings = new CsvParserSettings();
            settings.setMaxCharsPerColumn(-1);
            settings.setHeaderExtractionEnabled(true);
            settings.getFormat().setComment('#');
            parser = new CsvParser(settings);
            parser.beginParsing(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        void close() {
            if (parser != null) {
                parser.stopParsing();
                parser = null;
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                    // best effort; the parser may have already closed it at end-of-input
                }
                is = null;
            }
        }
    }

    @Override
    public Collection<DataTable<?>> getDataTables() {
        return Collections.unmodifiableCollection(knownTables.values());
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
        boolean append = Files.exists(path);

        DataTableDescriptor descriptor = dataTableDescriptorFromDataTable(dataTable);
        List<String> fieldNames = new ArrayList<>();
        for (ColumnDescriptor col : descriptor.getColumns()) {
            fieldNames.add(col.getName());
        }

        // Build headers: prefix + data table columns + suffix
        List<String> headers = new ArrayList<>(prefixColumns.keySet());
        for (ColumnDescriptor col : descriptor.getColumns()) {
            headers.add(col.getName());
        }
        headers.addAll(suffixColumns.keySet());

        // Writers sharing one file must agree on column order; fail loud rather than misalign rows.
        if (append) {
            validateExistingHeader(path, headers);
        }

        OutputStream os = outputStreamFactory.apply(path);
        try {
            CsvWriterSettings settings = new CsvWriterSettings();
            settings.setHeaderWritingEnabled(!append);
            settings.getFormat().setComment('#');
            CsvWriter csvWriter = new CsvWriter(os, settings);

            if (!append) {
                // Write metadata as comments only for new files
                csvWriter.commentRow(" @name " + dataTable.getName());
                csvWriter.commentRow(" @instanceName " + dataTable.getInstanceName());
                csvWriter.commentRow(" @group " + (dataTable.getGroup() != null ? dataTable.getGroup() : ""));
                csvWriter.writeHeaders(headers);
            }

            return new BucketWriter(dataTable, csvWriter, os, fieldNames, headers.size());
        } catch (Exception e) {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    private void validateExistingHeader(Path path, List<String> expectedHeaders) {
        try (InputStream is = inputStreamFactory.apply(path)) {
            CsvParserSettings settings = new CsvParserSettings();
            settings.setMaxCharsPerColumn(-1);
            settings.getFormat().setComment('#');
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(new InputStreamReader(is, StandardCharsets.UTF_8));
            String[] existing = parser.parseNext();
            parser.stopParsing();
            if (existing != null && !Arrays.asList(existing).equals(expectedHeaders)) {
                throw new IllegalStateException(
                        "Data table file " + path.getFileName() + " has header " + Arrays.toString(existing) +
                        " but a writer expected " + expectedHeaders + ". Writers sharing a data table file " +
                        "must agree on column order.");
            }
        } catch (IOException e) {
            // Existing file is unreadable here (e.g. not yet flushed); skip the guard and proceed.
        }
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
            // Flush per row so a shared file always ends at a complete line.
            csvWriter.flush();
        }

        void close() {
            csvWriter.close();
            try {
                os.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String metaKey(String dataTableName, @Nullable String group) {
        return dataTableName + "\0" + (group != null ? group : "");
    }

    /**
     * Caches the {@link Column @Column} field names for a row class so they
     * are only computed once, and converts CSV {@code String[]} rows back to
     * typed objects via Jackson.
     */
    private static class RowMetadata {
        final Class<?> rowClass;
        final List<String> fieldNames;

        private RowMetadata(Class<?> rowClass, List<String> fieldNames) {
            this.rowClass = rowClass;
            this.fieldNames = fieldNames;
        }

        static RowMetadata of(Class<?> rowClass) {
            List<String> names = new ArrayList<>();
            for (Field f : rowClass.getDeclaredFields()) {
                if (f.isAnnotationPresent(Column.class)) {
                    names.add(f.getName());
                }
            }
            return new RowMetadata(rowClass, names);
        }

        Object toRow(String[] values) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < fieldNames.size(); i++) {
                map.put(fieldNames.get(i), i < values.length ? values[i] : "");
            }
            return ROW_MAPPER.convertValue(map, rowClass);
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
        if (group != null) {
            if (group.equals(dataTable.getName())) {
                return dataTable.getName();
            }
            return dataTable.getName() + "--" + sanitize(group);
        }
        // Only add a suffix when the instance name was explicitly customized
        // (i.e. differs from the default display name)
        String instanceName = dataTable.getInstanceName();
        if (instanceName.equals(dataTable.getDisplayName())) {
            return dataTable.getName();
        }
        return dataTable.getName() + "--" + sanitize(instanceName);
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
        String hash = sha256Prefix(value);
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

    private static String sha256Prefix(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
                if (hex.length() >= 4) {
                    break;
                }
            }
            return hex.substring(0, Math.min(4, hex.length()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
