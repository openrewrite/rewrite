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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class SwitchTest implements RewriteTest {

    @Test
    void simple() {
        rewriteRun(
          python(
            """
              match x:
                case 1:
                    pass
                case 2:
                    pass
              """
          )
        );
    }

    @Test
    void wildcard() {
        rewriteRun(
          python(
            """
              match x:
                case 1:
                    pass
                case 2:
                    pass
                case _:
                    pass
              """
          )
        );
    }

    @Test
    void sequence() {
        rewriteRun(
          python(
            """
              match x:
                case [1, 2]:
                    pass
              """
          )
        );
    }

    @Test
    void star() {
        rewriteRun(
          python(
            """
              match x:
                case [1, 2, *rest]:
                    pass
              """
          )
        );
    }

    @Test
    void guard() {
        rewriteRun(
          python(
            """
              match x:
                case [1, 2, *rest] if 42 in rest:
                    pass
              """
          )
        );
    }

    @Test
    void or() {
        rewriteRun(
          python(
            """
              match x:
                case 2 | 3:
                    pass
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "",
      "a",
      "b, c",
      "a, b=c",
      "a, b=c, d=(e,f)",
    })
    void className(String args) {
        rewriteRun(
          python(
            """
              match x:
                case ClassName(%s):
                    pass
              """.formatted(args)
          )
        );
    }

    @Disabled("When parsing and printing the source code back to text without modifications, the printed source didn't match the original source code.")
    @Test
    void mapping() {
        rewriteRun(
          python(
            """
              match x:
                case {"x": x, "y": y, **z}:
                    pass
              """
          )
        );
    }

    @Test
    void value() {
        rewriteRun(
          python(
            """
              match x:
                case value.pattern:
                    pass
              """
          )
        );
    }

    @Test
    void nested() {
        rewriteRun(
          python(
            """
              match x:
                case [int(), str()]:
                    pass
              """
          )
        );
    }

    @Test
    void as() {
        rewriteRun(
          python(
            """
              match x:
                case [int(), str()] as y:
                    pass
              """
          )
        );
    }

    @Test
    void group() {
        rewriteRun(
          python(
            """
              match x:
                case (value.pattern):
                    pass
              """
          )
        );
    }

    @Test
    void sequenceTarget() {
        rewriteRun(
          python(
            """
              match x, y:
                case a, b:
                    pass
              """
          )
        );
    }
}
