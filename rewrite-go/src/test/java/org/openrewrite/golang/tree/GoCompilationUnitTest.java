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
package org.openrewrite.golang.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class GoCompilationUnitTest {

    private static J.Import goImport(String path) {
        J.FieldAccess qualid = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY,
                new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                JLeftPadded.build(new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY,
                        emptyList(), path, null, null)),
                null);
        return new J.Import(randomId(), Space.EMPTY, Markers.EMPTY,
                JLeftPadded.build(false), qualid, null);
    }

    private static Go.CompilationUnit compilationUnit(JContainer<J.Import> imports) {
        return new Go.CompilationUnit(randomId(), Space.EMPTY, Markers.EMPTY, Paths.get("main.go"),
                null, null, false, null, null, imports, emptyList(), Space.EMPTY);
    }

    @Test
    void withSameImportsReturnsSameInstance() {
        // given
        JContainer<J.Import> imports = JContainer.build(Space.SINGLE_SPACE,
                JRightPadded.withElements(emptyList(), singletonList(goImport("\"fmt\""))), Markers.EMPTY);
        Go.CompilationUnit cu = compilationUnit(imports);

        // when
        Go.CompilationUnit result = cu.withImports(cu.getImports());

        // then
        assertThat(result).isSameAs(cu);
    }

    @Test
    void withDifferentImportsReturnsNewInstancePreservingContainer() {
        // given
        Markers containerMarkers = Markers.build(singletonList(new SearchResult(randomId(), null)));
        JContainer<J.Import> imports = JContainer.build(Space.SINGLE_SPACE,
                JRightPadded.withElements(emptyList(), singletonList(goImport("\"fmt\""))), containerMarkers);
        Go.CompilationUnit cu = compilationUnit(imports);
        List<J.Import> newImports = singletonList(goImport("\"os\""));

        // when
        Go.CompilationUnit result = cu.withImports(newImports);

        // then
        assertThat(result).isNotSameAs(cu);
        assertThat(result.getImports()).isEqualTo(newImports);
        assertThat(result.getImportsContainer()).isNotNull();
        assertThat(result.getImportsContainer().getBefore()).isSameAs(imports.getBefore());
        assertThat(result.getImportsContainer().getMarkers()).isSameAs(containerMarkers);
    }
}
