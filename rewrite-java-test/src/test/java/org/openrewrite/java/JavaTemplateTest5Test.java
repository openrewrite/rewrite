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
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
public class JavaTemplateTest5Test implements RewriteTest {

    @Test
    void lastStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int n;").build();

              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getBody().getStatements().isEmpty()) {
                      return classDecl.withTemplate(t, classDecl.getBody().getCoordinates().lastStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              class Test {
                  int n;
              }
              """
          )
        );
    }

    @Test
    void lastStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 1;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return method.withTemplate(t, method.getBody().getCoordinates().lastStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStatementRequiringNewImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<String> s = null;")
                .imports("java.util.List")
                .build();

              @Override
              public J visitAssert(J.Assert azzert, ExecutionContext p) {
                  maybeAddImport("java.util.List");
                  return azzert.withTemplate(t, azzert.getCoordinates().replace());
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int n;
                  void test() {
                      List<String> s = null;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryBoxing")
    @Test
    void replaceArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "m, Integer.valueOf(n), \"foo\"").build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getArguments().size() == 1) {
                      return method.withTemplate(t, method.getCoordinates().replaceArguments());
                  }
                  return method;
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              J.MethodInvocation m = (J.MethodInvocation) ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(2)).getBody()
                .getStatements().get(0);
              JavaType.Method type = m.getMethodType();
              assertThat(type.getParameterTypes().get(0)).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1)).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(2)).matches(jt ->
                TypeUtils.asFullyQualified(jt).getFullyQualifiedName().equals("java.lang.String"));
          }),
          java(
            """
              abstract class Test {
                  abstract void test();
                  abstract void test(int m, int n, String foo);
                  void fred(int m, int n, String foo) {
                      test();
                  }
              }
              """,
            """
              abstract class Test {
                  abstract void test();
                  abstract void test(int m, int n, String foo);
                  void fred(int m, int n, String foo) {
                      test(m, Integer.valueOf(n), "foo");
                  }
              }
              """
          )
        );
    }

    Recipe replaceAnnotationRecipe = toRecipe(() -> new JavaIsoVisitor<>() {
        final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
            if (annotation.getSimpleName().equals("SuppressWarnings")) {
                return annotation.withTemplate(t, annotation.getCoordinates().replace());
            } else if (annotation.getSimpleName().equals("A1")) {
                return annotation.withTemplate(JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@A2")
                  .build(), annotation.getCoordinates().replace());
            }
            return super.visitAnnotation(annotation, p);
        }
    });

    @Test
    void replaceClassAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              @SuppressWarnings("ALL") class Test {}
              """,
            """
              @Deprecated class Test {}
              """
          )
        );
    }

    @Test
    void replaceMethodDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @SuppressWarnings("ALL")
                  void someTest() {}
              }
              """,
            """
              class A {
                  @Deprecated
                  void someTest() {}
              }
              """
          )
        );
    }

    @Test
    void replaceVariableDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  @A1
                  Object someObject;
              }
              """,
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  @A2
                  Object someObject;
              }
              """
          )
        );
    }

    @Test
    void replaceMethodDeclarationVariableDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  void someMethod(@A1 String a){}
              }
              """,
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  void someMethod(@A2 String a){}
              }
              """
          )
        );
    }

    @Test
    void replaceMethodAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return method.withTemplate(t, method.getCoordinates().replaceAnnotations());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })),
          java(
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  public @SuppressWarnings(WARNINGS) Test() {
                  }

                  public void test1() {
                  }

                  public @SuppressWarnings(WARNINGS) void test2() {
                  }
              }
              """,
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  public Test() {
                  }

                  @SuppressWarnings("other")
                  public void test1() {
                  }

                  @SuppressWarnings("other")
                  public void test2() {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceClassAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !classDecl.getSimpleName().equals("Test")) {
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceAnnotations());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  class Inner1 {
                  }
              }
              """,
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  class Inner1 {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return multiVariable.withTemplate(t, multiVariable.getCoordinates().replaceAnnotations());
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      // the m
                      int m;
                      final @SuppressWarnings("ALL") int n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      // the m
                      @SuppressWarnings("other")
                      int m;
                      @SuppressWarnings("other")
                      final int n;
                  }
              }
              """
          )
        );
    }

    @Test
    void addVariableAnnotationsToVariableAlreadyAnnotated() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().size() == 1) {
                      return multiVariable.withTemplate(t, multiVariable.getCoordinates().addAnnotation(comparing(a -> 0)));
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  @SuppressWarnings("ALL") private final int m, a;
                  void test() {
                      @SuppressWarnings("ALL") /* hello */
                      Boolean z;
                      // comment n
                      @SuppressWarnings("ALL")
                      int n;
                      @SuppressWarnings("ALL") final Boolean b;
                      @SuppressWarnings("ALL")
                      // comment x, y
                      private Boolean x, y;
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("ALL")
                  @Deprecated
                  private final int m, a;
                  void test() {
                      @SuppressWarnings("ALL")
                      @Deprecated /* hello */
                      Boolean z;
                      // comment n
                      @SuppressWarnings("ALL")
                      @Deprecated
                      int n;
                      @SuppressWarnings("ALL")
                      @Deprecated
                      final Boolean b;
                      @SuppressWarnings("ALL")
                      @Deprecated
                      // comment x, y
                      private Boolean x, y;
                  }
              }
              """
          )
        );
    }
}
