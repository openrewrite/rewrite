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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class JSTest {

    @Test
    void statementWrappersWithDifferentIdsAreNotEqual() {
        // given
        J.Empty empty = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        List<J> nodes = List.of(
          new JS.ExpressionStatement(randomId(), Space.EMPTY, Markers.EMPTY, empty),
          new JS.ExpressionStatement(randomId(), Space.EMPTY, Markers.EMPTY, empty),
          new JS.StatementExpression(randomId(), Space.EMPTY, Markers.EMPTY, empty),
          new JS.StatementExpression(randomId(), Space.EMPTY, Markers.EMPTY, empty)
        );

        // when
        Set<J> distinct = new HashSet<>(nodes);

        // then
        assertThat(distinct).hasSize(4);
    }

    @Test
    void binaryHasSideEffectsOfItsOperands() {
        // given
        J.Assignment call = assignment();
        J.Assignment other = assignment();

        // when
        JS.Binary power = binary(call, JS.Binary.Type.Exponentiation, other);
        JS.Binary pure = binary(identifier("a"), JS.Binary.Type.IdentityEquals, identifier("b"));

        // then
        assertThat(power.getSideEffects()).containsExactly(call, other);
        assertThat(pure.getSideEffects()).isEmpty();
    }

    @Test
    void nullishCoalescingIsKeptWholeOnlyWhenTheRightHasSideEffects() {
        // given
        J.Assignment left = assignment();

        // when
        JS.Binary sideEffectOnRight = binary(identifier("a"), JS.Binary.Type.QuestionQuestion, assignment());
        JS.Binary pureRight = binary(left, JS.Binary.Type.QuestionQuestion, identifier("b"));

        // then
        assertThat(sideEffectOnRight.getSideEffects()).containsExactly(sideEffectOnRight);
        assertThat(pureRight.getSideEffects()).containsExactly(left);
    }

    private static JS.Binary binary(Expression left, JS.Binary.Type operator, Expression right) {
        return new JS.Binary(randomId(), Space.EMPTY, Markers.EMPTY, left, JLeftPadded.build(operator), right, null);
    }

    private static J.Assignment assignment() {
        return new J.Assignment(randomId(), Space.EMPTY, Markers.EMPTY, identifier("x"),
          JLeftPadded.build(identifier("y")), null);
    }

    private static J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }
}
