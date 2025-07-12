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

@SuppressWarnings("ZeroIndexToHead")
class MethodInvocationTest implements RewriteTest {

    @Test
    void simpleMethodCall() {
        rewriteRun(
          scala(
            """
              object Test {
                println("Hello")
              }
              """
          )
        );
    }

    @Test
    void methodCallNoArgs() {
        rewriteRun(
          scala(
            """
              object Test {
                val s = "hello"
                val len = s.length()
              }
              """
          )
        );
    }

    @Test
    void methodCallMultipleArgs() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = Math.max(10, 20)
              }
              """
          )
        );
    }

    @Test
    void chainedMethodCalls() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = "hello".toUpperCase().substring(1)
              }
              """
          )
        );
    }

    @Test
    void methodCallOnFieldAccess() {
        rewriteRun(
          scala(
            """
              object Test {
                System.out.println("test")
              }
              """
          )
        );
    }

    @Test
    void methodCallWithNamedArguments() {
        rewriteRun(
          scala(
            """
              object Test {
                def greet(name: String, age: Int) = s"$name is $age"
                val msg = greet(name = "Alice", age = 30)
              }
              """
          )
        );
    }

    @Test
    void infixMethodCall() {
        rewriteRun(
          scala(
            """
              object Test {
                val list = List(1, 2, 3)
                val result = list map (_ * 2)
              }
              """
          )
        );
    }

    @Test
    void applyMethod() {
        rewriteRun(
          scala(
            """
              object Test {
                val list = List(1, 2, 3)
                val first = list(0)
              }
              """
          )
        );
    }

    @Test
    void methodCallInExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = Math.sqrt(16) + Math.pow(2, 3)
              }
              """
          )
        );
    }

    @Test
    void nestedMethodCalls() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = Math.max(Math.min(10, 20), 5)
              }
              """
          )
        );
    }

    @Test
    void methodCallWithBlock() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = List(1, 2, 3).map { x =>
                  x * 2
                }
              }
              """
          )
        );
    }

    @Test
    void curriedMethodCall() {
        rewriteRun(
          scala(
            """
              object Test {
                def add(x: Int)(y: Int) = x + y
                val result = add(5)(10)
              }
              """
          )
        );
    }
}