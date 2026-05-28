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

class TupleTypeTest implements RewriteTest {

    @Test
    void returnType() {
        rewriteRun(
          scala(
            """
            object O {
              def f(): (Int, Int) = (1, 2)
            }
            """
          )
        );
    }

    @Test
    void valAscription() {
        rewriteRun(
          scala(
            """
            object O {
              val pair: (Int, String) = (1, "a")
            }
            """
          )
        );
    }

    @Test
    void tripleReturnType() {
        rewriteRun(
          scala(
            """
            object O {
              def g(): (Int, String, Boolean) = (1, "a", true)
            }
            """
          )
        );
    }

    @Test
    void extraSpacesInsideParens() {
        rewriteRun(
          scala(
            """
            object O {
              def f() :  ( Int ,  String ) = (1, "a")
            }
            """
          )
        );
    }
}
