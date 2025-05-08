/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SwitchEnhancementsTest implements RewriteTest {

    @Test
    void addSwitchGuard() {
        rewriteRun(
          spec -> spec
            .recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Case visitCase(J.Case case_, ExecutionContext ctx) {
                    if (case_.getBody() == null || case_.getGuard() != null || case_.getCaseLabels().getFirst() instanceof J.Identifier) {
                        return case_;
                    }
                    J.Identifier s = ((J.VariableDeclarations) case_.getCaseLabels().getFirst()).getVariables().getFirst().getName();
                    Expression expression = JavaTemplate.apply(
                      "\"YES\".equalsIgnoreCase(#{any(String)})",
                      new Cursor(getCursor(), case_.getBody()),
                      ((Expression) case_.getBody()).getCoordinates().replace(),
                      s);
                    return autoFormat(case_.withGuard(expression), ctx);
                }
            })),
          //language=java
          java(
            """
              class Test {
                  void guardedCases(Object o) {
                      switch (o) {
                          case Integer i when i > 0 -> System.out.println("Perfect");
                          case String s -> System.out.println("Great");
                          default -> System.out.println("Ok");
                      }
                  }
              }
              """,
            """
              class Test {
                  void guardedCases(Object o) {
                      switch (o) {
                          case Integer i when i > 0 -> System.out.println("Perfect");
                          case String s when "YES".equalsIgnoreCase(s) -> System.out.println("Great");
                          default -> System.out.println("Ok");
                      }
                  }
              }
              """
          ));
    }
}
