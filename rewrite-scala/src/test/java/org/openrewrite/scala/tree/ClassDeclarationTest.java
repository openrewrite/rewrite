/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import static org.openrewrite.test.RewriteTest.toRecipe;

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

    @Test
    void multilineClassTypeParameters() {
        rewriteRun(
            scala(
                """
                class Foo[
                    A,
                    B
                ]
                """
            )
        );
    }

    @Test
    void multilineWithClauses() {
        rewriteRun(
            scala(
                """
                trait B
                trait C
                trait D
                class A extends B
                    with C
                    with D
                """
            )
        );
    }

    @Test
    void methodWithImplicitParameter() {
        rewriteRun(
            scala(
                """
                class Foo {
                  def setup(implicit system: ActorSystem): Unit = {}
                }
                """
            )
        );
    }

    @Test
    void annotatedFinalClass() {
        rewriteRun(
            scala(
                """
                @Module
                final class Env
                """
            )
        );
    }

    @Test
    void traitWithConstructorParameters() {
        rewriteRun(
            scala(
                """
                trait Named(val name: String)
                """
            )
        );
    }

    @Test
    void classWithParameterDefaultValue() {
        rewriteRun(
            scala(
                """
                class Greeter(name: String = "world")
                """
            )
        );
    }

    @Test
    void classConstructorParametersExposedAsVariableDeclarations() {
        // Verifies the primary constructor exposes parameters as semantic J.VariableDeclarations
        // (with names, types, and modifiers) rather than as opaque source text via J.Unknown.
        rewriteRun(
            spec -> spec.recipe(toRecipe(() -> new org.openrewrite.scala.ScalaIsoVisitor<>() {
                @Override
                public org.openrewrite.java.tree.J.ClassDeclaration visitClassDeclaration(
                        org.openrewrite.java.tree.J.ClassDeclaration classDecl,
                        org.openrewrite.ExecutionContext ctx) {
                    org.openrewrite.java.tree.JContainer<org.openrewrite.java.tree.Statement> ctor =
                            classDecl.getPadding().getPrimaryConstructor();
                    if (ctor != null) {
                        for (org.openrewrite.java.tree.Statement param : ctor.getElements()) {
                            if (!(param instanceof org.openrewrite.java.tree.J.VariableDeclarations)) {
                                throw new AssertionError("Expected J.VariableDeclarations for constructor param, got: " + param.getClass());
                            }
                        }
                    }
                    return super.visitClassDeclaration(classDecl, ctx);
                }
            })),
            scala(
                """
                class Person(val name: String, age: Int)
                """
            )
        );
    }
}
