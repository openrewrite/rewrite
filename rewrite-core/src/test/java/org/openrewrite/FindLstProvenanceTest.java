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
import org.openrewrite.marker.LstProvenance;
import org.openrewrite.table.LstProvenanceTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;

class FindLstProvenanceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindLstProvenance());
    }

    @DocumentExample
    @Test
    void reportProvenanceOncePerRepository() {
        Instant timestamp = Instant.parse("2026-01-02T03:04:05Z");
        LstProvenance provenance = new LstProvenance(randomId(), LstProvenance.Type.Gradle, "8.14", "2.0.0", timestamp);
        rewriteRun(
          spec -> spec.dataTable(LstProvenanceTable.Row.class, rows ->
            assertThat(rows).containsExactly(new LstProvenanceTable.Row(LstProvenance.Type.Gradle, "8.14", "2.0.0",
              timestamp.toEpochMilli(), "2026-01-02T03:04:05Z"))),
          text("one", spec -> spec.path("one.txt").markers(provenance)),
          text("two", spec -> spec.path("two.txt").markers(provenance))
        );
    }
}
