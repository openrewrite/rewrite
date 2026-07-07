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
package org.openrewrite.rpc.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.*;
import org.openrewrite.table.TextMatches;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SetDataTableStoreTest {

    @Test
    void onlyCsvAndNoopAreConveyable() {
        assertThat(SetDataTableStore.from(null)).isNull();

        assertThat(SetDataTableStore.from(DataTableStore.noop()))
          .isInstanceOf(SetDataTableStore.NoOp.class);

        DataTableStore custom = new DataTableStore() {
            @Override
            public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
            }

            @Override
            public java.util.stream.Stream<?> getRows(String dataTableName, String group) {
                return java.util.stream.Stream.empty();
            }

            @Override
            public java.util.Collection<DataTable<?>> getDataTables() {
                return Collections.emptyList();
            }
        };
        assertThat(SetDataTableStore.from(custom)).isNull();
    }

    @Test
    void csvConfigRoundTripsThroughWireFormAndWritesRows(@TempDir Path tmp) throws IOException {
        Map<String, String> prefix = new LinkedHashMap<>();
        prefix.put("repositoryOrigin", "github.com/acme/example");

        CsvDataTableStore original = new CsvDataTableStore(tmp, prefix, Collections.emptyMap());

        SetDataTableStore wire = SetDataTableStore.from(original);
        assertThat(wire).isInstanceOf(SetDataTableStore.Csv.class);
        SetDataTableStore.Csv csvWire = (SetDataTableStore.Csv) wire;
        assertThat(csvWire.getPrefixColumns()).containsEntry("repositoryOrigin", "github.com/acme/example");
        assertThat(Paths.get(csvWire.getOutputDir())).isEqualTo(tmp.toAbsolutePath().normalize());

        DataTableStore rebuilt = wire.toDataTableStore();
        assertThat(rebuilt).isInstanceOf(CsvDataTableStore.class);

        TextMatches table = new TextMatches(Recipe.noop());
        rebuilt.insertRow(table, new InMemoryExecutionContext(), new TextMatches.Row("a.txt", "ctx"));
        ((CsvDataTableStore) rebuilt).close();

        Path csv = tmp.resolve(table.getName() + ".csv");
        assertThat(csv).exists();
        String content = new String(Files.readAllBytes(csv), StandardCharsets.UTF_8);
        assertThat(content)
          .as("prefix column header + value and the data row are present")
          .contains("repositoryOrigin")
          .contains("github.com/acme/example")
          .contains("a.txt");
    }

    @Test
    void noopWireFormReconstructsNoop() {
        SetDataTableStore wire = SetDataTableStore.from(DataTableStore.noop());
        assertThat(wire).isInstanceOf(SetDataTableStore.NoOp.class);
        assertThat(wire.toDataTableStore()).isSameAs(DataTableStore.noop());
    }
}
