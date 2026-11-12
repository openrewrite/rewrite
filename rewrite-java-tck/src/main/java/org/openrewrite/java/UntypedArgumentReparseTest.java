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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UntypedArgumentReparseTest implements RewriteTest {

    @Test
    void callWithAnArgumentTheParserCannotTypeKeepsTheRecipesMethod() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    return method.withArguments(ListUtils.mapFirst(method.getArguments(), argument ->
                      argument instanceof J.Identifier ?
                        ((J.Identifier) argument).withSimpleName("undeclared").withFieldType(null) :
                        argument));
                }
            }))
            .recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(true).build()),
          java(
            """
              abstract class A {
                  abstract int f(String s);
                  abstract String f(char[] c);
                  void m(char[] c) {
                      f(c);
                  }
              }
              """,
            """
              abstract class A {
                  abstract int f(String s);
                  abstract String f(char[] c);
                  void m(char[] c) {
                      f(undeclared);
                  }
              }
              """
          )
        );
    }
}
