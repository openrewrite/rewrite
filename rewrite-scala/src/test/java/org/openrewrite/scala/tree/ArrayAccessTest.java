/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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

class ArrayAccessTest implements RewriteTest {

    @Test
    void simpleArrayAccess() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  val first = arr(0)
                }
                """
            )
        );
    }

    @Test
    void nestedArrayAccess() {
        rewriteRun(
            scala(
                """
                object Test {
                  val matrix = Array(Array(1, 2), Array(3, 4))
                  val element = matrix(0)(1)
                }
                """
            )
        );
    }

    @Test
    void arrayAccessInExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(10, 20, 30)
                  val sum = arr(0) + arr(1)
                }
                """
            )
        );
    }

    @Test
    void listApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  val second = list(1)
                }
                """
            )
        );
    }

    @Test
    void mapApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val map = Map("a" -> 1, "b" -> 2)
                  val value = map("a")
                }
                """
            )
        );
    }

    @Test
    void arrayAccessWithVariable() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  val index = 2
                  val element = arr(index)
                }
                """
            )
        );
    }

    @Test
    void arrayAccessWithExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3, 4, 5)
                  val mid = arr(arr.length / 2)
                }
                """
            )
        );
    }

    @Test
    void stringApply() {
        rewriteRun(
            scala(
                """
                object Test {
                  val str = "hello"
                  val firstChar = str(0)
                }
                """
            )
        );
    }
}