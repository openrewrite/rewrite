/*
 * Copyright 2022 the original author or authors.
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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.java.Assertions.java;

public class DataTableFieldTest implements RewriteTest {

    @Test
    void extractField() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  boolean isLocalVariable = getCursor().dropParentUntil(J.Block.class::isInstance).getParentTreeCursor().getValue() instanceof J.MethodDeclaration;
                  if (isLocalVariable) {
                      doAfterVisit(new ExtractField<>(multiVariable));
                  }
                  return multiVariable;
              }
          })),
          java(
            """
              import java.util.Date;
              class Test {
                  public Test() {
                     Date today = new Date();
                  }
              }
              """,
            """
              import java.util.Date;
              class Test {
                  private Date today;
                            
                  public Test() {
                      this.today = new Date();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    assertThat(requireNonNull(variable.getVariableType()).toString()).isEqualTo(
                      "Test{name=today,type=java.util.Date}");
                    return variable;
                }

                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, Object o) {
                    assertThat(requireNonNull(assignment.getType()).toString()).isEqualTo(
                      "java.util.Date");
                    return super.visitAssignment(assignment, o);
                }
            }.visit(cu, 0))
          )
        );
    }
}
