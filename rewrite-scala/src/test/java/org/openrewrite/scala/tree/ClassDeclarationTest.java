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
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void classConstructorParameterTypeNoSpaceAfterColon() {
        rewriteRun(
            scala(
                """
                class Foo(x:Int)
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
    void abstractClassWithEmptyParensAndAbstractDef() {
        rewriteRun(
            scala(
                """
                abstract class Foo() {
                  def x: Boolean
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

    @Test
    void paramNameEndingInUnderscore() {
        rewriteRun(
            scala(
                """
                class Foo(tag_ : String)
                """
            )
        );
    }

    @Test
    void typeParamContextBoundWithSpaceBeforeColon() {
        rewriteRun(
            scala(
                """
                case class Foo[T : Encoder]()
                """
            )
        );
    }

    @Test
    void significantCharactersInComments() {
        // buildKeywordMethodInvocation — super(...) close paren in line comment
        rewriteRun(
            scala(
                """
                class Base(val x: Int)
                class Derived(x: Int) extends Base(x // )
                )
                """
            )
        );
        // visitClassDef — type parameter close bracket in block comment
        rewriteRun(
            scala(
                """
                class C[A /* ] */, B]
                """
            )
        );
        // visitClassDef — type parameter comma in block comment
        rewriteRun(
            scala(
                """
                class C[A /* , */ , B]
                """
            )
        );
        // visitClassDef — constructor close paren in line comment
        rewriteRun(
            scala(
                """
                class C(x: Int // )
                )
                """
            )
        );
        // visitTypeParameter — `[` in block comment between class name and type params
        rewriteRun(
            scala(
                """
                class C /* [ */ [A]
                """
            )
        );
        // visitTypeParameter — `>:` lower bound operator in block comment
        rewriteRun(
            scala(
                """
                class C[A /* >: */ >: Nothing]
                """
            )
        );
        // visitTypeParameter — `<:` upper bound operator in block comment
        rewriteRun(
            scala(
                """
                class C[A /* <: */ <: AnyRef]
                """
            )
        );
    }

    @Test
    void extendsWithBraceLikeContentInParentArgs() {
        // Each class has no body, but the parent's constructor args contain a `{` that
        // must not be mistaken for the body opener:
        //   - block-form string interpolation `s"${x}"`
        //   - literal `{` inside a plain string
        //   - `{` inside a block comment
        //   - `{` introducing a block expression argument
        //   - interpolation followed by a `with` trait
        //   - multi-line extends used by `case class`
        rewriteRun(
            scala(
                """
                trait T
                class A(msg: String) extends Exception(msg)
                class B1(x: Int) extends Exception(s"${x}")
                class B2(x: Int) extends Exception("{ not a body")
                class B3(x: Int) extends Exception(/* { */ "x")
                class B4(x: Int) extends Exception({ val m = "msg"; m })
                class B5(x: Int) extends Exception(s"${x}") with T
                case class B6(json: String, err: String)
                    extends A(s"[x] $json --- ${err.length}")
                """
            )
        );
    }

    @Test
    void parentConstructorCallIsModeledNotCrammedIntoIdentifier() {
        // A parent constructor call must become an S.ConstructorInvocation (supertype + args),
        // not a J.Identifier whose simpleName is the raw source "Exception(msg)".
        rewriteRun(
            spec -> spec.beforeRecipe(sources -> {
                List<S.ConstructorInvocation> invocations = new ArrayList<>();
                List<String> identifierNames = new ArrayList<>();
                ScalaIsoVisitor<Integer> visitor = new ScalaIsoVisitor<Integer>() {
                    @Override
                    public S.ConstructorInvocation visitConstructorInvocation(S.ConstructorInvocation ci, Integer i) {
                        invocations.add(ci);
                        return super.visitConstructorInvocation(ci, i);
                    }

                    @Override
                    public J.Identifier visitIdentifier(J.Identifier ident, Integer i) {
                        identifierNames.add(ident.getSimpleName());
                        return super.visitIdentifier(ident, i);
                    }
                };
                sources.forEach(source -> visitor.visit(source, 0));

                assertThat(identifierNames).noneMatch(name -> name.contains("("));
                assertThat(invocations).hasSize(1);
                S.ConstructorInvocation ci = invocations.get(0);
                assertThat(ci.getTypeTree()).isInstanceOf(J.Identifier.class);
                assertThat(((J.Identifier) ci.getTypeTree()).getSimpleName()).isEqualTo("Exception");
                assertThat(ci.getArguments()).hasSize(1);
            }),
            scala(
                """
                class E(msg: String) extends Exception(msg)
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaInParameters() {
        rewriteRun(
            scala(
                """
                class Foo(
                    val x: String,
                    val y: Int,
                )
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaSingleParameter() {
        rewriteRun(
            scala(
                """
                class Foo(
                    x: Int,
                )
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaSingleLine() {
        rewriteRun(
            scala(
                """
                class Foo(x: Int,)
                """
            )
        );
    }

    @Test
    void classWithoutTrailingCommaDoesNotFail() {
        rewriteRun(
            scala(
                """
                class Foo(x: Int)
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaAndWhitespaceBeforeComma() {
        rewriteRun(
            scala(
                """
                class Foo(x: Int ,)
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaInTypeParameters() {
        rewriteRun(
            scala(
                """
                class Foo[
                    A,
                    B,
                ](x: A)
                """
            )
        );
    }

    @Test
    void classWithTrailingCommaInTypeParametersSingleLine() {
        rewriteRun(
            scala(
                """
                class Foo[A,](x: A)
                """
            )
        );
    }

    @Test
    void colonBodyWithTrailingCommentAfterColon() {
        rewriteRun(
            scala(
                """
                case class Matchup(users: Users): // score is x10
                  def nonEmpty = true
                """
            )
        );
    }
}
