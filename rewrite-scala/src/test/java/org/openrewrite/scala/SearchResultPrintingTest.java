/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.function.Predicate;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SearchResultPrintingTest implements RewriteTest {

    @DocumentExample
    @Test
    void catchClause() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Try.Catch.class)),
          scala(
            """
              object T { try { println(1) } catch { case _: Exception => } }
              """,
            """
              object T { try { println(1) } /*~~(Try.Catch)~~>*/catch { case _: Exception => } }
              """
          )
        );
    }

    @Test
    void everyCatchCase() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Try.Catch.class)),
          scala(
            """
              object T {
                try println(1)
                catch {
                  case e: IllegalStateException => println(e)
                  case _: Exception => println(2)
                }
              }
              """,
            """
              object T {
                try println(1)
                /*~~(Try.Catch)~~>*/catch {
                  case e: IllegalStateException => println(e)
                  /*~~(Try.Catch)~~>*/case _: Exception => println(2)
                }
              }
              """
          )
        );
    }

    @Test
    void catchCaseParentheses() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ControlParentheses.class)),
          scala(
            """
              object T {
                try println(1)
                catch {
                  case e: IllegalStateException => println(e)
                  case _: Exception => println(2)
                }
              }
              """,
            """
              object T {
                try println(1)
                catch /*~~(ControlParentheses)~~>*/{
                  case e: IllegalStateException => println(e)
                  /*~~(ControlParentheses)~~>*/case _: Exception => println(2)
                }
              }
              """
          )
        );
    }

    @Test
    void catchCasePatterns() {
        rewriteRun(
          spec -> spec.recipe(mark(J.VariableDeclarations.class)),
          scala(
            """
              object T {
                try println(1)
                catch {
                  case e: IllegalStateException => println(e)
                  case _: Exception => println(2)
                }
              }
              """,
            """
              object T {
                try println(1)
                catch {
                  /*~~(VariableDeclarations)~~>*/case e: IllegalStateException => println(e)
                  /*~~(VariableDeclarations)~~>*/case _: Exception => println(2)
                }
              }
              """
          )
        );
    }

    @Test
    void catchCaseNames() {
        rewriteRun(
          spec -> spec.recipe(mark(J.VariableDeclarations.NamedVariable.class)),
          scala(
            """
              object T {
                try println(1)
                catch {
                  case e: IllegalStateException => println(e)
                  case _: Exception => println(2)
                }
              }
              """,
            """
              object T {
                try println(1)
                catch {
                  case /*~~(VariableDeclarations.NamedVariable)~~>*/e: IllegalStateException => println(e)
                  case /*~~(VariableDeclarations.NamedVariable)~~>*/_: Exception => println(2)
                }
              }
              """
          )
        );
    }

    @Test
    void implicitParameter() {
        rewriteRun(
          spec -> spec.recipe(mark(J.VariableDeclarations.class)),
          scala(
            """
              object T { def f(x: Int)(implicit ec: scala.concurrent.ExecutionContext): Int = 1 }
              """,
            """
              object T { def f(/*~~(VariableDeclarations)~~>*/x: Int)(/*~~(VariableDeclarations)~~>*/implicit ec: scala.concurrent.ExecutionContext): Int = 1 }
              """
          )
        );
    }

    @Test
    void constructorVar() {
        rewriteRun(
          spec -> spec.recipe(mark(J.VariableDeclarations.class,
            v -> ((J.VariableDeclarations) v).getModifiers().stream().anyMatch(m -> "var".equals(m.getKeyword())))),
          scala(
            """
              class A(var ctorVar: Int, val ctorVal: Int, plain: Int)
              """,
            """
              class A(/*~~(VariableDeclarations)~~>*/var ctorVar: Int, val ctorVal: Int, plain: Int)
              """
          )
        );
    }

    @Test
    void parameterNames() {
        rewriteRun(
          spec -> spec.recipe(mark(J.VariableDeclarations.NamedVariable.class)),
          scala(
            """
              class A(a: Int) {
                def f(b: Int)(c: Int = 1)(implicit g: Ordering[Int]): Int = a
                for (d <- 1 to 3) println(d)
              }
              extension (e: String) def twice: String = e + e
              """,
            """
              class A(/*~~(VariableDeclarations.NamedVariable)~~>*/a: Int) {
                def f(/*~~(VariableDeclarations.NamedVariable)~~>*/b: Int)(/*~~(VariableDeclarations.NamedVariable)~~>*/c: Int = 1)(implicit /*~~(VariableDeclarations.NamedVariable)~~>*/g: Ordering[Int]): Int = a
                for (/*~~(VariableDeclarations.NamedVariable)~~>*/d <- 1 to 3) println(d)
              }
              extension (/*~~(VariableDeclarations.NamedVariable)~~>*/e: String) def twice: String = e + e
              """
          )
        );
    }

    @Test
    void valAndDefKeywords() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Modifier.class)),
          scala(
            """
              class A {
                val x = 1
                def f: Int = x
              }
              """,
            """
              class A {
                /*~~(Modifier)~~>*/val x = 1
                /*~~(Modifier)~~>*/def f: Int = x
              }
              """
          )
        );
    }

    @Test
    void curriedMethod() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Lambda.class, J.Lambda.Parameters.class)),
          scala(
            """
              object T { def f(a: Int)(b: Int): Int = a + b }
              """,
            """
              object T { def f(a: Int)/*~~(Lambda)~~>*//*~~(Lambda.Parameters)~~>*/(b: Int): Int = a + b }
              """
          )
        );
    }

    @Test
    void lambdaParameters() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Lambda.Parameters.class)),
          scala(
            """
              object T { val f = (x: Int, y: Int) => x + y }
              """,
            """
              object T { val f = /*~~(Lambda.Parameters)~~>*/(x: Int, y: Int) => x + y }
              """
          )
        );
    }

    @Test
    void methodBodyWithoutBraces() {
        rewriteRun(
          spec -> spec.recipe(mark(J.Block.class)),
          scala(
            """
              object T { def f: Int = 1 }
              """,
            """
              object T /*~~(Block)~~>*/{ def f: Int =/*~~(Block)~~>*/ 1 }
              """
          )
        );
    }

    @Test
    void matchSelectorAndCases() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ControlParentheses.class, J.Block.class)),
          scala(
            """
              object T {
                def f(x: Int): Int = x match {
                  case 1 => 1
                  case _ => 2
                }
              }
              """,
            """
              object T /*~~(Block)~~>*/{
                def f(x: Int): Int =/*~~(Block)~~>*/ /*~~(ControlParentheses)~~>*/x match /*~~(Block)~~>*/{
                  case 1 =>/*~~(Block)~~>*/ 1
                  case _ =>/*~~(Block)~~>*/ 2
                }
              }
              """
          )
        );
    }

    @Test
    void indentedIfAndWhileConditions() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ControlParentheses.class)),
          scala(
            """
              object O:
                var i = 0
                while i < 10 do
                  i = if i > 5 then i + 2 else i + 1
              """,
            """
              object O:
                var i = 0
                while/*~~(ControlParentheses)~~>*/ i < 10 do
                  i = if/*~~(ControlParentheses)~~>*/ i > 5 then i + 2 else i + 1
              """
          )
        );
    }

    @Test
    void asInstanceOfType() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ControlParentheses.class)),
          scala(
            """
              object T { val s = (1: Any).asInstanceOf[Int] }
              """,
            """
              object T { val s = (1: Any).asInstanceOf/*~~(ControlParentheses)~~>*/[Int] }
              """
          )
        );
    }

    @Test
    void typeParametersAndBounds() {
        rewriteRun(
          spec -> spec.recipe(mark(J.TypeParameters.class, J.TypeBound.class)),
          scala(
            """
              object T { def f[A <: AnyRef](a: A): A = a }
              """,
            """
              object T { def f/*~~(TypeParameters)~~>*/[A /*~~(TypeBound)~~>*/<: AnyRef](a: A): A = a }
              """
          )
        );
    }

    @Test
    void parenthesizedType() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ParenthesizedTypeTree.class)),
          scala(
            """
              object T { def f: (Int => Unit) = _ => () }
              """,
            """
              object T { def f: /*~~(ParenthesizedTypeTree)~~>*/(Int => Unit) = _ => () }
              """
          )
        );
    }

    @Test
    void wildcardImport() {
        rewriteRun(
          spec -> spec.recipe(mark(J.FieldAccess.class, f -> "_".equals(((J.FieldAccess) f).getSimpleName()))),
          scala(
            """
              import scala.collection._
              """,
            """
              import /*~~(FieldAccess)~~>*/scala.collection._
              """
          )
        );
    }

    @Test
    void forComprehensionControl() {
        rewriteRun(
          spec -> spec.recipe(mark(J.ForEachLoop.Control.class)),
          scala(
            """
              object T { for (i <- 1 to 3) println(i) }
              """,
            """
              object T { for /*~~(ForEachLoop.Control)~~>*/(i <- 1 to 3) println(i) }
              """
          )
        );
    }

    @SafeVarargs
    private static Recipe mark(Class<? extends J>... types) {
        return mark(j -> {
            for (Class<? extends J> type : types) {
                if (type.isInstance(j)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static Recipe mark(Class<? extends J> type, Predicate<J> predicate) {
        return mark(j -> type.isInstance(j) && predicate.test(j));
    }

    private static Recipe mark(Predicate<J> predicate) {
        return toRecipe(() -> new ScalaVisitor<>() {
            @Override
            public J postVisit(J tree, ExecutionContext ctx) {
                return predicate.test(tree) ? SearchResult.found(tree, typeName(tree)) : tree;
            }
        });
    }

    private static String typeName(J tree) {
        return tree.getClass().getName().replaceFirst("^.*\\.J\\$", "").replace('$', '.');
    }
}
