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
package org.openrewrite.scala.tree;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.marker.UnicodeArrow;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UnicodeArrowTest implements RewriteTest {

    @Test
    void lambdaWithTypedParam() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) ⇒ x
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(J.Lambda.class))
            )
        );
    }

    @Test
    void lambdaWithUntypedParam() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f: Int => Int = x ⇒ x
                }
                """
            )
        );
    }

    @Test
    void caseClause() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(x: Int) = x match {
                    case 1 ⇒ "one"
                    case _ ⇒ "other"
                  }
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(J.Case.class, J.Case.class))
            )
        );
    }

    @Test
    void partialFunctionLiteral() {
        rewriteRun(
            scala(
                """
                object Test {
                  val xs = List(1).collect { case x ⇒ x }
                }
                """
            )
        );
    }

    @Test
    void functionType() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f: Int ⇒ Int = identity
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(S.FunctionType.class))
            )
        );
    }

    @Test
    void byNameParameter() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(x: ⇒ Int) = x
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(S.FunctionType.class))
            )
        );
    }

    @Test
    void importRename() {
        rewriteRun(
            scala(
                """
                import scala.collection.{mutable ⇒ m}
                object Test
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(S.ImportSelector.class))
            )
        );
    }

    @Test
    void forGenerator() {
        rewriteRun(
            scala(
                """
                object Test {
                  val xs = for (x ← List(1)) yield x
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(S.For.Enumerator.class))
            )
        );
    }

    @Test
    void forEachGenerator() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (x ← List(1)) println(x)
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(J.ForEachLoop.class))
            )
        );
    }

    @Test
    void mixedAsciiAndUnicodeArrowsInOneFile() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) => x
                  val g = (x: Int) ⇒ x
                  val xs = for {
                    a <- List(1)
                    b ← List(2)
                  } yield a + b
                }
                """
            )
        );
    }

    @Test
    void caseWithGuard() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(x: Int) = x match {
                    case n if n > 0 ⇒ "positive"
                    case _ ⇒ "other"
                  }
                }
                """
            )
        );
    }

    @Test
    void catchClause() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f() = try {
                    1
                  } catch {
                    case e: Exception ⇒ 0
                  }
                }
                """,
                spec -> spec.afterRecipe(cu -> assertThat(unicodeArrowOwners(cu)).containsExactly(J.Try.Catch.class))
            )
        );
    }

    @Test
    void importHiding() {
        rewriteRun(
            scala(
                """
                import scala.collection.{mutable ⇒ _, _}
                object Test
                """
            )
        );
    }

    @Test
    void lambdaWithFunctionTypedParam() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (g: Int ⇒ Int) ⇒ g(1)
                }
                """
            )
        );
    }

    @Test
    void lambdaInBraces() {
        rewriteRun(
            scala(
                """
                object Test {
                  val xs = List(1).map { x ⇒
                    x + 1
                  }
                }
                """
            )
        );
    }

    @Test
    void byNameParameterInFunctionType() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(g: (⇒ Int) ⇒ Int) = g(1)
                }
                """
            )
        );
    }

    @Test
    void unicodeArrowFollowedByAsciiArrowLaterInFile() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) ⇒ x
                  def g(x: Int) = x match {
                    case 1 => "one"
                  }
                  val h: Int => Int = x => x
                }
                """
            )
        );
    }

    @Test
    void arrowInStringBeforeToken() {
        rewriteRun(
            scala(
                """
                object Test {
                  def f(x: String) = x match {
                    case "⇒" ⇒ 1
                    case "=>" => 2
                  }
                }
                """
            )
        );
    }

    @Test
    void removingMarkerPrintsAsciiArrows() {
        rewriteRun(
            spec -> spec.recipe(toRecipe(() -> new ScalaIsoVisitor<>() {
                @Override
                public @Nullable J preVisit(J tree, ExecutionContext ctx) {
                    return tree.withMarkers(tree.getMarkers().removeByType(UnicodeArrow.class));
                }
            })),
            scala(
                """
                import scala.collection.{mutable ⇒ m}
                object Test {
                  val f: Int ⇒ Int = (x: Int) ⇒ x
                  def g(x: ⇒ Int) = x match {
                    case 1 ⇒ for (a ← List(1)) yield a
                    case _ ⇒ try 1 catch { case e: Exception ⇒ 0 }
                  }
                  for (x ← List(1)) println(x)
                }
                """,
                """
                import scala.collection.{mutable => m}
                object Test {
                  val f: Int => Int = (x: Int) => x
                  def g(x: => Int) = x match {
                    case 1 => for (a <- List(1)) yield a
                    case _ => try 1 catch { case e: Exception => 0 }
                  }
                  for (x <- List(1)) println(x)
                }
                """
            )
        );
    }

    private static List<Class<?>> unicodeArrowOwners(Tree tree) {
        return new ScalaIsoVisitor<List<Class<?>>>() {
            @Override
            public @Nullable J preVisit(J t, List<Class<?>> acc) {
                if (t.getMarkers().findFirst(UnicodeArrow.class).isPresent()) {
                    acc.add(t.getClass());
                }
                return t;
            }
        }.reduce(tree, new ArrayList<>());
    }
}
