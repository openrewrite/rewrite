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

class ReturnTest implements RewriteTest {

    @Test
    void simpleReturn() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(): Int = {
                return 42
              }
            }
            """
          )
        );
    }

    @Test
    void returnWithExpression() {
        rewriteRun(
          scala(
            """
            object Test {
              def calculate(x: Int, y: Int): Int = {
                return x + y
              }
            }
            """
          )
        );
    }

    @Test
    void returnVoid() {
        rewriteRun(
          scala(
            """
            object Test {
              def doSomething(): Unit = {
                println("doing something")
                return
              }
            }
            """
          )
        );
    }

    @Test
    void earlyReturn() {
        rewriteRun(
          scala(
            """
            object Test {
              def checkValue(x: Int): String = {
                if (x < 0) {
                  return "negative"
                }
                if (x == 0) {
                  return "zero"
                }
                return "positive"
              }
            }
            """
          )
        );
    }

    @Test
    void returnWithMethodCall() {
        rewriteRun(
          scala(
            """
            object Test {
              def getName(): String = {
                return toString()
              }
            }
            """
          )
        );
    }

    @Test
    void returnWithNewExpression() {
        rewriteRun(
          scala(
            """
            object Test {
              def createPerson(): Person = {
                return new Person("John")
              }
            }
            """
          )
        );
    }

    @Test
    void returnInBlock() {
        rewriteRun(
          scala(
            """
            object Test {
              def getValue(): Int = {
                {
                  val x = 10
                  return x * 2
                }
              }
            }
            """
          )
        );
    }

    @Test
    void returnWithComplexExpression() {
        rewriteRun(
          scala(
            """
            object Test {
              def compute(list: List[Int]): Int = {
                return list.filter(_ > 0).map(_ * 2).sum
              }
            }
            """
          )
        );
    }
}