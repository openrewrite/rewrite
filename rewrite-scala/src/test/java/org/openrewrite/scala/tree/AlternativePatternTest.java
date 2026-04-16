/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class AlternativePatternTest implements RewriteTest {

    @Test
    void literalAlternatives() {
        rewriteRun(
          scala(
            """
              object Test {
                def describe(x: Int): String = x match {
                  case 1 | 2 | 3 => "small"
                  case 4 | 5 | 6 => "medium"
                  case _ => "other"
                }
              }
              """
          )
        );
    }

    @Test
    void stringAlternatives() {
        rewriteRun(
          scala(
            """
              object Test {
                def color(s: String): Int = s match {
                  case "red" | "blue" => 1
                  case _ => 0
                }
              }
              """
          )
        );
    }

    @Test
    void typedAlternatives() {
        rewriteRun(
          scala(
            """
              object Test {
                def size(x: Any): Int = x match {
                  case _: Int | _: Long => 4
                  case _ => 0
                }
              }
              """
          )
        );
    }
}
