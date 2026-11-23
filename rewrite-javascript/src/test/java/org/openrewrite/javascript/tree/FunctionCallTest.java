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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class FunctionCallTest {

    @Test
    void withTypeParametersReturnsSameInstanceForUnchangedList() {
        // given
        JS.FunctionCall fc = functionCall(identifier("T"));

        // when
        JS.FunctionCall result = fc.withTypeParameters(fc.getTypeParameters());

        // then
        assertThat(result).isSameAs(fc);
    }

    @Test
    void withTypeParametersReturnsNewInstanceForDifferentList() {
        // given
        JS.FunctionCall fc = functionCall(identifier("T"));
        J.Identifier u = identifier("U");

        // when
        JS.FunctionCall result = fc.withTypeParameters(singletonList(u));

        // then
        assertThat(result).isNotSameAs(fc);
        assertThat(result.getTypeParameters()).containsExactly(u);
        assertThat(fc.getTypeParameters()).extracting(e -> ((J.Identifier) e).getSimpleName()).containsExactly("T");
    }

    private static JS.FunctionCall functionCall(Expression typeParameter) {
        return new JS.FunctionCall(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(identifier("f")),
                JContainer.build(singletonList(JRightPadded.build(typeParameter))),
                JContainer.build(emptyList()),
                null
        );
    }

    private static J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }
}
