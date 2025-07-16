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

class MethodDeclarationTest implements RewriteTest {

    @Test
    void simpleMethod() {
        rewriteRun(
            scala(
                """
                object Test {
                  def hello(): Unit = println("Hello")
                }
                """
            )
        );
    }

    @Test
    void methodWithParameters() {
        rewriteRun(
            scala(
                """
                object Test {
                  def greet(name: String): Unit = println(s"Hello, $name")
                }
                """
            )
        );
    }

    @Test
    void methodWithMultipleParameters() {
        rewriteRun(
            scala(
                """
                object Test {
                  def add(x: Int, y: Int): Int = x + y
                }
                """
            )
        );
    }

    @Test
    void methodWithDefaultParameter() {
        rewriteRun(
            scala(
                """
                object Test {
                  def greet(name: String = "World"): Unit = println(s"Hello, $name")
                }
                """
            )
        );
    }

    @Test
    void methodWithTypeParameters() {
        rewriteRun(
            scala(
                """
                object Test {
                  def identity[T](x: T): T = x
                }
                """
            )
        );
    }

    @Test
    void abstractMethod() {
        rewriteRun(
            scala(
                """
                abstract class Shape {
                  def area(): Double
                }
                """
            )
        );
    }

    @Test
    void privateMethod() {
        rewriteRun(
            scala(
                """
                object Test {
                  private def helper(): Int = 42
                }
                """
            )
        );
    }

    @Test
    void overrideMethod() {
        rewriteRun(
            scala(
                """
                class Child extends Parent {
                  override def toString: String = "Child"
                }
                """
            )
        );
    }
}