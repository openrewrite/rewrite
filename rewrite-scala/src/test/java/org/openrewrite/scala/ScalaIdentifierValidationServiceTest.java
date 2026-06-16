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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.internal.ScalaIdentifierValidationService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class ScalaIdentifierValidationServiceTest {

    private final ScalaIdentifierValidationService service = new ScalaIdentifierValidationService();

    private J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), name, null, null);
    }

    private boolean isFlagged(String name) {
        // given an identifier with the given name
        J.Identifier id = identifier(name);
        // when the validation visitor runs
        J.Identifier visited = (J.Identifier) service.getVisitor().visit(id, new InMemoryExecutionContext());
        // then it is flagged iff the visitor mutated the name
        return visited != null && !name.equals(visited.getSimpleName());
    }

    @Test
    void flagsSourceTextStuffedIntoIdentifierNames() {
        assertThat(isFlagged("Int | String")).isTrue();
        assertThat(isFlagged("Serializable & Comparable[String]")).isTrue();
        assertThat(isFlagged("Array[Int]()")).isTrue();
        assertThat(isFlagged("Exception(msg)")).isTrue();
        assertThat(isFlagged("F[_]")).isTrue();
        assertThat(isFlagged("e @ (_: A | _: B)")).isTrue();
    }

    @Test
    void allowsLegitimateScalaIdentifiers() {
        assertThat(isFlagged("x")).isFalse();
        assertThat(isFlagged("myVariable")).isFalse();
        assertThat(isFlagged("_value")).isFalse();
        assertThat(isFlagged("$value")).isFalse();
        // operator identifiers are legal Scala identifiers
        assertThat(isFlagged("::")).isFalse();
        assertThat(isFlagged("<=")).isFalse();
        assertThat(isFlagged("+")).isFalse();
        assertThat(isFlagged("->")).isFalse();
    }

    @Test
    void allowsBacktickQuotedIdentifiers() {
        // backtick-quoted identifiers may contain any character
        assertThat(isFlagged("`my variable`")).isFalse();
        assertThat(isFlagged("`text/html(UTF-8)`")).isFalse();
        assertThat(isFlagged("`type`")).isFalse();
    }
}
