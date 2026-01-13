/*
 * Copyright 2024 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class AssignTest implements RewriteTest {
    @Test
    void assignment() {
        rewriteRun(
          python(
            """
              j = 1
              k = 2
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "+",
      "-",
      "*",
      "/",
      "%",
      "&",
      "|",
      "^",
      "<<",
      ">>",
      "@",
      "**",
      "//",
    })
    void assignmentOp(String op) {
        rewriteRun(
          python(
            """
              a %s= 3
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "(a:=3)",
      "(a :=3)",
      "(a:= 3)",
      "a[b:=1]",
    })
    void assignmentExpression(String expr) {
        rewriteRun(
          python(
            """
              %s
              """.formatted(expr)
          )
        );
    }
}
