/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class MethodInvocationTest implements RewriteTest {

    @Test
    void implicitFunctionCall() {
        rewriteRun(
          kotlin("""
              fun plugins(input: () -> String) {
                println( input() )
              }
          """),
          kotlin("""
              fun main() {
                plugins {
                    "test"
                }
              }
            """,
            isFullyParsed()
          )
        );
    }

    @Test
    void functionTypeReference() {
        rewriteRun(
          kotlin(
            """
              fun method(input: (  ) -> String) {
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Test
    void typedFunctionTypeReference() {
        rewriteRun(
          kotlin(
            """
              fun method(input : ( Int, Int ) -> Boolean) {
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Test
    void methodWithLambda() {
        rewriteRun(
          kotlin("""
              fun method(arg: Any) {
              }
            """,
            isFullyParsed()
          ),
          kotlin(
            """
              fun callMethodWithLambda() {
                  method {
                  }
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Test
    void methodInvocation() {
        rewriteRun(
          kotlin(
            """
              fun method(arg: Any) {
                val l = listOf(1, 2, 3)
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Test
    void multipleTypesOfMethodArguments() {
        rewriteRun(
          kotlin(
            """
              fun methodA(a: String, b: int, c: Double) {
              }
              fun methodB() {
                methodA("a", 1, 2.0)
              }
              """,
            isFullyParsed()
          )
        );
    }
}
