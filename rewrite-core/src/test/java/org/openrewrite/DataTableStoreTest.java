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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("DataFlowIssue")
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
        //noinspection EqualsWithItself
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
    // CsvDataTableStore.getRows
    // =========================================================================

    static class MultiColTable extends DataTable<MultiColTable.Row> {
        public MultiColTable(Recipe recipe) {
            super(recipe, "Multi-column table", "A table with multiple columns.");
        }

        @Value
        public static class Row {
            @Column(displayName = "Position", description = "The index position")
            int position;

            @Column(displayName = "Text", description = "The text value")
            String text;
        }
    }

    @Test
    void csvStoreGetRowsReadsBackWrittenData(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
            store.insertRow(table, ctx(), new TestTable.Row("bob"));

            List<?> rows = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)).isEqualTo(new TestTable.Row("alice"));
            assertThat(rows.get(1)).isEqualTo(new TestTable.Row("bob"));
        }
    }

    @Test
    void csvStoreGetRowsMultipleColumns(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            MultiColTable table = new MultiColTable(Recipe.noop());
            store.insertRow(table, ctx(), new MultiColTable.Row(1, "hello"));
            store.insertRow(table, ctx(), new MultiColTable.Row(2, "world"));

            List<?> rows = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)).isEqualTo(new MultiColTable.Row(1, "hello"));
            assertThat(rows.get(1)).isEqualTo(new MultiColTable.Row(2, "world"));
        }
    }

    @Test
    void csvStoreGetRowsReturnsEmptyForMissingTable(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("alice"));

            List<?> rows = store.getRows("nonexistent.Table", null).collect(Collectors.toList());
            assertThat(rows).isEmpty();
        }
    }

    @Test
    void csvStoreGetRowsMatchesByGroup(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable grouped = new TestTable(Recipe.noop()).withGroup("group-a");
            TestTable ungrouped = new TestTable(Recipe.noop());
            store.insertRow(grouped, ctx(), new TestTable.Row("grouped-row"));
            store.insertRow(ungrouped, ctx(), new TestTable.Row("ungrouped-row"));

            List<?> groupedRows = store.getRows(grouped.getName(), "group-a").collect(Collectors.toList());
            assertThat(groupedRows).hasSize(1);
            assertThat(groupedRows.getFirst()).isEqualTo(new TestTable.Row("grouped-row"));

            List<?> ungroupedRows = store.getRows(ungrouped.getName(), null).collect(Collectors.toList());
            assertThat(ungroupedRows).hasSize(1);
            assertThat(ungroupedRows.getFirst()).isEqualTo(new TestTable.Row("ungrouped-row"));
        }
    }

    @Test
    void csvStoreGetRowsStripsPrefixAndSuffixColumns(@TempDir Path tempDir) {
        Map<String, String> prefix = new LinkedHashMap<>();
        prefix.put("repo", "my-repo");
        Map<String, String> suffix = new LinkedHashMap<>();
        suffix.put("org", "my-org");

        try (CsvDataTableStore store = new CsvDataTableStore(
                tempDir, (path) -> {
            try {
                return Files.newOutputStream(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, (path) -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ".csv", prefix, suffix)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("alice"));

            List<?> rows = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(rows).hasSize(1);
            // Should only contain the data column, not prefix/suffix
            assertThat(rows.getFirst()).isEqualTo(new TestTable.Row("alice"));
        }
    }

    @Test
    void csvStoreGetRowsAfterClose(@TempDir Path tempDir) {
        TestTable table = new TestTable(Recipe.noop());
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
            store.insertRow(table, ctx(), new TestTable.Row("bob"));
        }

        // Read back from a new store instance pointing at the same directory
        try (CsvDataTableStore store2 = new CsvDataTableStore(tempDir)) {
            List<?> rows = store2.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(rows).hasSize(2);
            assertThat((String[]) rows.get(0)).containsExactly("alice");
            assertThat((String[]) rows.get(1)).containsExactly("bob");
        }
    }

    @Test
    void csvStoreGetRowsHandlesSpecialCharacters(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());
            store.insertRow(table, ctx(), new TestTable.Row("value with, comma"));
            store.insertRow(table, ctx(), new TestTable.Row("value with \"quotes\""));
            store.insertRow(table, ctx(), new TestTable.Row("value with\nnewline"));

            List<?> rows = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(rows).hasSize(3);
            assertThat(rows.get(0)).isEqualTo(new TestTable.Row("value with, comma"));
            assertThat(rows.get(1)).isEqualTo(new TestTable.Row("value with \"quotes\""));
            assertThat(rows.get(2)).isEqualTo(new TestTable.Row("value with\nnewline"));
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

    // =========================================================================
    // CsvDataTableStore: write in cycle 1, read back in cycle 2
    // =========================================================================

    /**
     * A scanning recipe that writes to a data table during cycle 1's scanner,
     * then reads rows back from the DataTableStore in cycle 2's getInitialValue.
     */
    static class WriteThenReadRecipe extends ScanningRecipe<List<String>> {
        transient TestTable table = new TestTable(this);

        @Override
        public String getDisplayName() {
            return "Write then read";
        }

        @Override
        public String getDescription() {
            return "Writes data table rows in cycle 1, reads them back in cycle 2.";
        }

        @Override
        public boolean causesAnotherCycle() {
            return true;
        }

        @Override
        public List<String> getInitialValue(ExecutionContext ctx) {
            // On cycle 2+, the store already contains rows written in cycle 1
            List<String> readBack = new ArrayList<>();
            DataTableStore store = DataTableExecutionContextView.view(ctx).getDataTableStore();
            try (Stream<?> rows = store.getRows(table.getName(), null)) {
                rows.forEach(row -> {
                    if (row instanceof TestTable.Row) {
                        readBack.add(((TestTable.Row) row).getName());
                    } else {
                        readBack.add(((String[]) row)[0]);
                    }
                });
            }
            return readBack;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(List<String> acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    // DataTable.insertRow only writes during cycle 1
                    table.insertRow(ctx, new TestTable.Row(text.getText()));
                    return text;
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(List<String> acc) {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (acc.isEmpty()) {
                        // Cycle 1: no read-back data yet; make a change to trigger cycle 2
                        return text.withText(text.getText() + "-scanned");
                    }
                    // Cycle 2: append the data read back from the store
                    return text.withText(text.getText() + "-read:" + String.join(",", acc));
                }
            };
        }
    }

    @Test
    void csvStoreIntermixedWritesAndReads(@TempDir Path tempDir) {
        try (CsvDataTableStore store = new CsvDataTableStore(tempDir)) {
            TestTable table = new TestTable(Recipe.noop());

            // First batch of writes
            store.insertRow(table, ctx(), new TestTable.Row("alice"));
            store.insertRow(table, ctx(), new TestTable.Row("bob"));

            // Mid-run read (closes the writer internally)
            List<?> firstRead = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(firstRead).hasSize(2);
            assertThat(firstRead.get(0)).isEqualTo(new TestTable.Row("alice"));
            assertThat(firstRead.get(1)).isEqualTo(new TestTable.Row("bob"));

            // Second batch of writes (writer re-created in append mode)
            store.insertRow(table, ctx(), new TestTable.Row("charlie"));

            // Second read should see all three rows
            List<?> secondRead = store.getRows(table.getName(), null).collect(Collectors.toList());
            assertThat(secondRead).hasSize(3);
            assertThat(secondRead.get(0)).isEqualTo(new TestTable.Row("alice"));
            assertThat(secondRead.get(1)).isEqualTo(new TestTable.Row("bob"));
            assertThat(secondRead.get(2)).isEqualTo(new TestTable.Row("charlie"));
        }
    }

    @Test
    void csvStoreWriteInCycle1ReadBackInCycle2(@TempDir Path tempDir) {
        ExecutionContext ctx = ctx();
        DataTableExecutionContextView.view(ctx).setDataTableStore(new CsvDataTableStore(tempDir));

        List<SourceFile> sources = List.of(
                PlainText.builder().text("hello").sourcePath(Path.of("test.txt")).build()
        );

        RecipeRun run = new RecipeScheduler().scheduleRun(
                new WriteThenReadRecipe(),
                new InMemoryLargeSourceSet(sources),
                ctx, 2, 1
        );

        // The recipe should have run 2 cycles:
        //   Cycle 1: scanner writes "hello" to data table, visitor appends "-scanned"
        //   Cycle 2: getInitialValue reads back ["hello"], visitor appends "-read:hello"
        PlainText after = (PlainText) run.getChangeset().getAllResults().getFirst().getAfter();
        assertThat(after).isNotNull();
        assertThat(after.getText()).isEqualTo("hello-scanned-read:hello");
    }
}
