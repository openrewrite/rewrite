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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateContextFreeTest implements RewriteTest {

    @Test
    void replaceMethodBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return method.getBody() != null && JavaTemplate.matches("System.out.println(1);",
                    new Cursor(new Cursor(getCursor(), method.getBody()), method.getBody().getStatements().get(0))) ?
                    JavaTemplate.apply("System.out.println(2);", getCursor(), method.getCoordinates().replaceBody()) :
                    super.visitMethodDeclaration(method, ctx);
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

    @Test
    void replaceField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                  if (vd.getVariables().size() == 1 && vd.getVariables().get(0).getSimpleName().equals("i")) {
                      return JavaTemplate.apply("Integer i = 2;", getCursor(), vd.getCoordinates().replace());
                  }
                  return super.visitVariableDeclarations(vd, ctx);
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  private Integer i = 1;
                  void m() {
                      Integer i = 1;
                      Object o = new Object() {
                          private final Integer i = 1;
                      };
                  }
              }
              """,
            """
              class Test {
                  Integer i = 2;
                  void m() {
                      Integer i = 2;
                      Object o = new Object() {
                          Integer i = 2;
                      };
                  }
              }
              """
          ));
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void genericsAndAnyParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                  if (literal.getMarkers().findFirst(SearchResult.class).isEmpty() &&
                      (Objects.equals(literal.getValue(), 1) || Objects.requireNonNull(literal.getValue()).equals("s"))) {
                      return JavaTemplate.apply("java.util.List.of(#{any()})", getCursor(), literal.getCoordinates().replace(), SearchResult.found(literal));
                  }
                  return super.visitLiteral(literal, executionContext);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      Object o;
                      o = 1;
                      o = 2;
                      o = "s";
                      o = "s2";
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      Object o;
                      o = java.util.List.of(/*~~>*/1);
                      o = 2;
                      o = java.util.List.of(/*~~>*/"s");
                      o = "s2";
                  }
              }
              """,
            sourceSpecs -> sourceSpecs.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @SuppressWarnings("DataFlowIssue")
                @Override
                public <M extends Marker> M visitMarker(Marker marker, Object o) {
                    if (marker instanceof SearchResult) {
                        J.Literal literal = getCursor().getValue();
                        Expression parent = getCursor().getParentTreeCursor().getValue();
                        if (literal.getType() == JavaType.Primitive.Int) {
                            assertThat(parent.getType().toString()).isEqualTo("java.util.List<java.lang.Integer>");
                        } else if (literal.getType() == JavaType.Primitive.String) {
                            assertThat(parent.getType().toString()).isEqualTo("java.util.List<java.lang.String>");
                        }
                    }
                    return super.visitMarker(marker, o);
                }
            }.visit(cu, 0))
          ));
    }

}
