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

class TryTest implements RewriteTest {

    @Test
    void simpleTryCatch() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky operation")
                } catch {
                  case e: Exception => println("caught exception")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithFinally() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky operation")
                } finally {
                  println("cleanup")
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val result = 10 / 0
                } catch {
                  case e: ArithmeticException => println("division by zero")
                  case e: Exception => println("other exception")
                } finally {
                  println("cleanup")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithMultipleCatches() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val text = "not a number"
                  val num = text.toInt
                } catch {
                  case e: NumberFormatException => println("not a valid number")
                  case e: NullPointerException => println("null pointer")
                  case e: Exception => println("unexpected error")
                }
              }
              """
          )
        );
    }

    @Test
    void tryExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = try {
                  "42".toInt
                } catch {
                  case e: NumberFormatException => 0
                }
              }
              """
          )
        );
    }

    @Test
    void nestedTry() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  try {
                    println("inner try")
                  } catch {
                    case e: Exception => println("inner catch")
                  }
                } catch {
                  case e: Exception => println("outer catch")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithWildcardCatch() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  throw new RuntimeException("error")
                } catch {
                  case _ => println("caught something")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithTypedPattern() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val result = riskyOperation()
                } catch {
                  case _: IllegalArgumentException | _: IllegalStateException => 
                    println("illegal argument or state")
                  case e: Throwable => 
                    println(s"unexpected error: ${e.getMessage}")
                }
              }
              """
          )
        );
    }
}