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

class ForEachLoopTest implements RewriteTest {

    @Test
    void simpleForEach() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  for (x <- list) println(x)
                }
                """
            )
        );
    }

    @Test
    void forEachWithBlockBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  for (item <- list) {
                    println(item)
                  }
                }
                """
            )
        );
    }

    @Test
    void forEachWithMultipleStatements() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  for (x <- arr) {
                    println(x)
                    println(x * 2)
                  }
                }
                """
            )
        );
    }

    @Test
    void forEachWithRangeTo() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (i <- 1 to 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }

    @Test
    void forEachWithRangeUntil() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (i <- 0 until 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }

    @Test
    void forEachWithIfInBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3, 4, 5)
                  for (x <- arr) {
                    if (x > 2) println(x)
                    println(x)
                  }
                }
                """
            )
        );
    }

    @Test
    void forEachNestedInMethod() {
        rewriteRun(
            scala(
                """
                object Test {
                  def process(): Unit = {
                    val items = List("a", "b", "c")
                    for (item <- items) {
                      println(item)
                    }
                  }
                }
                """
            )
        );
    }
}
