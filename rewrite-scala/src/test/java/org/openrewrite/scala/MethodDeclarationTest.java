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
    }
}
