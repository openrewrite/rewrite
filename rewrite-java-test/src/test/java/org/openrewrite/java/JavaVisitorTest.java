/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaVisitorTest implements RewriteTest {

    @SuppressWarnings("RedundantThrows")
    @Test
    void javaVisitorHandlesPaddedWithNullElem() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  var mi = super.visitMethodInvocation(method, p);
                  if ("removeMethod".equals(mi.getSimpleName())) {
                      //noinspection ConstantConditions
                      return null;
                  }
                  return mi;
              }
          }).doNext(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  J.MethodDeclaration md = super.visitMethodDeclaration(method, p);
                  if (md.getSimpleName().equals("allTheThings")) {
                      md = md.withTemplate(JavaTemplate.builder(this::getCursor, "Exception").build(),
                        md.getCoordinates().replaceThrows()
                      );
                  }
                  return md;
              }
          }))).cycles(2).expectedCyclesThatMakeChanges(2),
          java(
            """
              class A {
                  void allTheThings() {
                      doSomething();
                      removeMethod();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """,
            """
              class A {
                  void allTheThings() throws Exception {
                      doSomething();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """
          )
        );
    }

    @Test
    void thrownExceptionsAreSpecific() {
        rewriteRun(
          spec -> spec
            .executionContext(new InMemoryExecutionContext())
            .afterRecipe(run -> assertThat(run.getResults().get(0).getRecipeErrors())
              .singleElement()
              .satisfies(t -> assertThat(t.getMessage()).containsSubsequence("A.java", "A", "allTheThings"))
            )
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Literal visitLiteral(final J.Literal literal, final ExecutionContext executionContext) {
                  throw new IllegalStateException("boom");
              }
          })),
          java(
            """
              class A {
                  void allTheThings() {
                      String var = "qwe";
                  }
              }
              """,
            """
              class A {
                  void allTheThings() {
                      String var = /*~~(boom)~~>*/"qwe";
                  }
              }
              """
          )
        );
    }
}
