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
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class FunctionTypeTest implements RewriteTest {

    @Test
    void functionType() {
        rewriteRun(
          scala(
            """
            object Test {
              def apply(f: Int => Int, x: Int): Int = f(x)
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                S.FunctionType ft = firstFunctionType(cu);
                assertThat(ft.isParenthesized()).isFalse();
                assertThat(ft.getParameters()).hasSize(1);
                assertThat(((J.Identifier) ft.getParameters().get(0)).getSimpleName()).isEqualTo("Int");
                assertThat(((J.Identifier) ft.getReturnType()).getSimpleName()).isEqualTo("Int");
            })
          )
        );
    }

    @Test
    void byNameParameterIsAnEmptyUnparenthesizedFunctionType() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(x: => Int): Int = x
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                S.FunctionType ft = firstFunctionType(cu);
                assertThat(ft.getParameters()).isEmpty();
                assertThat(ft.isParenthesized()).isFalse();
                assertThat(((J.Identifier) ft.getReturnType()).getSimpleName()).isEqualTo("Int");
            })
          )
        );
    }

    @Test
    void byNameTypeParameterIsAnEmptyUnparenthesizedFunctionType() {
        rewriteRun(
          scala(
            """
            class Lazy[A](value: => A) {
              lazy val get: A = value
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                S.FunctionType ft = firstFunctionType(cu);
                assertThat(ft.getParameters()).isEmpty();
                assertThat(ft.isParenthesized()).isFalse();
                assertThat(((J.Identifier) ft.getReturnType()).getSimpleName()).isEqualTo("A");
            })
          )
        );
    }

    private static S.FunctionType firstFunctionType(J tree) {
        AtomicReference<S.FunctionType> ref = new AtomicReference<>();
        new ScalaIsoVisitor<Integer>() {
            @Override
            public J visitFunctionType(S.FunctionType functionType, Integer p) {
                ref.compareAndSet(null, functionType);
                return super.visitFunctionType(functionType, p);
            }
        }.visit(tree, 0);
        assertThat(ref.get()).as("should find an S.FunctionType").isNotNull();
        return ref.get();
    }
}
