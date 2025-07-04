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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
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
    void replaceClassAnnotation() {
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

    @Test
    void replaceNestedClassAnnotation() {
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
              class A {
                  @Deprecated(since = "1.0", forRemoval = true)
                  class B {
                  }
              }
              """,
            """
              class A {
                  @Deprecated(since = "2.0", forRemoval = true)
                  class B {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAnnotation2() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                      annotation = JavaTemplate.apply("@Deprecated(since = \"#{}\", forRemoval = true)",
                        getCursor(), annotation.getCoordinates().replace(), "2.0");
                      return annotation;
                  }
              }
            ))
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
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

    @Issue("https://github.com/openrewrite/rewrite/issues/4634")
    @Test
    void replacesInRecordVisitor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if (annotation.getSimpleName().equals("NotNull")) {
                      maybeRemoveImport("org.jetbrains.annotations.NotNull");
                      maybeAddImport("lombok.NonNull");
                      return JavaTemplate.builder("@NonNull")
                        .imports("lombok.NonNull")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replace());
                  }
                  return annotation;
              }
          })),
          java(
            """
              import org.jetbrains.annotations.NotNull;
  
              public record Person(
                  @NotNull String firstName,
                  @NotNull String lastName
              ) {}
              """,
            """
              import lombok.NonNull;

              public record Person(
                  @NonNull String firstName,
                  @NonNull String lastName
              ) {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5712")
    @Test
    void replaceArgumentsInNestedAnnotation() {
        @Language("java")
        String annotations = """
          package foo;

          import java.lang.annotation.*;

          @Repeatable(NestedAnnotations.class)
          public @interface NestedAnnotation {
              String a() default "";
              String b() default "";
          }

          public @interface NestedAnnotations {
              NestedAnnotation[] value();
          }
          """;
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                    if (annotation.getSimpleName().equals("NestedAnnotation") &&
                      !annotation.getArguments().isEmpty()) {
                        // Check if this annotation still has the 'a' attribute that needs to be replaced
                        J.Assignment arg = (J.Assignment) annotation.getArguments().get(0);
                        if (arg.getVariable() instanceof J.Identifier &&
                          ((J.Identifier) arg.getVariable()).getSimpleName().equals("a")) {
                            // Only apply the template if we haven't already transformed this annotation
                            J.Literal value = (J.Literal) arg.getAssignment();

                            // Replace 'a' with 'b' in the annotation
                            return JavaTemplate.builder("@NestedAnnotation(b = #{any(java.lang.String)})")
                              .javaParser(JavaParser.fromJavaVersion().dependsOn(annotations))
                              .imports("foo.*")
                              .build()
                              .apply(getCursor(), annotation.getCoordinates().replace(), value);
                        }
                    }
                    return super.visitAnnotation(annotation, ctx);
                }
            })),
          java(
            """
              import foo.*;

              @NestedAnnotations({
                      @NestedAnnotation(a = "1"),
                      @NestedAnnotation(a = "2")
              })
              class Test {
              }
              """,
            """
              import foo.*;

              @NestedAnnotations({
                      @NestedAnnotation(b = "1"),
                      @NestedAnnotation(b = "2")
              })
              class Test {
              }
              """
          )
        );
    }

}
