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

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class EndMarkerTest implements RewriteTest {

    @Test
    void typeDefinitions() {
        rewriteRun(
          scala(
            """
              class A:
                def x = 1
              end A
              """
          ),
          scala(
            """
              trait B:
                def x = 1
              end B
              """
          ),
          scala(
            """
              object C:
                def x = 1
              end C
              """
          ),
          scala(
            """
              enum D:
                case E, F
              end D
              """
          )
        );
    }

    @Test
    void givens() {
        rewriteRun(
          scala(
            """
              trait Ord[T]:
                def x: Int
              given intOrd: Ord[Int] with
                def x = 1
              end intOrd
              given Ord[Long] with
                def x = 2
              end given
              """
          )
        );
    }

    @Test
    void anonymousGivenAliases() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              object O:
                given Ord[Int] =
                  new Ord[Int] {}
                end given
                given (using Ord[Int]): Ord[Long] =
                  new Ord[Long] {}
                end given
              """
          )
        );
    }

    @Test
    void quotedNames() {
        rewriteRun(
          scala(
            """
              class `my class`:
                def `my f` =
                  1
                end `my f`
              end `my class`
              """
          )
        );
    }

    @Test
    void methodsAndValues() {
        rewriteRun(
          scala(
            """
              class A(i: Int):
                def this() =
                  this(1)
                end this
                def f =
                  1
                end f
                val v =
                  1
                end v
                val (a, b) =
                  (1, 2)
                end val
              """
          )
        );
    }

    @Test
    void controlFlow() {
        rewriteRun(
          scala(
            """
              object O:
                def f(b: Boolean, i: Int, xs: List[Int]): Unit =
                  if b then
                    println(1)
                  end if
                  i match
                    case 1 => println(1)
                    case _ => println(2)
                  end match
                  while false do
                    println(1)
                  end while
                  for x <- xs do
                    println(x)
                  end for
                  try
                    println(1)
                  finally
                    println(2)
                  end try
              """
          )
        );
    }

    @Test
    void packageExtensionAndNew() {
        rewriteRun(
          scala(
            """
              package p:
                object O:
                  extension (i: Int)
                    def twice = i * 2
                  end extension
                  def f(): Unit =
                    new Object:
                      def x = 1
                    end new
              end p
              """
          )
        );
    }

    @Test
    void bracedBodies() {
        rewriteRun(
          scala(
            """
              class A {
                def x = 1
              }
              end A
              """
          ),
          scala(
            """
              object B {
                def x = 1
              }
              end B
              """
          ),
          scala(
            """
              object C:
                extension (i: Int) {
                  def twice = i * 2
                }
                end extension
              """
          )
        );
    }

    @Test
    void commentBetweenEndAndName() {
        rewriteRun(
          scala(
            """
              class A:
                def x = 1
              end /* a /* b */ c */ A
              """
          )
        );
    }

    @Test
    void endMarkerTakesTheNameOfWhatItCloses() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  return classDecl.withName(classDecl.getName().withSimpleName("B"));
              }
          })),
          scala(
            """
              class A:
                def x = 1
              end A
              """,
            """
              class B:
                def x = 1
              end B
              """
          )
        );
    }
}
