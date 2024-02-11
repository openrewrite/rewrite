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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MethodMatcherTest implements RewriteTest {

    @DocumentExample
    @Test
    void matchesTopLevelFunction() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              private static final MethodMatcher methodMatcher = new MethodMatcher("openRewriteFile0Kt function(..)");
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (methodMatcher.matches(method.getMethodType())) {
                      return SearchResult.found(method);
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })),
          kotlin(
            """
              fun function() {}
              fun usesFunction() {
                  function()
              }
              """,
            """
              /*~~>*/fun function() {}
              fun usesFunction() {
                  function()
              }
              """
          )
        );
    }
}