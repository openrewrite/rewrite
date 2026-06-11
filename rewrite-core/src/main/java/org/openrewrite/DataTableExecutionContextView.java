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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Collections.emptyMap;

public class DataTableExecutionContextView extends DelegatingExecutionContext {
    public static final String DATA_TABLE_STORE = "org.openrewrite.dataTables.store";

    /**
     * Configuration for constructing a {@link CsvDataTableStore} locally. These keys
     * use {@link ExecutionContext#RPC_SHARED_MESSAGE_PREFIX} so that the configuration
     * travels to remote RPC peers along with the context, allowing each peer to write
     * data table rows to its own files in the shared output directory. Values are
     * restricted to strings and lists of strings so every peer language can read them.
     */
    public static final String DATA_TABLE_STORE_OUTPUT_DIR = ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "dataTableOutputDir";
    public static final String DATA_TABLE_STORE_FILE_EXTENSION = ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "dataTableFileExtension";
    public static final String DATA_TABLE_STORE_PREFIX_COLUMNS = ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "dataTablePrefixColumns";
    public static final String DATA_TABLE_STORE_SUFFIX_COLUMNS = ExecutionContext.RPC_SHARED_MESSAGE_PREFIX + "dataTableSuffixColumns";

    private DataTableExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static DataTableExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof DataTableExecutionContextView) {
            return (DataTableExecutionContextView) ctx;
        }
        return new DataTableExecutionContextView(ctx);
    }

    public DataTableExecutionContextView setDataTableStore(DataTableStore store) {
        putMessage(DATA_TABLE_STORE, store);
        return this;
    }

    /**
     * Configure how data table rows are persisted to CSV files. The configuration is
     * stored as RPC-shared messages, so when this context is transferred to a remote
     * RPC peer, the peer installs its own local {@link CsvDataTableStore} from it and
     * streams rows to disk in the same output directory. Every store created from this
     * configuration (local or remote) writes files with a unique name suffix, so
     * concurrently open (possibly compressed) streams never collide.
     *
     * @param outputDir     directory to write files into
     * @param fileExtension file extension including dot, {@code .csv} or {@code .csv.gz}
     *                      ({@code .gz} implies GZIP compression)
     * @param prefixColumns static columns prepended to each row, in iteration order
     * @param suffixColumns static columns appended to each row, in iteration order
     */
    public DataTableExecutionContextView setDataTableStoreConfig(Path outputDir, String fileExtension,
                                                                 Map<String, String> prefixColumns,
                                                                 Map<String, String> suffixColumns) {
        putMessage(DATA_TABLE_STORE_OUTPUT_DIR, outputDir.toString());
        putMessage(DATA_TABLE_STORE_FILE_EXTENSION, fileExtension);
        putMessage(DATA_TABLE_STORE_PREFIX_COLUMNS, flattenColumns(prefixColumns));
        putMessage(DATA_TABLE_STORE_SUFFIX_COLUMNS, flattenColumns(suffixColumns));
        return this;
    }

    public DataTableStore getDataTableStore() {
        DataTableStore store = getMessage(DATA_TABLE_STORE);
        if (store == null) {
            String outputDir = getMessage(DATA_TABLE_STORE_OUTPUT_DIR);
            store = outputDir == null ? new InMemoryDataTableStore() : csvDataTableStore(Paths.get(outputDir));
            putMessage(DATA_TABLE_STORE, store);
        }
        return store;
    }

    private CsvDataTableStore csvDataTableStore(Path outputDir) {
        String fileExtension = getMessage(DATA_TABLE_STORE_FILE_EXTENSION, ".csv");
        // A unique per-store suffix keeps this store's files separate from those of
        // other processes (or other contexts) writing to the same directory.
        String uniqueExtension = "-" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        Map<String, String> prefixColumns = unflattenColumns(getMessage(DATA_TABLE_STORE_PREFIX_COLUMNS));
        Map<String, String> suffixColumns = unflattenColumns(getMessage(DATA_TABLE_STORE_SUFFIX_COLUMNS));
        if (fileExtension.endsWith(".gz")) {
            return new CsvDataTableStore(outputDir,
                    path -> {
                        try {
                            return new GZIPOutputStream(Files.newOutputStream(path,
                                    StandardOpenOption.CREATE, StandardOpenOption.APPEND));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    },
                    path -> {
                        try {
                            return new GZIPInputStream(Files.newInputStream(path));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    },
                    uniqueExtension, prefixColumns, suffixColumns);
        }
        return new CsvDataTableStore(outputDir,
                path -> {
                    try {
                        return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                path -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                uniqueExtension, prefixColumns, suffixColumns);
    }

    private static List<String> flattenColumns(Map<String, String> columns) {
        List<String> flattened = new ArrayList<>(columns.size() * 2);
        for (Map.Entry<String, String> column : columns.entrySet()) {
            flattened.add(column.getKey());
            flattened.add(column.getValue());
        }
        return flattened;
    }

    private static Map<String, String> unflattenColumns(@Nullable List<String> flattened) {
        if (flattened == null || flattened.isEmpty()) {
            return emptyMap();
        }
        Map<String, String> columns = new LinkedHashMap<>();
        for (int i = 0; i + 1 < flattened.size(); i += 2) {
            columns.put(flattened.get(i), flattened.get(i + 1));
        }
        return columns;
    }
}
