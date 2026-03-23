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

import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DataTableStoreTest {

    static class TestTable extends DataTable<TestTable.Row> {
        public TestTable(Recipe recipe) {
            super(recipe, "Test table", "A test data table.");
        }

        @Value
        public static class Row {
            @Column(displayName = "Name", description = "The name")
            String name;
        }
    }

    private ExecutionContext ctx() {
        return new InMemoryExecutionContext();
    }

    // =========================================================================
    // InMemoryDataTableStore
    // =========================================================================

    @Test
    void insertAndRetrieveRows() {
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        TestTable table = new TestTable(Recipe.noop());
        store.insertRow(table, ctx(), new TestTable.Row("alice"));
        store.insertRow(table, ctx(), new TestTable.Row("bob"));

        List<?> rows = store.getRows(table.getName(), null).collect(Collectors.toList());
        assertThat(rows).hasSize(2);
    }

    @Test
    void getRowsReturnsEmptyForMissingTable() {
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        assertThat(store.getRows("nonexistent", null).count()).isZero();
    }

    @Test
    void getDataTablesReturnsInsertedTables() {
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        TestTable table = new TestTable(Recipe.noop());
        store.insertRow(table, ctx(), new TestTable.Row("alice"));

        assertThat(store.getDataTables()).hasSize(1);
        assertThat(store.getDataTables().iterator().next().getName()).isEqualTo(TestTable.class.getName());
    }

    @Test
    void groupedTablesShareBucket() {
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        ExecutionContext ctx = ctx();

        TestTable table1 = new TestTable(Recipe.noop()).withGroup("shared");
        TestTable table2 = new TestTable(Recipe.noop()).withGroup("shared");

        store.insertRow(table1, ctx, new TestTable.Row("from-recipe-1"));
        store.insertRow(table2, ctx, new TestTable.Row("from-recipe-2"));

        // Both contributed to the same group bucket
        List<?> rows = store.getRows(TestTable.class.getName(), "shared").collect(Collectors.toList());
        assertThat(rows).hasSize(2);

        // Only one bucket entry (first one wins as representative)
        assertThat(store.getDataTables()).hasSize(1);
    }

    @Test
    void ungroupedTablesGetSeparateBuckets() {
        InMemoryDataTableStore store = new InMemoryDataTableStore();
        ExecutionContext ctx = ctx();

        TestTable table1 = new TestTable(Recipe.noop()).withInstanceName(() -> "instance-a");
        TestTable table2 = new TestTable(Recipe.noop()).withInstanceName(() -> "instance-b");

        store.insertRow(table1, ctx, new TestTable.Row("from-a"));
        store.insertRow(table2, ctx, new TestTable.Row("from-b"));

        // Two separate buckets
        assertThat(store.getDataTables()).hasSize(2);
    }

    // =========================================================================
    // DataTable identity
    // =========================================================================

    @Test
    void instanceNameDefaultsToDisplayName() {
        TestTable table = new TestTable(Recipe.noop());
        assertThat(table.getInstanceName()).isEqualTo("Test table");
    }

    @Test
    void instanceNameFromSupplier() {
        TestTable table = new TestTable(Recipe.noop())
                .withInstanceName(() -> "custom name");
        assertThat(table.getInstanceName()).isEqualTo("custom name");
    }

    @Test
    void groupIsNullByDefault() {
        TestTable table = new TestTable(Recipe.noop());
        assertThat(table.getGroup()).isNull();
    }

    @Test
    void groupSetByWithGroup() {
        TestTable table = new TestTable(Recipe.noop()).withGroup("architecture");
        assertThat(table.getGroup()).isEqualTo("architecture");
    }

    // =========================================================================
    // DataTableStore.noop()
    // =========================================================================

    @Test
    void noopStoreDropsInserts() {
        DataTableStore store = DataTableStore.noop();
        TestTable table = new TestTable(Recipe.noop());
        store.insertRow(table, ctx(), new TestTable.Row("dropped"));

        assertThat(store.getRows(table.getName(), null).count()).isZero();
        assertThat(store.getDataTables()).isEmpty();
    }

    @Test
    void noopStoreIsSingleton() {
        assertThat(DataTableStore.noop()).isSameAs(DataTableStore.noop());
    }

    // =========================================================================
    // DataTableExecutionContextView
    // =========================================================================

    @Test
    void viewLazyInitializesStore() {
        ExecutionContext ctx = ctx();
        DataTableStore store = DataTableExecutionContextView.view(ctx).getDataTableStore();
        assertThat(store).isNotNull();
        assertThat(store).isInstanceOf(InMemoryDataTableStore.class);
    }

    @Test
    void viewSharesStoreAcrossCalls() {
        ExecutionContext ctx = ctx();
        DataTableStore store1 = DataTableExecutionContextView.view(ctx).getDataTableStore();
        DataTableStore store2 = DataTableExecutionContextView.view(ctx).getDataTableStore();
        assertThat(store1).isSameAs(store2);
    }

    @Test
    void viewSharesStoreThroughDelegation() {
        ExecutionContext ctx = ctx();
        DelegatingExecutionContext wrapped = new DelegatingExecutionContext(ctx) {};

        DataTableStore storeFromWrapped = DataTableExecutionContextView.view(wrapped).getDataTableStore();
        DataTableStore storeFromOriginal = DataTableExecutionContextView.view(ctx).getDataTableStore();
        assertThat(storeFromWrapped).isSameAs(storeFromOriginal);
    }

    @Test
    void setDataTableStoreOverridesDefault() {
        ExecutionContext ctx = ctx();
        DataTableStore custom = DataTableStore.noop();
        DataTableExecutionContextView.view(ctx).setDataTableStore(custom);

        assertThat(DataTableExecutionContextView.view(ctx).getDataTableStore()).isSameAs(custom);
    }

    // =========================================================================
    // CsvDataTableStore
    // =========================================================================

    @Test
    void csvStoreWritesFiles(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
            store.insertRow(table, ctx(), new TestTable.Row("bob"));
        }

        // Should have written a CSV file
        assertThat(tempDir.toFile().listFiles((dir, name) -> name.endsWith(".csv")))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void csvStoreWritesMetadataComments(@TempDir Path tempDir) throws Exception {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
        }

        Path csvFile = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"))[0].toPath();
        List<String> lines = Files.readAllLines(csvFile);
        assertThat(lines.get(0)).startsWith("# @name ");
        assertThat(lines.get(1)).startsWith("# @instanceName ");
        assertThat(lines.get(2)).startsWith("# @group ");
    }

    @Test
    void csvStoreReadDescriptor(@TempDir Path tempDir) throws Exception {
        TestTable table = new TestTable(Recipe.noop()).withGroup("test-group");
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
        }

        Path csvFile = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"))[0].toPath();
        try (FileInputStream fis = new FileInputStream(csvFile.toFile())) {
            var descriptor = CsvDataTableStore.readDescriptor(fis);
            assertThat(descriptor).isNotNull();
            assertThat(descriptor.getName()).isEqualTo(TestTable.class.getName());
            assertThat(descriptor.getInstanceName()).isEqualTo("Test table");
            assertThat(descriptor.getGroup()).isEqualTo("test-group");
        }
    }

    // =========================================================================
    // CsvDataTableStore.fileKey
    // =========================================================================

    @Test
    void fileKeyNoSuffixWhenInstanceNameNotCustomized() {
        TestTable table = new TestTable(Recipe.noop());
        assertThat(CsvDataTableStore.fileKey(table)).isEqualTo(TestTable.class.getName());
    }

    @Test
    void fileKeySuffixWhenInstanceNameCustomized() {
        TestTable table = new TestTable(Recipe.noop())
                .withInstanceName(() -> "custom instance");
        String key = CsvDataTableStore.fileKey(table);
        assertThat(key).startsWith(TestTable.class.getName() + "--");
    }

    @Test
    void fileKeySuffixFromGroup() {
        TestTable table = new TestTable(Recipe.noop()).withGroup("my-group");
        String key = CsvDataTableStore.fileKey(table);
        assertThat(key).startsWith(TestTable.class.getName() + "--");
    }

    // =========================================================================
    // CsvDataTableStore.sanitize
    // =========================================================================

    @Test
    void sanitizeSimpleString() {
        String result = CsvDataTableStore.sanitize("architecture");
        assertThat(result).startsWith("architecture-");
        assertThat(result).hasSize("architecture-".length() + 4); // 4-char hash
    }

    @Test
    void sanitizeComplexString() {
        String result = CsvDataTableStore.sanitize("Find methods 'java.util.List add(..)'");
        // Should be lowercased, special chars replaced, truncated, with hash
        assertThat(result).matches("[a-z0-9-]+");
        assertThat(result.length()).isLessThanOrEqualTo(35); // ~30 + dash + 4 hash
    }

    @Test
    void sanitizeProducesDeterministicOutput() {
        String a = CsvDataTableStore.sanitize("test input");
        String b = CsvDataTableStore.sanitize("test input");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void sanitizeDifferentInputsProduceDifferentHashes() {
        String a = CsvDataTableStore.sanitize("Find methods 'add(..)'");
        String b = CsvDataTableStore.sanitize("Find methods 'delete(..)'");
        assertThat(a).isNotEqualTo(b);
    }
}
