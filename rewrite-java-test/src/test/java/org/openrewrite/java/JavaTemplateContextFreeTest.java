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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateContextFreeTest implements RewriteTest {

    @Test
    void replaceMethodBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate before = JavaTemplate.builder("System.out.println(1);").build();
              private final JavaTemplate after = JavaTemplate.builder("System.out.println(2);").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return before.matches(method.getBody()) ? method.withTemplate(after, getCursor(), method.getCoordinates().replaceBody()) : super.visitMethodDeclaration(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      System.out.println(1);
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      System.out.println(2);
                  }
              }
              """
          ));
    }

}
