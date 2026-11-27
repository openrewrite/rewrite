/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.DeserializationError;
import org.openrewrite.marker.LstProvenance;
import org.openrewrite.marker.OutdatedSerializer;
import org.openrewrite.table.DeserializationErrorTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;

class FindDeserializationErrorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindDeserializationErrors());
    }

    @DocumentExample
    @Test
    void reportDeserializationError() {
        Instant timestamp = Instant.parse("2026-01-02T03:04:05Z");
        rewriteRun(
          spec -> spec.dataTable(DeserializationErrorTable.Row.class, rows -> {
              assertThat(rows).hasSize(1);
              DeserializationErrorTable.Row row = rows.getFirst();
              assertThat(row.getSourcePath()).isEqualTo("file.txt");
              assertThat(row.getErrorMessage()).isEqualTo("Unknown LST element");
              assertThat(row.getStackTrace()).contains("boom");
              assertThat(row.getLanguage()).isEqualTo("Python");
              assertThat(row.getMinimumVersion()).isEqualTo("3.0.0");
              assertThat(row.getActualVersion()).isEqualTo("2.0.0");
              assertThat(row.getTimestampEpochMillis()).isEqualTo(timestamp.toEpochMilli());
          }),
          text(
            "hello",
            spec -> spec.path("file.txt").markers(
              new DeserializationError(randomId(), "Unknown LST element", new IllegalStateException("boom")),
              new OutdatedSerializer(randomId(), "Python", "3.0.0", "2.0.0"),
              new LstProvenance(randomId(), LstProvenance.Type.Cli, "1.0.0", "1", timestamp))
          )
        );
    }
}
