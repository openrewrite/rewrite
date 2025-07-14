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

class ThrowTest implements RewriteTest {

    @Test
    void simpleThrow() {
        rewriteRun(
          scala(
            """
            object Test {
              def fail(): Nothing = {
                throw new Exception("Error occurred")
              }
            }
            """
          )
        );
    }

    @Test
    void throwWithCustomException() {
        rewriteRun(
          scala(
            """
            object Test {
              def validate(x: Int): Unit = {
                if (x < 0) {
                  throw new IllegalArgumentException("x must be non-negative")
                }
              }
            }
            """
          )
        );
    }

    @Test
    void throwWithoutNew() {
        rewriteRun(
          scala(
            """
            object Test {
              def rethrow(e: Exception): Nothing = {
                throw e
              }
            }
            """
          )
        );
    }

    @Test
    void throwWithMethodCall() {
        rewriteRun(
          scala(
            """
            object Test {
              def createError(): Exception = new Exception("error")
              
              def fail(): Nothing = {
                throw createError()
              }
            }
            """
          )
        );
    }

    @Test
    void throwInTryCatch() {
        rewriteRun(
          scala(
            """
            object Test {
              def process(): Unit = {
                try {
                  throw new RuntimeException("Processing failed")
                } catch {
                  case e: Exception => println(e.getMessage)
                }
              }
            }
            """
          )
        );
    }

    @Test
    void throwWithComplexExpression() {
        rewriteRun(
          scala(
            """
            object Test {
              def fail(msg: String, code: Int): Nothing = {
                throw new Exception(s"Error: $msg (code: $code)")
              }
            }
            """
          )
        );
    }

    @Test
    void throwInMatchCase() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String => s
                case _ => throw new UnsupportedOperationException("Not a string")
              }
            }
            """
          )
        );
    }

    @Test
    void throwAsExpression() {
        rewriteRun(
          scala(
            """
            object Test {
              def getValue(opt: Option[Int]): Int = {
                opt.getOrElse(throw new NoSuchElementException("No value"))
              }
            }
            """
          )
        );
    }
}