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
package org.openrewrite.csharp.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class CSharpWhitespaceValidationServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {" \t\f\u000B\r\n", " ", " ", " ", "　", "\u0085", " ", " "})
    void acceptsCSharpWhitespace(String whitespace) {
        // given
        J.Identifier ident = identifier(whitespace);

        // when
        Tree validated = new CSharpWhitespaceValidationService().getVisitor().visit(ident, new InMemoryExecutionContext());

        // then
        assertThat(validated).isSameAs(ident);
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", " ;", "​", "﻿"})
    void rejectsNonWhitespace(String whitespace) {
        // given
        J.Identifier ident = identifier(whitespace);

        // when
        Tree validated = new CSharpWhitespaceValidationService().getVisitor().visit(ident, new InMemoryExecutionContext());

        // then
        assertThat(validated).isNotSameAs(ident);
    }

    private static J.Identifier identifier(String whitespace) {
        return new J.Identifier(randomId(), Space.build(whitespace, emptyList()), Markers.EMPTY, emptyList(), "x", null, null);
    }
}
