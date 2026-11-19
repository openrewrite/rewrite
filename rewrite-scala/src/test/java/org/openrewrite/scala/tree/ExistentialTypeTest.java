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
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class ExistentialTypeTest implements RewriteTest {

    @Test
    void parameterAfterTheClauseIsKept() {
        rewriteRun(
          scala(
            """
              class Example {
                def process(list: List[T] forSome { type T }, n: Int): Unit = ()
              }
              """
          )
        );
    }

    @Test
    void valueBinding() {
        rewriteRun(
          scala(
            """
              class Example {
                def f(s: x.type forSome { val x: String }): Unit = ()
              }
              """
          )
        );
    }

    @Test
    void clauseDeclaresTheBoundNames() {
        rewriteRun(
          scala(
            """
              class Example {
                def f(m: Map[K, V] forSome { type K; type V <: AnyRef }): Unit = ()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<S.ExistentialType> existentials = new ArrayList<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J visitExistentialType(S.ExistentialType existentialType, Integer p) {
                        existentials.add(existentialType);
                        return super.visitExistentialType(existentialType, p);
                    }
                }.visit(cu, 0);
                assertThat(existentials).singleElement().satisfies(e -> {
                    assertThat(e.getUnderlying()).isInstanceOf(J.ParameterizedType.class);
                    assertThat(e.getClause().getStatements()).hasSize(2);
                });
            })
          )
        );
    }

    @Test
    void forSomeInAStringLiteralIsNotAKeyword() {
        rewriteRun(
          scala(
            """
              class Example {
                val s = "forSome"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                List<Object> values = new ArrayList<>();
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.Literal visitLiteral(J.Literal literal, Integer p) {
                        values.add(literal.getValue());
                        return literal;
                    }
                }.visit(cu, 0);
                assertThat(values).containsExactly("forSome");
            })
          )
        );
    }
}
