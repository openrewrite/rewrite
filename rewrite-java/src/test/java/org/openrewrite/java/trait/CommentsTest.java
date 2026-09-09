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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Comments;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("NullableProblems")
class CommentsTest implements RewriteTest {

    @DocumentExample
    @Test
    void commentToMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).comment(" TODO: revisit");
              }
          })),
          java(
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

    @Test
    void commentIsIdempotent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).comment(" once");
              }
          })),
          java(
            """
              class Test {
                  // once
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void addBlockComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).multilineComment(" generated ");
              }
          })),
          java(
            """
              class Test {
                  void foo() {
                  }
              }
              """,
            """
              class Test {
                  /* generated */
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void editExistingComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).replaceComment(" old", " new");
              }
          })),
          java(
            """
              class Test {
                  // old
                  void foo() {
                  }
              }
              """,
            """
              class Test {
                  // new
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return Comments.of(getCursor()).removeComment(" remove me");
              }
          })),
          java(
            """
              class Test {
                  // remove me
                  void foo() {
                  }
              }
              """,
            """
              class Test {
                  void foo() {
                  }
              }
              """
          )
        );
    }
}
