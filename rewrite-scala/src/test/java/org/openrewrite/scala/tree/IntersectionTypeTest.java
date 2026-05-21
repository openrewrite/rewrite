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

class IntersectionTypeTest implements RewriteTest {

    @Test
    void valWithIntersectionType() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              object Test {
                val x: A with B = null
              }
              """
          )
        );
    }

    @Test
    void valWithTripleIntersection() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              trait C
              object Test {
                val x: A with B with C = null
              }
              """
          )
        );
    }

    @Test
    void defReturnIntersection() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              object Test {
                def foo(): A with B = null
              }
              """
          )
        );
    }

    @Test
    void defParamIntersection() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              object Test {
                def foo(x: A with B): Unit = ()
              }
              """
          )
        );
    }

    @Test
    void typeAliasIntersection() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              object Test {
                type AB = A with B
              }
              """
          )
        );
    }

    @Test
    void extraSpacing() {
        rewriteRun(
          scala(
            """
              trait A
              trait B
              object Test {
                val x:  A   with   B = null
              }
              """
          )
        );
    }
}
