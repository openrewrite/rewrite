/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Comments;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Demonstrates that the language-agnostic {@link Comments} trait works on Groovy with the exact
 * same call site as Java, because Groovy reuses the {@code J} model and inherits the Java
 * {@code CommentService}.
 */
@SuppressWarnings("NullableProblems")
class CommentsTraitTest implements RewriteTest {

    @Test
    void commentToGroovyMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).comment(" TODO: revisit");
              }
          })),
          groovy(
            """
              class Test {
                  void foo() {
                  }
              }
              """,
            """
              class Test {
                  // TODO: revisit
                  void foo() {
                  }
              }
              """
          )
        );
    }
}
