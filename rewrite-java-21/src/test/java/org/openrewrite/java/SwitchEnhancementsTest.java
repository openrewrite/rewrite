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
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class SwitchEnhancementsTest implements RewriteTest {

    @Test
    void addSwitchGuard() {
        rewriteRun(
          spec -> spec
              .afterTypeValidationOptions(TypeValidation.none())
              .recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.Case visitCase(J.Case _case, ExecutionContext executionContext) {
                      if (_case.getBody() == null || _case.getGuard() != null || (!_case.getCaseLabels().isEmpty() && _case.getCaseLabels().get(0) instanceof J.Identifier)) {
                          return _case;
                      }
                      Expression expression = JavaTemplate.builder("\"YES\".equalsIgnoreCase(s)")
                          .contextSensitive()
                          .build()
                          .apply(new Cursor(getCursor(), _case.getBody()), ((Expression) _case.getBody()).getCoordinates().replace());
                      return _case.withGuard(expression);
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
