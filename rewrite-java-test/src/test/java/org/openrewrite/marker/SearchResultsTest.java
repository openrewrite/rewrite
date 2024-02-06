/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SearchResultsTest implements RewriteTest {

    @Test
    void searchResultIsOnlyAddedOnceEvenWhenRunMultipleTimesByScheduler() {
        rewriteRun(spec -> {
              spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                      return SearchResult.mergingFound(super.visitMethodInvocation(method, executionContext), method.getSimpleName());
                  }
              }));
          },
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello, world!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~(println)~~>*/System.out.println("Hello, world!");
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleSearchResultIsOnlyAddedOnceEvenWhenRunMultipleTimesByScheduler() {
        rewriteRun(spec -> {
              spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                      return SearchResult.mergingFound(
                        SearchResult.mergingFound(
                          SearchResult.mergingFound(
                            super.visitMethodInvocation(method, executionContext),
                            "42"
                          ),
                          "Hello, world!"
                        ),
                        method.getSimpleName()
                      );
                  }
              }));
          },
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello, world!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~(42, Hello, world!, println)~~>*/System.out.println("Hello, world!");
                  }
              }
              """
          )
        );
    }

    @Test
    void foundSearchResultsShouldNotClobberResultsWithDescription() {
        rewriteRun(spec -> {
              spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                      return SearchResult.found(
                        SearchResult.found(
                          super.visitMethodInvocation(method, executionContext),
                          "Hello, world!"
                        )
                      );
                  }
              }));
          },
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello, world!");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~(Hello, world!)~~>*/System.out.println("Hello, world!");
                  }
              }
              """
          )
        );
    }
}
