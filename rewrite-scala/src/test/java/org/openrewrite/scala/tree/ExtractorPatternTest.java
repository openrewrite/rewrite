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
import org.openrewrite.Cursor;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.marker.InfixNotation;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class ExtractorPatternTest implements RewriteTest {

    private static List<J.DeconstructionPattern> deconstructionPatterns(J cu) {
        List<J.DeconstructionPattern> patterns = new ArrayList<>();
        List<J.MethodInvocation> invocationsInPatterns = new ArrayList<>();
        new ScalaIsoVisitor<Integer>() {
            @Override
            public J.DeconstructionPattern visitDeconstructionPattern(J.DeconstructionPattern pattern, Integer p) {
                patterns.add(pattern);
                return super.visitDeconstructionPattern(pattern, p);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if (getCursor().firstEnclosing(J.DeconstructionPattern.class) != null) {
                    invocationsInPatterns.add(method);
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);
        // ScalaVisitor swallows exceptions thrown while visiting a statement, so assert outside the visitor
        assertThat(invocationsInPatterns).isEmpty();
        return patterns;
    }

    private static String print(J cu, J pattern) {
        return pattern.printTrimmed(new Cursor(null, cu));
    }

    @Test
    void extractorPatternsAreDeconstructionPatterns() {
        rewriteRun(
          scala(
            """
              package p
              import scala.util.control.NonFatal
              object F {
                def m(o: Option[Int], l: List[Int], a: Array[String]): Int = {
                  o match { case Some(v) => v; case None => 0 }
                  l match { case h :: t => h; case Nil => 0 }
                  a match { case Array(x, y) => 1; case _ => 0 }
                  try 1 catch { case NonFatal(e) => 0 }
                }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).isEmpty();
                List<J.DeconstructionPattern> patterns = deconstructionPatterns(cu);
                assertThat(patterns).extracting(d -> print(cu, d))
                  .containsExactly("Some(v)", "h :: t", "Array(x, y)", "NonFatal(e)");

                J.DeconstructionPattern some = patterns.get(0);
                assertThat(TypeUtils.isOfClassType(some.getType(), "scala.Some")).isTrue();
                assertThat(TypeUtils.isOfClassType(some.getDeconstructor().getType(), "scala.Some$")).isTrue();
                assertThat(some.getNested()).singleElement()
                  .satisfies(v -> assertThat(((J.Identifier) v).getType()).isEqualTo(JavaType.Primitive.Int));

                J.DeconstructionPattern cons = patterns.get(1);
                assertThat(cons.getMarkers().findFirst(InfixNotation.class)).isPresent();
                assertThat(((J.Identifier) cons.getDeconstructor()).getSimpleName()).isEqualTo("::");
                assertThat(TypeUtils.isOfClassType(cons.getDeconstructor().getType(), "scala.collection.immutable.::$")).isTrue();
                assertThat(cons.getNested()).extracting(n -> ((J.Identifier) n).getSimpleName()).containsExactly("h", "t");

                assertThat(TypeUtils.isOfClassType(patterns.get(3).getDeconstructor().getType(), "scala.util.control.NonFatal$")).isTrue();
                assertThat(TypeUtils.isOfClassType(patterns.get(3).getType(), "java.lang.Throwable")).isTrue();
            })
          )
        );
    }

    @Test
    void nestedAndQualifiedExtractorPatterns() {
        rewriteRun(
          scala(
            """
              object F {
                case class Person(name: String, age: Int)
                case class Empty()
                def m(l: List[Int], x: Any): Int = {
                  l match { case a :: b :: rest => a; case List( ) => 0; case all @ List(head, _*) => head }
                  x match {
                    case F.Person( "x" , 3 ) => 1
                    case Person(n, a) | Person(n, a) if a > 1 => 2
                    case (Some(q), Empty()) => 3
                    case `Some`(_: String) => 4
                    case _ => 0
                  }
                }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).isEmpty();
                List<J.DeconstructionPattern> patterns = deconstructionPatterns(cu);
                assertThat(patterns).extracting(d -> print(cu, d)).containsExactly(
                  "a :: b :: rest", "b :: rest", "List( )", "List(head, _*)", "F.Person( \"x\" , 3 )",
                  "Person(n, a)", "Person(n, a)", "Some(q)", "Empty()", "`Some`(_: String)");
                assertThat(patterns.get(0).getNested().get(1)).isSameAs(patterns.get(1));
                assertThat(TypeUtils.isOfClassType(patterns.get(3).getDeconstructor().getType(), "scala.collection.immutable.List$")).isTrue();
                assertThat(patterns.get(4).getDeconstructor()).isInstanceOf(J.FieldAccess.class);
                assertThat(patterns.get(8).getNested()).isEmpty();
            })
          )
        );
    }

    @Test
    void typeAppliedExtractorPattern() {
        rewriteRun(
          scala(
            """
              object F {
                case class Box[T](t: T)
                def m(x: Any): Int = x match {
                  case Box[Int](q) => q
                  case _ => 0
                }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).isEmpty();
                List<J.DeconstructionPattern> patterns = deconstructionPatterns(cu);
                assertThat(patterns).extracting(d -> print(cu, d)).containsExactly("Box[Int](q)");
                assertThat(patterns.get(0).getDeconstructor()).isInstanceOf(J.ParameterizedType.class);
            })
          )
        );
    }

    @Test
    void extractorPatternWithTrailingComma() {
        rewriteRun(
          scala(
            """
              object F {
                case class P(a: Int, b: Int)
                def m(x: Any): Int = x match {
                  case P(
                    a,
                    b ,
                  ) => a
                  case _ => 0
                }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(deconstructionPatterns(cu)).singleElement()
              .satisfies(d -> assertThat(d.getNested()).hasSize(2)))
          )
        );
    }

    @Test
    void extractorPatternInForGenerator() {
        rewriteRun(
          scala(
            """
              object F {
                def m(os: List[Option[Int]]): List[Int] = for (Some(z) <- os) yield z
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMissingTypes.findMissingTypes(cu, false)).isEmpty();
                assertThat(deconstructionPatterns(cu)).extracting(d -> print(cu, d))
                  .containsExactly("Some(z)");
            })
          )
        );
    }
}
