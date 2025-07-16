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

class InstanceOfTest implements RewriteTest {

    @Test
    void simpleInstanceOf() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = "hello"
                  val isString = obj.isInstanceOf[String]
                }
                """
            )
        );
    }

    @Test
    void instanceOfWithGenerics() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = List(1, 2, 3)
                  val isList = obj.isInstanceOf[List[Int]]
                }
                """
            )
        );
    }

    @Test
    void instanceOfInCondition() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = 42
                  if (obj.isInstanceOf[Int]) {
                    println("It's an integer!")
                  }
                }
                """
            )
        );
    }

    @Test
    void instanceOfWithMethodCall() {
        rewriteRun(
            scala(
                """
                object Test {
                  def getValue(): Any = "test"
                  val isString = getValue().isInstanceOf[String]
                }
                """
            )
        );
    }

    @Test
    void negatedInstanceOf() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = 123
                  val notString = !obj.isInstanceOf[String]
                }
                """
            )
        );
    }

    @Test
    void instanceOfChain() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = "test"
                  val result = obj.isInstanceOf[String] && obj.asInstanceOf[String].nonEmpty
                }
                """
            )
        );
    }

    @Test
    void instanceOfWithParentheses() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = List(1, 2)
                  val check = (obj.isInstanceOf[List[_]])
                }
                """
            )
        );
    }

    @Test
    void multipleInstanceOfChecks() {
        rewriteRun(
            scala(
                """
                object Test {
                  val obj: Any = "hello"
                  val check = obj.isInstanceOf[String] || obj.isInstanceOf[Int]
                }
                """
            )
        );
    }
}