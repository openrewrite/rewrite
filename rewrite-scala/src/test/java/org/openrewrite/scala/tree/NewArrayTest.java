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

class NewArrayTest implements RewriteTest {

    @Test
    void simpleArrayCreation() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                }
                """
            )
        );
    }

    @Test
    void emptyArray() {
        rewriteRun(
            scala(
                """
                object Test {
                  val empty = Array[Int]()
                }
                """
            )
        );
    }

    @Test
    void arrayWithTypeParameter() {
        rewriteRun(
            scala(
                """
                object Test {
                  val strings = Array[String]("hello", "world")
                }
                """
            )
        );
    }

    @Test
    void arrayOfArrays() {
        rewriteRun(
            scala(
                """
                object Test {
                  val matrix = Array(Array(1, 2), Array(3, 4))
                }
                """
            )
        );
    }

    @Test
    void arrayWithMixedTypes() {
        rewriteRun(
            scala(
                """
                object Test {
                  val mixed = Array[Any](1, "hello", true)
                }
                """
            )
        );
    }

    @Test
    void arrayWithNewKeyword() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = new Array[Int](5)
                }
                """
            )
        );
    }

    @Test
    void arrayOfObjects() {
        rewriteRun(
            scala(
                """
                object Test {
                  case class Person(name: String)
                  val people = Array(Person("Alice"), Person("Bob"))
                }
                """
            )
        );
    }

    @Test
    void arrayWithExpressions() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = 10
                  val arr = Array(x, x * 2, x + 5)
                }
                """
            )
        );
    }

    @Test
    void arrayFill() {
        rewriteRun(
            scala(
                """
                object Test {
                  val filled = Array.fill(5)(0)
                }
                """
            )
        );
    }

    @Test
    void arrayRange() {
        rewriteRun(
            scala(
                """
                object Test {
                  val range = Array.range(1, 10)
                }
                """
            )
        );
    }
}