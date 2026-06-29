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
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class TypeApplyTest implements RewriteTest {

    @Test
    void listEmpty() {
        rewriteRun(
          scala(
            """
              object Test {
                val empties: List[Int] = List.empty[Int]
              }
              """
          )
        );
    }

    @Test
    void arrayEmpty() {
        rewriteRun(
          scala(
            """
              object Test {
                val arr = Array.empty[String]
              }
              """
          )
        );
    }

    @Test
    void qualifiedModuleEmpty() {
        rewriteRun(
          scala(
            """
              import scala.collection.mutable
              object Test {
                val m = mutable.Map.empty[String, Int]
              }
              """
          )
        );
    }

    @Test
    void multipleTypeArgs() {
        rewriteRun(
          scala(
            """
              object Test {
                val m: Map[String, Int] = Map.empty[String, Int]
              }
              """
          )
        );
    }

    @Test
    void typeApplyWithComplexType() {
        rewriteRun(
          scala(
            """
              object Test {
                val l = List.empty[List[Int]]
              }
              """
          )
        );
    }

    @Test
    void typeAppliedFunctionIdent() {
        rewriteRun(
          scala(
            """
              object Test {
                def id[A](a: A): A = a
                val x = id[Int](5)
              }
              """
          )
        );
    }

    @Test
    void typeApplyInMethodChain() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = List(1, 2, 3).map[String](x => x.toString)
              }
              """
          )
        );
    }

    @Test
    void typeApplyWithBlockArgument() {
        rewriteRun(
          scala(
            """
              object Test {
                intercept[IllegalArgumentException] {
                  foo()
                }
              }
              """
          )
        );
    }

    @Test
    void typeApplyWithChainedSelect() {
        rewriteRun(
          scala(
            """
              object Outer {
                object Inner {
                  def empty[A](): List[A] = Nil
                }
              }
              object Test {
                val x = Outer.Inner.empty[Int]()
              }
              """
          )
        );
    }

    @Test
    void classOfTypeApplyIsMethodInvocation() {
        rewriteRun(
          scala(
            """
              object Test {
                val x = classOf[String]
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<J.Identifier> misparsedIdentifier = new AtomicReference<>();
                AtomicReference<J.MethodInvocation> classOf = new AtomicReference<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if ("classOf[String]".equals(identifier.getSimpleName())) {
                            misparsedIdentifier.set(identifier);
                        }
                        return super.visitIdentifier(identifier, p);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        if ("classOf".equals(method.getSimpleName())) {
                            classOf.set(method);
                        }
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);

                assertThat(misparsedIdentifier.get())
                  .as("classOf[String] should not be parsed as a single identifier")
                  .isNull();
                assertThat(classOf.get()).isNotNull();
                assertThat(classOf.get().getArguments()).isEmpty();
                assertThat(classOf.get().getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class)).isPresent();
                assertThat(classOf.get().getTypeParameters()).singleElement().satisfies(typeParameter -> {
                    assertThat(typeParameter).isInstanceOf(J.Identifier.class);
                    assertThat(((J.Identifier) typeParameter).getSimpleName()).isEqualTo("String");
                });
            })
          )
        );
    }

    @Test
    void summonTypeApplyIsMethodInvocation() {
        rewriteRun(
          scala(
            """
              val x = summon[Foo]
              """,
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<J.MethodInvocation> summon = new AtomicReference<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        if ("summon".equals(method.getSimpleName())) {
                            summon.set(method);
                        }
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);

                assertThat(summon.get())
                  .as("summon[Foo] should be a method invocation, not crammed into an identifier")
                  .isNotNull();
                assertThat(summon.get().getArguments()).isEmpty();
                assertThat(summon.get().getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class)).isPresent();
                assertThat(summon.get().getTypeParameters()).singleElement().satisfies(typeParameter ->
                  assertThat(typeParameter).isInstanceOf(J.Identifier.class)
                    .extracting(t -> ((J.Identifier) t).getSimpleName()).isEqualTo("Foo"));
            })
          )
        );
    }

    @Test
    void significantCharactersInComments() {
        // visitTypeApply — `)` in line comment closing the value-arg list of a type-applied call
        rewriteRun(
          scala(
            """
              def f[T](x: T): T = x
              val r = f[Int](1 // )
              )
              """
          )
        );
    }
}
