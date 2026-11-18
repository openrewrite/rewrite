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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.marker.Given;
import org.openrewrite.scala.marker.ParameterClause;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class GivenTest implements RewriteTest {

    @Test
    void givenWithBodyIsAClassDeclarationWhoseMethodsAVisitorReaches() {
        rewriteRun(
          scala(
            """
              trait Ord[T] {
                def compare(a: T, b: T): Int
              }
              object Test {
                given intOrd: Ord[Int] with {
                  def compare(a: Int, b: Int): Int = a - b
                }
                given Ord[Long] with {
                  def compare(a: Long, b: Long): Int = 0
                }
                given [T](using ord: Ord[T]): Ord[List[T]] with {
                  def compare(a: List[T], b: List[T]): Int = 0
                }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<String> methods = new ArrayList<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                        J.ClassDeclaration owner = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                        if (owner.getMarkers().findFirst(Given.class).isPresent()) {
                            String typeParameters = owner.getTypeParameters() == null ? "" :
                              owner.getTypeParameters().stream().map(tp -> tp.getName().printTrimmed(getCursor()))
                                .collect(joining(",", "[", "]"));
                            List<Statement> parameters = owner.getPrimaryConstructor();
                            String usingClause = parameters == null || parameters.isEmpty() ? "" :
                              parameters.stream().map(s -> ((J.VariableDeclarations) s).getVariables().getFirst().getSimpleName())
                                .collect(joining(",", "(", ")"));
                            methods.add((owner.hasModifier(J.Modifier.Type.Final) ? "final " : "") +
                                        "given " + owner.getSimpleName() + typeParameters + usingClause + ": " +
                                        owner.getExtends().printTrimmed(getCursor()) + " { " + method.getSimpleName() + " }");
                        }
                        return method;
                    }
                }.visit(cu, 0);
                assertThat(methods).containsExactly(
                  "final given intOrd: Ord[Int] { compare }",
                  "final given : Ord[Long] { compare }",
                  "given [T](ord): Ord[List[T]] { compare }"
                );
            })
          )
        );
    }

    @Test
    void laterParameterClausesAreConstructorParameters() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              trait Show[T]
              trait Executor
              class C(a: Int)(using e: Executor)
              class D(a: Int)( )
              object Test {
                given [T] => (a: Ord[T])  =>/* c */( b: Show[T] ) => Ord[List[T]]:
                  def x = 1
                given [T] => Ord[T] => Show[T] => Ord[Vector[T]]:
                  def x = 1
                given seqOrd[T](using a: Ord[T]) (using b: Show[T]): Ord[Seq[T]] with {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<String> clauses = new ArrayList<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        List<Statement> parameters = classDecl.getPrimaryConstructor();
                        if (parameters != null && parameters.size() > 1) {
                            StringBuilder shape = new StringBuilder("(");
                            for (int i = 0; i < parameters.size(); i++) {
                                Statement parameter = parameters.get(i);
                                if (i > 0) {
                                    shape.append(parameter.getMarkers().findFirst(ParameterClause.class).isPresent() ? ")(" : ",");
                                }
                                if (parameter instanceof J.VariableDeclarations) {
                                    shape.append(((J.VariableDeclarations) parameter).getVariables().getFirst().getSimpleName());
                                }
                            }
                            clauses.add(shape.append(")").toString());
                        }
                        return super.visitClassDeclaration(classDecl, p);
                    }
                }.visit(cu, 0);
                assertThat(clauses).containsExactly("(a)(e)", "(a)()", "(a)(b)", "(x$1)(x$2)", "(a)(b)");
            })
          )
        );
    }

    @Test
    void bodyIntroducedByWith() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              object Test {
                given Ord[Long] with
                  def compare(a: Long, b: Long): Int = 0
                given shortOrd: Ord[Short]
                  with {
                    def compare(a: Short, b: Short): Int = 0
                  }
                given Ord[Byte] with {}
              }
              """
          )
        );
    }

    @Test
    void bodyInScala36Syntax() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              object Test {
                given intOrd: Ord[Int]:
                  def compare(a: Int, b: Int): Int = a - b
                given Ord[Long] {
                  def compare(a: Long, b: Long): Int = 0
                }
              }
              """
          )
        );
    }

    @Test
    void givenWithoutBody() {
        rewriteRun(
          scala(
            """
              class Foo(i: Int)
              trait Bar
              trait Ord[T]
              object Test {
                given Foo(1)
                given foo: Foo(2) with Bar
              }
              trait Abstract {
                given Ord[Int]
              }
              """
          )
        );
    }

    @Test
    void conditionsFollowedByArrows() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              object Test {
                given [T: Ord] => Ord[List[T]]:
                  def x = 1
                given vectorOrd: [T: Ord] => Ord[Vector[T]]:
                  def x = 1
                given [T] => (ord: Ord[T]) => Ord[Option[T]] {
                  def x = 1
                }
                given Ord[Int] => Ord[Seq[Int]]:
                  def x = 1
                given () => Ord[Unit]:
                  def x = 1
              }
              """
          )
        );
    }

    @Test
    void commentsThatSpellADelimiter() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              trait Show
              object Test {
                @deprecated /* given */ private given /* : */ intOrd /* with */ : /* => */ Ord[Int] /* { */ with Show /* : */ with /* { */ {
                  // member
                  def compare(a: Int, b: Int): Int = a - b
                } // after
                given /* : */ [T] /* : */ => /* with */ Ord[List[T]] /* { */ :
                  def x = 1
              }
              """
          )
        );
    }

    @Test
    void nestedComments() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              trait Show
              object Test {
                private /* a /* b */ c */ given longOrd /* d /* e */ f */ : Ord[Long] /* g /* h */ i */ with Show with {}
              }
              """
          )
        );
    }

    @Test
    void parentInParentheses() {
        rewriteRun(
          scala(
            """
              trait Ord[T]
              object Test {
                given (Ord[Int]) with {}
              }
              """
          )
        );
    }
}
