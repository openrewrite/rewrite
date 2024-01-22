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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;


/**
 * These JavaTemplate tests are specific to the annotation matching syntax.
 */
class JavaTemplateAnnotationTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceAnnotation() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2)
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                      return JavaTemplate.apply("@Deprecated(since = \"#{}\", forRemoval = true)",
                        getCursor(), annotation.getCoordinates().replace(), "2.0");
                  }
              }
            )),
          java(
            """
              @Deprecated(since = "1.0", forRemoval = true)
              class A {
              }
                """,
            """
              @Deprecated(since = "2.0", forRemoval = true)
              class A {
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void replaceAnnotation2() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                      annotation = JavaTemplate.apply("@Deprecated(since = \"#{any(java.lang.String)}\", forRemoval = true)",
                        getCursor(), annotation.getCoordinates().replace(), "2.0");
                      return annotation;
                  }
              }
            )),
          java(
            """
              @Deprecated(since = "1.0", forRemoval = true)
              class A {
              }
                """,
            """
              @Deprecated(since = "2.0", forRemoval = true)
              class A {
              }
              """
          )
        );
    }
}
