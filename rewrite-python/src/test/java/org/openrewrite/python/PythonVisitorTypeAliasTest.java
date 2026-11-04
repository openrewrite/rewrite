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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.Py;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds the node rather than parsing one, so this runs without the Python peer that
 * {@link PythonVisitorCompletenessTest} needs.
 */
class PythonVisitorTypeAliasTest {

    @Test
    void visitsTypeParameterBounds() {
        Py.TypeAlias alias = typeAlias(ident("Old"));

        Py.TypeAlias renamed = (Py.TypeAlias) new PythonVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                return "Old".equals(ident.getSimpleName()) ? ident.withSimpleName("New") : ident;
            }
        }.visitTypeAlias(alias, 0);

        assertThat(boundName(renamed)).isEqualTo("New");
    }

    private static Py.TypeAlias typeAlias(J.Identifier bound) {
        J.TypeParameter typeParam = new J.TypeParameter(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), emptyList(), ident("T"),
                JContainer.build(Space.EMPTY, List.of(JRightPadded.build((TypeTree) bound)), Markers.EMPTY));
        return new Py.TypeAlias(
                Tree.randomId(), Space.EMPTY, Markers.EMPTY, ident("X"),
                JContainer.build(Space.EMPTY, List.of(JRightPadded.build(typeParam)), Markers.EMPTY),
                JLeftPadded.build((J) ident("int")).withBefore(Space.SINGLE_SPACE),
                null);
    }

    private static String boundName(Py.TypeAlias alias) {
        //noinspection DataFlowIssue
        return ((J.Identifier) alias.getTypeParameters().get(0).getBounds().get(0)).getSimpleName();
    }

    private static J.Identifier ident(String name) {
        return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }
}
