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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class MethodDeclarationTest implements RewriteTest {

    @Test
    void extraSpaceBetweenModifierAndDef() {
        rewriteRun(
            scala(
                """
                object Test {
                  private   def hello(): Int = 1
                }
                """
            )
        );
    }

    @Test
    void extraSpaceBeforeReturnTypeColon() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo : Int = 5
                }
                """
            )
        );
    }

    @Test
    void extraSpaceBeforeReturnTypeColonWithParens() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo()  :  Int = 5
                }
                """
            )
        );
    }

    @Test
    void extraSpaceBeforeParameterDefaultEquals() {
        rewriteRun(
            scala(
                """
                object Test {
                  def hello(x: Int   = 1): Int = x
                }
                """
            )
        );
    }

    @Test
    void noSpaceAroundParameterDefaultEquals() {
        rewriteRun(
            scala(
                """
                object Test {
                  def hello(x: Int=1): Int = x
                }
                """
            )
        );
    }

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
    void methodParameterTypeNoSpaceAfterColon() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo(x:Int) = x
                }
                """
            )
        );
    }

    @Test
    void methodExtraSpaceBeforeEquals() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo()  = 1
                }
                """
            )
        );
    }

    @Test
    void methodNewlineBeforeEquals() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo()
                    = 1
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
    void methodWithVarargsParameter() {
        rewriteRun(
            scala(
                """
                object A {
                  def foo(x: Int, more: String*): Int = x
                }
                object B {
                  def foo(args: String*): Int = args.size
                }
                object C {
                  def foo(args: Array[String]*): Int = 0
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
    void multilineTypeParameters() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f[
                      A,
                      B
                  ](x: A): A = x
                }
                """
            )
        );
    }

    @Test
    void multilineCurriedParameterList() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(a: Int)(
                      b: Int,
                      c: Int
                  ): Int = a + b + c
                }
                """
            )
        );
    }

    @Test
    void curriedMethodExtraSpaceBeforeEquals() {
        rewriteRun(
            scala(
                """
                object Test {
                  def add(x: Int)(y: Int)  = x + y
                }
                """
            )
        );
    }

    @Test
    void qualifiedContextBound() {
        rewriteRun(
            scala(
                """
                object pkg {
                  trait Zero[A]
                }
                object Test {
                  def f[A: pkg.Zero](x: A): A = x
                }
                """
            )
        );
    }

    @Test
    void tripleCurriedParamList() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(a: Int)(b: Int)(c: Int): Int = a + b + c
                }
                """
            )
        );
    }

    @Test
    void curriedMethodParameterTypeNoSpaceAfterColon() {
        rewriteRun(
            scala(
                """
                object Test {
                  def add(x:Int)(y:Int) = x + y
                }
                """
            )
        );
    }

    @Test
    void multilineParameterListWithClosingParenOnOwnLine() {
        rewriteRun(
            scala(
                """
                object Test {
                  def add(
                      x: Int,
                      y: Int
                  ): Int = x + y
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

    @Test
    void symbolicNameWithTupleBody() {
        rewriteRun(
            scala(
                """
                class Foo {
                  def * = (1, 2, 3)
                }
                """
            )
        );
    }

    @Test
    void functionTypes() {
        rewriteRun(
            scala(
                """
                object Test {
                  def make1(): Int => Int = x => x + 1
                  def make2(): () => Int = () => 42
                  def make3(): (Int, String) => Boolean = (i, s) => s.length == i
                  def apply(f: Int => Int, x: Int): Int = f(x)
                }
                """
            )
        );
    }

    @Test
    void functionTypeAsDefaultParameter() {
        rewriteRun(
            scala(
                """
                case class C(f: Int => Boolean = _ => false)
                """
            )
        );
    }

    @Test
    void implicitInSecondParamList() {
        rewriteRun(
            scala(
                """
                object Test {
                  def apply(obj: String)(implicit c: Int): String = obj
                }
                """
            )
        );
    }

    @Nested
    class Using implements RewriteTest {

        @Test
        void inSecondParamList() {
            rewriteRun(
                scala(
                    """
                    object Test {
                      def apply(obj: String)(using c: Int): String = obj
                    }
                    """
                )
            );
        }

        @Test
        void inFirstParamList() {
            rewriteRun(
                scala(
                    """
                    object Test {
                      def apply(using c: Int): String = c.toString
                    }
                    """
                )
            );
        }

        @Test
        void inThirdParamListSingleLine() {
            rewriteRun(
                scala(
                    """
                    trait BSONHandler[T]
                    object Test {
                      def valueMapHandler[K, V](mapping: Map[K, V])(toKey: V => K)(using keyHandler: BSONHandler[K]): BSONHandler[V] = new BSONHandler[V] {}
                    }
                    """
                )
            );
        }

        @Test
        void inThirdParamListMultiline() {
            rewriteRun(
                scala(
                    """
                    trait BSONHandler[T]
                    object Test {
                      def valueMapHandler[K, V](mapping: Map[K, V])(toKey: V => K)(using
                          keyHandler: BSONHandler[K]
                      ): BSONHandler[V] = new BSONHandler[V] {}
                    }
                    """
                )
            );
        }

        @Test
        void anonymousParameter() {
            rewriteRun(
                scala(
                    """
                    trait Ord[T]
                    object Test {
                      def sort[T](xs: List[T])(using Ord[T]): List[T] = xs
                    }
                    """
                )
            );
        }
    }

    @Nested
    class ExtensionMethods implements RewriteTest {

        @Test
        void simpleExtensionWithSpaceBeforeParen() {
            rewriteRun(
                scala(
                    """
                    extension (x: Int) {
                      def foo = x + 1
                    }
                    """
                )
            );
        }

        @Test
        void simpleExtensionNoSpaceBeforeParen() {
            rewriteRun(
                scala(
                    """
                    extension(x: Int) {
                      def foo = x + 1
                    }
                    """
                )
            );
        }

        @Test
        void extensionWithMultipleSpaces() {
            rewriteRun(
                scala(
                    """
                    extension   (x: Int) {
                      def foo = x + 1
                    }
                    """
                )
            );
        }

        @Test
        void extensionParameterTypeNoSpaceAfterColon() {
            rewriteRun(
                scala(
                    """
                    extension (x:Int) {
                      def doubled = x * 2
                    }
                    """
                )
            );
        }

        @Test
        void extensionBracelessIndentedBody() {
            rewriteRun(
                scala(
                    """
                    extension (s: String)
                      def shout: String = s.toUpperCase
                      def whisper: String = s.toLowerCase
                    """
                )
            );
        }
    }

    @Test
    void parameterlessDef() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo: Int = 1
                }
                """
            )
        );
    }

    @Test
    void parameterlessDefInTrait() {
        rewriteRun(
            scala(
                """
                trait T {
                  def bar: String
                }
                """
            )
        );
    }

    @Test
    void spaceBeforeEqualsWithNoSpaceAfter() {
        rewriteRun(scala("def f(): Unit ={ }"));
    }

    @Test
    void spaceBeforeColonOnMethodParameter() {
        rewriteRun(scala("def f(map : Int): Int = 1"));
    }

    @Test
    void auxiliaryConstructors() {
        rewriteRun(
            scala(
                """
                class DelegatingToPrimary(a: Int) {
                  def this() = this(0)
                }

                class WithParams(a: Int, b: Int) {
                  def this(x: Int) = this(x, 0)
                }

                class Chained(a: Int, b: Int) {
                  def this(x: Int) = this(x, 0)
                  def this() = this(0)
                }

                class WithBlockBody(a: Int) {
                  def this() = {
                    this(0)
                    println("init")
                  }
                }

                class WithPrivate(a: Int) {
                  private def this() = this(0)
                }

                class WithAnnotation(a: Int) {
                  @deprecated def this() = this(0)
                }

                case class CaseClass(a: Int, b: Int) {
                  def this() = this(0, 0)
                }
                """
            )
        );
    }

    @Test
    void significantCharactersInComments() {
        // buildKeywordMethodInvocation — this(...) auxiliary constructor close paren in line comment
        rewriteRun(
          scala(
            """
              class C(val x: Int) {
                def this() = this(0 // )
                )
              }
              """
          )
        );
        // reparseProcedureBody — `{` in block comment before procedure body
        rewriteRun(
          scala(
            """
              class C {
                def foo() /* { */ {
                  println("x")
                }
              }
              """
          )
        );
        // visitDefDefImpl — type parameter close bracket in block comment
        rewriteRun(
          scala(
            """
              def f[T /* ] */](x: T): T = x
              """
          )
        );
        // visitDefDefImpl — parameter list close paren in line comment
        rewriteRun(
          scala(
            """
              def f(x: Int // )
              ): Int = x
              """
          )
        );
        // visitExtMethods — extension method close paren in block comment
        rewriteRun(
          scala(
            """
              extension (x: Int /* ) */ ) {
                def doubled: Int = x * 2
              }
              """
          )
        );
        // visitTypeParameter — context bound colon in block comment
        rewriteRun(
          scala(
            """
              def f[A /* : */ : Ordering](x: A): A = x
              """
          )
        );
    }
}
