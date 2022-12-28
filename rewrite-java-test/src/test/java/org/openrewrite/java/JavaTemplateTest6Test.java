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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
public class JavaTemplateTest6Test implements RewriteTest {

    @Test
    void addVariableAnnotationsToVariableNotAnnotated() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"ALL\")").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return multiVariable.withTemplate(
                        t,
                        multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                      );
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      final int m;
                      int n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      @SuppressWarnings("ALL")
                      final int m;
                      @SuppressWarnings("ALL")
                      int n;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1111")
    @Test
    void addMethodAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return method.withTemplate(t, method.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })),
          java(
            """
              class Test {
                  public void test0() {
                  }

                  static final String WARNINGS = "ALL";

                  void test1() {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("other")
                  public void test0() {
                  }

                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  void test1() {
                  }
              }
              """
          )
        );
    }

    @Test
    void addClassAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !classDecl.getSimpleName().equals("Test")) {
                      return classDecl.withTemplate(
                        t,
                        classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
                  class Inner1 {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("other")
                  class Inner1 {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if (annotation.getSimpleName().equals("SuppressWarnings")) {
                      return annotation.withTemplate(t, annotation.getCoordinates().replace());
                  }
                  return annotation;
              }
          })),
          java(
            """
              @SuppressWarnings("ALL")
              class Test {
              }
              """,
            """
              @Deprecated
              class Test {
              }
              """
          )
        );
    }

    @Test
    void replaceClassImplements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Serializable, Closeable")
                .imports("java.io.*")
                .build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getImplements() == null) {
                      maybeAddImport("java.io.Closeable");
                      maybeAddImport("java.io.Serializable");
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceImplementsClause());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              import java.io.Closeable;
              import java.io.Serializable;

              class Test implements Serializable, Closeable {
              }
              """
          )
        );
    }

    @Test
    void replaceClassExtends() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<String>")
                .imports("java.util.*")
                .build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getExtends() == null) {
                      maybeAddImport("java.util.List");
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceExtendsClause());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              import java.util.List;

              class Test extends List<String> {
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void replaceThrows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Exception").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getThrows() == null) {
                      return method.withTemplate(t, method.getCoordinates().replaceThrows());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              J.MethodDeclaration testMethodDecl = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
              assertThat(testMethodDecl.getMethodType().getThrownExceptions().stream().map(JavaType.FullyQualified::getFullyQualifiedName))
                .containsExactly("java.lang.Exception");
          }),
          java(
            """
              class Test {
                  void test() {}
              }
              """,
            """
              class Test {
                  void test() throws Exception {}
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void replaceMethodTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate typeParamsTemplate = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "T, U").build();

              final JavaTemplate methodArgsTemplate = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<T> t, U u")
                .imports("java.util.List")
                .build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getTypeParameters() == null) {
                      return method.withTemplate(
                          typeParamsTemplate,
                          method.getCoordinates().replaceTypeParameters()
                        )
                        .withTemplate(methodArgsTemplate, method.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
              assertThat(type).isNotNull();
              var paramTypes = type.getParameterTypes();
              assertThat(paramTypes.get(0))
                .as("The method declaration's type's genericSignature first argument should have have type 'java.util.List'")
                .matches(tType -> tType instanceof JavaType.FullyQualified &&
                                  TypeUtils.asFullyQualified(tType).getFullyQualifiedName().equals("java.util.List"));

              assertThat(paramTypes.get(1))
                .as("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                .matches(uType ->
                  uType instanceof JavaType.GenericTypeVariable &&
                  TypeUtils.asGeneric(uType).getName().equals("U") &&
                  TypeUtils.asGeneric(uType).getBounds().isEmpty());
          }),
          java(
            """
              import java.util.List;

              class Test {

                  void test() {
                  }
              }
              """,
            """
              import java.util.List;

              class Test {

                  <T, U> void test(List<T> t, U u) {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceClassTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "T, U").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getTypeParameters() == null) {
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceTypeParameters());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              class Test<T, U> {
              }
              """
          )
        );
    }

    @Test
    void replaceBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 1;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(0);
                  if (statement instanceof J.Unary) {
                      return method.withTemplate(t, method.getCoordinates().replaceBody());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMissingBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var m = method;
                  if (!m.isAbstract()) {
                      return m;
                  }
                  m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(Space.EMPTY));
                  m = m.withModifiers(emptyList());
                  m = m.withTemplate(t, m.getCoordinates().replaceBody());
                  return m;
              }
          })),
          java(
            """
              abstract class Test {
                  abstract void test();
              }
              """,
            """
              abstract class Test {
                  void test(){
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @Test
    @SuppressWarnings({
      "UnnecessaryBoxing",
      "CachedNumberConstructorCall",
      "ResultOfMethodCallIgnored"
      , "Convert2MethodRef"})
    void replaceNamedVariableInitializerMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher matcher = new MethodMatcher("Integer valueOf(..)");
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "new Integer(#{any()})").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (matcher.matches(method)) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getArguments().get(0));
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.function.Function;
              class Test {
                  void t() {
                      List<String> nums = Arrays.asList("1", "2", "3");
                      nums.forEach(s -> Integer.valueOf(s));
                  }
                  void inLambda(int i) {
                      Function<String, Integer> toString = it -> {
                          try {
                              return Integer.valueOf(it);
                          }catch (NumberFormatException ex) {
                              ex.printStackTrace();
                          }
                          return 0;
                      };
                  }
                  String inClassDeclaration(int i) {
                      return new Object() {
                          void foo() {
                              Integer.valueOf(i);
                          }
                      }.toString();
                  }
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.function.Function;
              class Test {
                  void t() {
                      List<String> nums = Arrays.asList("1", "2", "3");
                      nums.forEach(s -> new Integer(s));
                  }
                  void inLambda(int i) {
                      Function<String, Integer> toString = it -> {
                          try {
                              return new Integer(it);
                          }catch (NumberFormatException ex) {
                              ex.printStackTrace();
                          }
                          return 0;
                      };
                  }
                  String inClassDeclaration(int i) {
                      return new Object() {
                          void foo() {
                              new Integer(i);
                          }
                      }.toString();
                  }
              }
              """
          )
        );
    }
}
