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
class ThrowsTest implements RewriteTest {

    @Test
    void raise() {
        rewriteRun(
          python("raise")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "x", " x"
    })
    void raiseError(String expr) {
        rewriteRun(
          python(
            """
              raise %s
              """.formatted(expr)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "x from None",
      " x from None",
      "x  from None",
      "x from  None",
    })
    void raiseErrorFrom(String expr) {
        rewriteRun(
          python(
            """
              raise %s
              """.formatted(expr)
          )
        );
    }

}
