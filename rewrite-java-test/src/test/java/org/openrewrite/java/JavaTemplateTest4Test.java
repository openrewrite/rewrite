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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
class JavaTemplateTest4Test implements RewriteTest {

    @DocumentExample
    @Test
    void replaceMethodParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("int m, java.util.List<String> n").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getSimpleName().equals("test") && method.getParameters().size() == 1) {
                      // insert in outer method
                      J.MethodDeclaration m = t.apply(getCursor(), method.getCoordinates().replaceParameters());
                      J.NewClass newRunnable = (J.NewClass) method.getBody().getStatements().get(0);

                      // insert in inner method
                      J.MethodDeclaration innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().get(0);
                      return t.apply(updateCursor(m), innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getChangeset().getAllResults().get(0).getAfter();
              J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
              JavaType.Method type = m.getMethodType();
              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("m", "n");
              assertThat(type.getParameterTypes().get(0))
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .matches(jt -> jt instanceof JavaType.Parameterized
                               && ((JavaType.Parameterized) jt).getType().getFullyQualifiedName().equals("java.util.List")
                               && ((JavaType.Parameterized) jt).getTypeParameters().size() == 1
                               && TypeUtils.asFullyQualified(((JavaType.Parameterized) jt).getTypeParameters().get(0)).getFullyQualifiedName().equals("java.lang.String"),
                  "Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'"
                );
              assertThat(m.getName().getType()).isEqualTo(type);
          }),
          java(
            """
              class Test {
                  void test() {
                      new Runnable() {
                          void inner() {
                          }
                          @Override
                          public void run() {}
                      };
                  }
              }
              """,
            """
              class Test {
                  void test(int m, java.util.List<String> n) {
                      new Runnable() {
                          void inner(int m, java.util.List<String> n) {
                          }
                          @Override
                          public void run() {}
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodParametersVariadicArray() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("Object[]... values").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getSimpleName().equals("test") && method.getParameters().get(0) instanceof J.Empty) {
                      // insert in outer method
                      J.MethodDeclaration m = t.apply(getCursor(), method.getCoordinates().replaceParameters());
                      J.NewClass newRunnable = (J.NewClass) method.getBody().getStatements().get(0);

                      // insert in inner method
                      J.MethodDeclaration innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().get(0);
                      return t.apply(updateCursor(m), innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getChangeset().getAllResults().get(0).getAfter();
              final JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0))
                .getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("values");

              var param = TypeUtils.asArray(type.getParameterTypes().get(0));
              assertThat(param.getElemType())
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                .matches(at -> TypeUtils.asFullyQualified(TypeUtils.asArray(at).getElemType()).getFullyQualifiedName()
                  .equals("java.lang.Object"));
          }),
          java(
            """
              class Test {
                  void test() {
                      new Runnable() {
                          void inner() {
                          }
                      };
                  }
              }
              """,
            """
              class Test {
                  void test(Object[]... values) {
                      new Runnable() {
                          void inner(Object[]... values) {
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAndInterpolateMethodParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  {
                      if (method.getSimpleName().equals("test") && method.getParameters().size() == 1) {
                          return JavaTemplate.builder("int n, #{}")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), method.getCoordinates().replaceParameters(), method.getParameters().get(0));
                      }
                      return method;
                  }
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getChangeset().getAllResults().get(0).getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("n", "s");
              assertThat(type.getParameterTypes().get(0))
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .as("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches(jt -> TypeUtils.asFullyQualified(jt).getFullyQualifiedName().equals("java.lang.String"));
          }),
          java(
            """
              class Test {
                  void test(String s) {
                  }
              }
              """,
            """
              class Test {
                  void test(int n, String s) {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceLambdaParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext p) {
                  if (lambda.getParameters().getParameters().size() == 1) {
                      return JavaTemplate.apply("int m, int n", getCursor(), lambda.getParameters().getCoordinates().replace());
                  }
                  return super.visitLambda(lambda, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      Object o = () -> 1;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      Object o = (int m, int n) -> 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSingleStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitAssert(J.Assert assert_, ExecutionContext p) {
                  return JavaTemplate.builder(
                      """
                        if(n != 1) {
                          n++;
                        }"""
                    )
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), assert_.getCoordinates().replace());
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
                      if (n != 1) {
                          n++;
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void replaceStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(1);
                  if (statement instanceof J.Unary) {
                      return JavaTemplate.builder("n = 2;\nn = 3;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), statement.getCoordinates().replace());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n = 2;
                      n = 3;
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(0);
                  if (statement instanceof J.Assignment) {
                      return JavaTemplate.builder("assert n == 0;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), statement.getCoordinates().before());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
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
    void afterStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return JavaTemplate.builder("n = 1;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getBody().getStatements().get(0).getCoordinates().after());
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getBody().getStatements().size() == 1) {
                      return JavaTemplate.apply("int m;", getCursor(), classDecl.getBody().getCoordinates().firstStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
                  // comment
                  int n;
              }
              """,
            """
              class Test {
                  int m;
                  // comment
                  int n;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return JavaTemplate.apply("int m = 0;", getCursor(), method.getBody().getCoordinates().firstStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      // comment
                      int n = 1;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      int m = 0;
                      // comment
                      int n = 1;
                  }
              }
              """
          )
        );
    }
}
