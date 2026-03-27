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

class TypeCastTest implements RewriteTest {

    @Test
    void simpleCast() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "hello"
                val str = obj.asInstanceOf[String]
              }
              """
          )
        );
    }

    @Test
    void castWithMethodCall() {
        rewriteRun(
          scala(
            """
              object Test {
                def getValue(): Any = 42
                val num = getValue().asInstanceOf[Int]
              }
              """
          )
        );
    }

    @Test
    void castInExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = 10
                val result = obj.asInstanceOf[Int] + 5
              }
              """
          )
        );
    }

    @Test
    void castToParameterizedType() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = List(1, 2, 3)
                val list = obj.asInstanceOf[List[Int]]
              }
              """
          )
        );
    }

    @Test
    void nestedCasts() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "42"
                val num = obj.asInstanceOf[String].toInt
              }
              """
          )
        );
    }

    @Test
    void castInIfCondition() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = true
                if (obj.asInstanceOf[Boolean]) {
                  println("It's true!")
                }
              }
              """
          )
        );
    }

    @Test
    void castWithParentheses() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = 42
                val result = (obj.asInstanceOf[Int]) * 2
              }
              """
          )
        );
    }

    @Test
    void castChain() {
        rewriteRun(
          scala(
            """
              object Test {
                val obj: Any = "test"
                val upper = obj.asInstanceOf[String].toUpperCase.asInstanceOf[CharSequence]
              }
              """
          )
        );
    }
}