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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Comparator;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateMatchTest implements RewriteTest {

    /**
     * Adding an annotation via JavaTemplate to a method inside an anonymous class within a Groovy script
     * (no top-level class declarations) crashes in AnnotationTemplateGenerator.template with IndexOutOfBoundsException
     */
    @Test
    void addAnnotationToMethodInAnonymousClassInsideGroovyScript() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if ("a".equals(method.getSimpleName()) &&
                    method.getLeadingAnnotations().stream().noneMatch(a -> "Ann".equals(a.getSimpleName()))) {
                      return JavaTemplate.builder("@Ann")
                        .build()
                        .apply(getCursor(), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }
          })),
          groovy(
            """
              foo(new A() {
                  void a() {
                  }
              })
              """,
            """
              foo(new A() {
                  @Ann
                  void a() {
                  }
              })
              """
          )
        );
    }

    @Test
    void nonJavaCode() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                "\"a\" + \"b\""
              ).build();

              @Override
              public J visitExpression(Expression expression, ExecutionContext ctx) {
                  return expression.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(getCursor()) ?
                    SearchResult.found(expression) : super.visitExpression(expression, ctx);
              }
          })),
          groovy(
            //language=groovy
            """
              class T {
                  def foo = "${1}"
              }
              """
          )
        );
    }
}
