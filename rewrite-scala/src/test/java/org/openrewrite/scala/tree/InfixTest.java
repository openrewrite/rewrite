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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.marker.InfixNotation;
import org.openrewrite.scala.marker.RightAssociative;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class InfixTest implements RewriteTest {

    @Test
    void namedInfix() {
        rewriteRun(
          scala("val x = list map func")
        );
    }

    @Test
    void namedInfixMax() {
        rewriteRun(
          scala("val x = 1 max 2")
        );
    }

    @Test
    void rightAssocCons() {
        rewriteRun(
          scala("val xs = 1 :: Nil")
        );
    }

    @Test
    void rightAssocConsChain() {
        rewriteRun(
          scala("val xs = 1 :: 2 :: 3 :: Nil")
        );
    }

    @Test
    void rightAssocPrepend() {
        rewriteRun(
          scala("val xs = 1 +: List(2, 3)")
        );
    }

    @Test
    void rightAssocWithExtraWhitespace() {
        rewriteRun(
          scala("val xs = 1  ::  Nil")
        );
    }

    @Test
    void leftAssocPreservesSelectAndArgument() {
        rewriteRun(
          scala(
            "val x = list map func",
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<J.MethodInvocation> call = new AtomicReference<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, Integer p) {
                        if (m.getMarkers().findFirst(InfixNotation.class).isPresent()) {
                            call.set(m);
                        }
                        return super.visitMethodInvocation(m, p);
                    }
                }.visit(cu, 0);

                J.MethodInvocation m = call.get();
                assertThat(m).as("should find infix call").isNotNull();
                assertThat(m.getMarkers().findFirst(RightAssociative.class)).isEmpty();
                assertThat(((J.Identifier) m.getSelect()).getSimpleName()).isEqualTo("list");
                assertThat(m.getSimpleName()).isEqualTo("map");
                assertThat(((J.Identifier) m.getArguments().get(0)).getSimpleName()).isEqualTo("func");
            })
          )
        );
    }

    @Test
    void rightAssocStoresSemantically() {
        rewriteRun(
          scala(
            "val xs = 1 :: Nil",
            spec -> spec.afterRecipe(cu -> {
                AtomicReference<J.MethodInvocation> call = new AtomicReference<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation m, Integer p) {
                        if (m.getMarkers().findFirst(RightAssociative.class).isPresent()) {
                            call.set(m);
                        }
                        return super.visitMethodInvocation(m, p);
                    }
                }.visit(cu, 0);

                J.MethodInvocation m = call.get();
                assertThat(m).as("should find right-associative infix call").isNotNull();
                assertThat(m.getMarkers().findFirst(InfixNotation.class)).isPresent();
                assertThat(m.getSimpleName()).isEqualTo("::");
                // Semantic storage: receiver is the right operand (Nil), argument is the left (1).
                assertThat(((J.Identifier) m.getSelect()).getSimpleName()).isEqualTo("Nil");
                assertThat(m.getArguments()).hasSize(1);
                assertThat(m.getArguments().get(0)).isInstanceOf(J.Literal.class);
                assertThat(((J.Literal) m.getArguments().get(0)).getValue()).isEqualTo(1);
            })
          )
        );
    }
}
