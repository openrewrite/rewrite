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

class ComplexForDoTest implements RewriteTest {

    @Test
    void multiGeneratorForLoop() {
        rewriteRun(
          scala(
            """
              object Test {
                val xs = List(1, 2, 3)
                val ys = List(4, 5, 6)
                def run(): Unit = {
                  for (x <- xs; y <- ys) println(x + y)
                }
              }
              """
          )
        );
    }

    @Test
    void forLoopWithGuard() {
        rewriteRun(
          scala(
            """
              object Test {
                val xs = List(1, 2, 3)
                def run(): Unit = {
                  for (x <- xs if x > 1) println(x)
                }
              }
              """
          )
        );
    }

    @Test
    void forLoopWithDestructuringPattern() {
        rewriteRun(
          scala(
            """
              object Test {
                val pairs = List((1, 2), (3, 4))
                def run(): Unit = {
                  for ((a, b) <- pairs) println(a + b)
                }
              }
              """
          )
        );
    }
}
