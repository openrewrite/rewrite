/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Paths;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class GoFileCheckerTest {

    private static Go.CompilationUnit goCompilationUnit() {
        return new Go.CompilationUnit(randomId(), Space.EMPTY, Markers.EMPTY, Paths.get("main.go"),
                null, null, false, null, null, null, emptyList(), Space.EMPTY);
    }

    @Test
    void marksGoSourceFile() {
        // given
        Go.CompilationUnit cu = goCompilationUnit();

        // when
        Tree result = new GoFileChecker<>().visit(cu, new InMemoryExecutionContext());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMarkers().findFirst(SearchResult.class)).isPresent();
    }

    @Test
    void ignoresNonGoTree() {
        // given
        J.Empty notGo = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);

        // when
        Tree result = new GoFileChecker<>().visit(notGo, new InMemoryExecutionContext());

        // then
        assertThat(result).isSameAs(notGo);
        assertThat(result.getMarkers().findFirst(SearchResult.class)).isEmpty();
    }
}
