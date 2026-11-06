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
package org.openrewrite.python.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.PythonVisitor;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class MatchCaseTest {

    @Test
    void withGuardPreservesType() {
        // given
        Py.MatchCase matchCase = matchCase(JavaType.Primitive.Boolean);
        JLeftPadded<Expression> newGuard = JLeftPadded.build((Expression) ident("other")).withBefore(Space.SINGLE_SPACE);

        // when
        Py.MatchCase result = matchCase.getPadding().withGuard(newGuard);

        // then
        assertThat(result).isNotSameAs(matchCase);
        assertThat(result.getType()).isSameAs(JavaType.Primitive.Boolean);
        assertThat(result.getPadding().getGuard()).isSameAs(newGuard);
    }

    @Test
    void visitorChangingGuardPreservesType() {
        // given
        Py.MatchCase matchCase = matchCase(JavaType.Primitive.Boolean);

        // when
        Py.MatchCase result = (Py.MatchCase) new PythonVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                return "guard".equals(ident.getSimpleName()) ? ident.withSimpleName("other") : ident;
            }
        }.visitMatchCase(matchCase, 0);

        // then
        assertThat(((J.Identifier) result.getGuard()).getSimpleName()).isEqualTo("other");
        assertThat(result.getType()).isSameAs(JavaType.Primitive.Boolean);
    }

    private static Py.MatchCase matchCase(JavaType type) {
        Py.MatchCase.Pattern pattern = new Py.MatchCase.Pattern(
                randomId(), Space.EMPTY, Markers.EMPTY, Py.MatchCase.Pattern.Kind.CAPTURE,
                JContainer.build(Space.EMPTY, List.of(JRightPadded.build((J) ident("x"))), Markers.EMPTY),
                null);
        return new Py.MatchCase(
                randomId(), Space.EMPTY, Markers.EMPTY, pattern,
                JLeftPadded.build((Expression) ident("guard")).withBefore(Space.SINGLE_SPACE),
                type);
    }

    private static J.Identifier ident(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }
}
