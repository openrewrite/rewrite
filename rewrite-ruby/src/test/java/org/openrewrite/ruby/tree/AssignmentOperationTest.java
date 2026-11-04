/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class AssignmentOperationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"+=", "-=", "*=", "/=", "%=", "**="})
    void assignmentOperatorsWithJavaEquivalents(String op) {
        rewriteRun(
          ruby(
            """
              a %s 1
              """.formatted(op)
          )
        );
    }

    @Test
    void assignToElement() {
        rewriteRun(
          ruby(
            """
              recv[index] += value
              """
          )
        );
    }

    @Test
    void assignToAttribute() {
        rewriteRun(
          ruby(
            """
              recv.x += value
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"&&=", "||="})
    void andOrAssign(String op) {
        rewriteRun(
          ruby(
            """
              recv %s value
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"&&=", "||="})
    void andOrAssignIndexed(String op) {
        rewriteRun(
          ruby(
            """
              recv[0] %s value
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"+=", "-=", "*=", "/="})
    void assignOpsWithJavaEquivalent(String op) {
        rewriteRun(
          ruby(
            """
              recv[index] %s value
              """.formatted(op)
          )
        );
    }
}
