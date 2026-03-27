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

class ClassDeclarationTest implements RewriteTest {

    @Test
    void emptyClass() {
        rewriteRun(
            scala(
                """
                class Empty
                """
            )
        );
    }

    @Test
    void classWithEmptyBody() {
        rewriteRun(
            scala(
                """
                class Empty {
                }
                """
            )
        );
    }

    @Test
    void classWithSingleParameter() {
        rewriteRun(
            scala(
                """
                class Person(name: String)
                """
            )
        );
    }

    @Test
    void classWithMultipleParameters() {
        rewriteRun(
            scala(
                """
                class Person(firstName: String, lastName: String, age: Int)
                """
            )
        );
    }

    @Test
    void classWithValParameters() {
        rewriteRun(
            scala(
                """
                class Person(val name: String, val age: Int)
                """
            )
        );
    }

    @Test
    void classWithVarParameters() {
        rewriteRun(
            scala(
                """
                class Counter(var count: Int)
                """
            )
        );
    }

    @Test
    void classWithMixedParameters() {
        rewriteRun(
            scala(
                """
                class Person(val name: String, var age: Int, nickname: String)
                """
            )
        );
    }

    @Test
    void classWithMethod() {
        rewriteRun(
            scala(
                """
                class Greeter {
                  def greet(): Unit = println("Hello!")
                }
                """
            )
        );
    }

    @Test
    void classWithField() {
        rewriteRun(
            scala(
                """
                class Counter {
                  var count = 0
                }
                """
            )
        );
    }

    @Test
    void classWithConstructorAndBody() {
        rewriteRun(
            scala(
                """
                class Person(val name: String) {
                  def greet(): String = s"Hello, I'm $name"
                  val upperName = name.toUpperCase
                }
                """
            )
        );
    }

    @Test
    void nestedClass() {
        rewriteRun(
            scala(
                """
                class Outer {
                  class Inner {
                    def foo(): Int = 42
                  }
                }
                """
            )
        );
    }

    @Test
    void classWithPrivateModifier() {
        rewriteRun(
            scala(
                """
                private class Secret
                """
            )
        );
    }

    @Test
    void classWithProtectedModifier() {
        rewriteRun(
            scala(
                """
                protected class Internal
                """
            )
        );
    }

    @Test
    void classWithAccessModifiers() {
        rewriteRun(
            scala(
                """
                class Person(private val id: Int, protected var name: String, age: Int)
                """
            )
        );
    }

    @Test
    void abstractClass() {
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
    void classExtendingAnother() {
        rewriteRun(
            scala(
                """
                class Dog extends Animal
                """
            )
        );
    }

    @Test
    void classWithTypeParameter() {
        rewriteRun(
            scala(
                """
                class Box[T](value: T)
                """
            )
        );
    }

    @Test
    void caseClass() {
        rewriteRun(
            scala(
                """
                case class Point(x: Int, y: Int)
                """
            )
        );
    }
}