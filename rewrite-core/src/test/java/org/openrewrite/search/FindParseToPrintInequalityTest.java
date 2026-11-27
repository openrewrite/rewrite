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
package org.openrewrite.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.ParseToPrintInequalities;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class FindParseToPrintInequalityTest {

    @Test
    void markParseErrorWhoseErroneousTreePrintsDifferently() {
        // given
        Path path = Path.of("file.txt");
        PlainText erroneous = PlainText.builder().sourcePath(path).text("hello world").build();
        ParseError parseError = new ParseError(randomId(), Markers.EMPTY, path, null, null, false, null,
          "hello", erroneous);
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new AssertionError(t);
        });

        // when
        RecipeRun run = new FindParseToPrintInequality().run(new InMemoryLargeSourceSet(List.of(parseError)), ctx);

        // then
        SourceFile after = run.getChangeset().getAllResults().getFirst().getAfter();
        assertThat(after).isNotNull();
        assertThat(after.getMarkers().findFirst(SearchResult.class)).isPresent();
        List<ParseToPrintInequalities.Row> rows = run.getDataTableRows(ParseToPrintInequalities.class);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getSourcePath()).isEqualTo("file.txt");
        assertThat(rows.getFirst().getDiff())
          .contains("-hello")
          .contains("+hello world");
    }
}
