/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.table.SourcesFileErrors;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RecipeRunExceptionMarkupTest implements RewriteTest {

    private static final JavaVisitor<ExecutionContext> THROW_ON_METHOD = new JavaVisitor<>() {
        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            throw new IllegalStateException("boom");
        }
    };

    @Test
    void marksTreeWhereExceptionWasThrown() {
        rewriteRun(
          spec -> spec
            .executionContext(new InMemoryExecutionContext())
            .recipe(toRecipe(() -> THROW_ON_METHOD)),
          java(
            """
              class Test {
                  void m() {
                  }
              }
              """,
            """
              class Test {
                  /*~~(boom)~~>*/void m() {
                  }
              }
              """
          )
        );
    }

    @Test
    void marksNearestEnclosingTreeWhenThrownOnSynthesizedTree() {
        rewriteRun(
          spec -> spec
            .executionContext(new InMemoryExecutionContext())
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    // given a nested visit of a synthesized copy, positioned under the enclosing class
                    J.MethodDeclaration synthesized = method.withName(method.getName().withSimpleName("synthesized"));
                    return (J.MethodDeclaration) THROW_ON_METHOD.visitNonNull(synthesized, ctx, getCursor().getParentOrThrow());
                }
            }))
            // the recipe throws in both cycles, and each cycle records its error once
            .dataTable(SourcesFileErrors.Row.class, rows -> assertThat(rows).hasSize(2)),
          // when the nested visit throws
          // then the error lands on the nearest enclosing tree of the original source file
          java(
            """
              class Test {
                  void m() {
                  }
              }
              """,
            """
              class Test /*~~(boom)~~>*/{
                  void m() {
                  }
              }
              """
          )
        );
    }
}
