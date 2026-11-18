/*
 * Copyright 2026 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "StatementWithEmptyBody", "UnusedAssignment", "SizeReplaceableByIsEmpty", "ResultOfMethodCallIgnored", "RedundantOperationOnEmptyContainer"})
class JavaTemplateDeclarationTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/550")
    @Test
    void genericTypeVariable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getTypeExpression() instanceof J.Identifier && "var".equals(((J.Identifier) multiVariable.getTypeExpression()).getSimpleName())) {
                      return multiVariable;
                  }
                  J.VariableDeclarations.NamedVariable var0 = multiVariable.getVariables().getFirst();
                  return JavaTemplate.apply("var #{} = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), var0.getSimpleName(), var0.getInitializer());
              }
          })),
          java(
            """
              import java.io.Serializable;

              abstract class Outer<T extends Serializable> {
                  abstract T doIt();
                  void trigger() {
                      T x = doIt();
                  }
              }
              """,
            """
              import java.io.Serializable;

              abstract class Outer<T extends Serializable> {
                  abstract T doIt();
                  void trigger() {
                      var x = doIt();
                  }
              }
              """
          )
        );
    }

    @Test
    void changeFieldToMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  var vd = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                  if (vd.getVariables().size() == 1 || "a".equals(vd.getVariables().getFirst().getSimpleName())) {
                      return JavaTemplate.apply("String a();", getCursor(), vd.getCoordinates().replace());
                  }
                  return vd;
              }
          })),
          java(
            """
              interface Test {

                  String a;
              }
              """,
            """
              interface Test {

                  String a();
              }
              """
          )
        );
    }

    @Test
    void finalMethodParameter() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull", null)),
          java(
            """
              import org.jetbrains.annotations.NotNull;

              class A {
                  String testMethod(@NotNull final String test) {}
              }
              """,
            """
              import lombok.NonNull;

              class A {
                  String testMethod(@NonNull final String test) {}
              }
              """
          )
        );
    }

    @Test
    void replaceVariableDeclarationWithFinalVar() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
                  if (TypeUtils.isString(vd.getType()) && "String".equals(((J.Identifier) vd.getTypeExpression()).getSimpleName())) {
                      JavaCoordinates coordinates = vd.getCoordinates().replace();
                      return JavaTemplate.builder("final var #{}")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), coordinates, new Object[]{vd.getVariables().getFirst().getSimpleName()});
                  }
                  return vd;
              }
          })),
          java(
            """
              import java.util.List;
              import java.util.ArrayList;

              class A {
                  void bar(List<String> lst) {
                      for (String s : lst) {}
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.ArrayList;

              class A {
                  void bar(List<String> lst) {
                      for (final var s : lst) {}
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replacePackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Package visitPackage(J.Package pkg, ExecutionContext p) {
                  if ("a".equals(pkg.getExpression().printTrimmed(getCursor()))) {
                      return JavaTemplate.apply("b", getCursor(), pkg.getCoordinates().replace());
                  }
                  return super.visitPackage(pkg, p);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  var cd = super.visitClassDeclaration(classDecl, p);
                  if ("a".equals(classDecl.getType().getPackageName())) {
                      var type = cd.getType().withFullyQualifiedName("b." + cd.getSimpleName());
                      cd = cd.withType(type).withName(cd.getName().withType(type));
                  }
                  return cd;
              }
          })),
          java(
            """
              package a;
              class Test {
              }
              """,
            """
              package b;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void replaceMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if ("test".equals(method.getSimpleName())) {
                      return JavaTemplate.apply("int test2(int n) { return n; }", getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              var methodType = ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst()).getMethodType();
              assertThat(methodType.getReturnType()).isEqualTo(JavaType.Primitive.Int);
              assertThat(methodType.getParameterTypes()).containsExactly(JavaType.Primitive.Int);
          }),
          java(
            """
              class Test {
                  void test() {
                  }
              }
              """,
            """
              class Test {

                  int test2(int n) {
                      return n;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("int m, java.util.List<String> n").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if ("test".equals(method.getSimpleName()) && method.getParameters().size() == 1) {
                      // insert in outer method
                      J.MethodDeclaration m = t.apply(getCursor(), method.getCoordinates().replaceParameters());
                      var newRunnable = (J.NewClass) method.getBody().getStatements().getFirst();

                      // insert in inner method
                      var innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().getFirst();
                      return t.apply(updateCursor(m), innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              var m = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
              JavaType.Method type = m.getMethodType();
              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("m", "n");
              assertThat(type.getParameterTypes().getFirst())
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .matches(jt -> jt instanceof JavaType.Parameterized &&
                               "java.util.List".equals(((JavaType.Parameterized) jt).getType().getFullyQualifiedName()) &&
                               ((JavaType.Parameterized) jt).getTypeParameters().size() == 1 &&
                               "java.lang.String".equals(TypeUtils.asFullyQualified(((JavaType.Parameterized) jt).getTypeParameters().getFirst()).getFullyQualifiedName()),
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
                  if ("test".equals(method.getSimpleName()) && method.getParameters().getFirst() instanceof J.Empty) {
                      // insert in outer method
                      J.MethodDeclaration m = t.apply(getCursor(), method.getCoordinates().replaceParameters());
                      var newRunnable = (J.NewClass) method.getBody().getStatements().getFirst();

                      // insert in inner method
                      var innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().getFirst();
                      return t.apply(updateCursor(m), innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              final JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst())
                .getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("values");

              var param = TypeUtils.asArray(type.getParameterTypes().getFirst());
              assertThat(param.getElemType())
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                .matches(at -> "java.lang.Object"
                  .equals(TypeUtils.asFullyQualified(TypeUtils.asArray(at).getElemType()).getFullyQualifiedName()));
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
                      if ("test".equals(method.getSimpleName()) && method.getParameters().size() == 1) {
                          return JavaTemplate.apply("int n, #{}", getCursor(), method.getCoordinates().replaceParameters(), method.getParameters().getFirst());
                      }
                      return method;
                  }
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst()).getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("n", "s");
              assertThat(type.getParameterTypes().getFirst())
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .as("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches(jt -> "java.lang.String".equals(TypeUtils.asFullyQualified(jt).getFullyQualifiedName()));
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

    @Issue("https://github.com/openrewrite/rewrite/issues/9520")
    @ParameterizedTest
    @ValueSource(strings = {
      "interface Wrapper<T>",
      "class Wrapper<T>",
      "interface Wrapper",
      "class Wrapper",
      "interface Wrapper<T> extends java.util.function.Supplier<T>",
      "abstract class Wrapper<T> extends java.util.AbstractList<T> implements java.util.function.Supplier<T>",
      "interface Wrapper extends java.util.function.Supplier<String>",
      "abstract class Wrapper extends java.util.AbstractList<String> implements java.util.function.Supplier<String>"
    })
    void addImplementsClause(String declaration) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if (TypeUtils.isAssignableTo("java.io.Serializable", classDecl.getType())) {
                      return classDecl;
                  }
                  JavaType.FullyQualified original = classDecl.getType();
                  J.ClassDeclaration result = JavaTemplate.builder("java.io.Serializable")
                    .build()
                    .apply(getCursor(), classDecl.getCoordinates().addImplementsClause());
                  JavaType.FullyQualified updated = result.getType();
                  assertThat(updated.getFullyQualifiedName()).isEqualTo(original.getFullyQualifiedName());
                  assertThat(updated.getKind()).isEqualTo(original.getKind());
                  assertThat(updated.getSupertype()).isSameAs(original.getSupertype());
                  assertThat(updated.getTypeParameters()).containsExactlyElementsOf(original.getTypeParameters());
                  assertThat(updated.getInterfaces()).hasSize(original.getInterfaces().size() + 1);
                  assertThat(updated.getInterfaces().subList(0, original.getInterfaces().size()))
                    .containsExactlyElementsOf(original.getInterfaces());
                  assertThat(TypeUtils.isOfClassType(updated.getInterfaces().getLast(), "java.io.Serializable")).isTrue();
                  if (original instanceof JavaType.Parameterized) {
                      assertThat(updated).isInstanceOf(JavaType.Parameterized.class);
                  }
                  assertThat(result.getName()).isSameAs(classDecl.getName());
                  return result;
              }
          })),
          java(
            declaration + " {\n}",
            declaration + (declaration.contains(" extends ") || declaration.contains(" implements ") ? ", " :
              declaration.startsWith("interface") ? " extends " : " implements ") + "java.io.Serializable {\n}"
          )
        );
    }

    @Test
    void replaceClassImplements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getImplements() == null) {
                      maybeAddImport("java.io.Closeable");
                      maybeAddImport("java.io.Serializable");
                      return JavaTemplate.builder("Serializable, Closeable")
                        .imports("java.io.*")
                        .build()
                        .apply(getCursor(), classDecl.getCoordinates().replaceImplementsClause());
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
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getExtends() == null) {
                      maybeAddImport("java.util.List");
                      return JavaTemplate.builder("List<String>")
                        .imports("java.util.*")
                        .build()
                        .apply(getCursor(), classDecl.getCoordinates().replaceExtendsClause());
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
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getThrows() == null) {
                      return JavaTemplate.apply("Exception", getCursor(), method.getCoordinates().replaceThrows());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              var testMethodDecl = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
              assertThat(testMethodDecl.getMethodType().getThrownExceptions().stream()
                .map(JavaType.FullyQualified.class::cast)
                .map(JavaType.FullyQualified::getFullyQualifiedName))
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

    @Test
    void replaceParametersResolvesSimpleNamesThroughSourceFileImports() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getParameters().getFirst() instanceof J.Empty) {
                      return JavaTemplate.builder("List<String> names")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          }))
            // resolving List through the file's imports is what this tests
            .recipeQualityValidation(RecipeQualityValidation.builder().importReferencedTypes(false).build())
            .afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst()).getMethodType();
              assertThat(type).isNotNull();
              assertThat(type.getParameterTypes().getFirst())
                .as("The parameter's type should resolve through the source file's `java.util.List` import")
                .matches(t -> TypeUtils.isOfClassType(t, "java.util.List"));
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

                  void test(List<String> names) {
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("a type parameter added by replaceTypeParameters is not attributed in a later replaceParameters template")
    @Test
    void replaceMethodTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getTypeParameters() == null) {
                      J j = JavaTemplate.apply("T, U", getCursor(), method.getCoordinates().replaceTypeParameters());
                      return JavaTemplate.builder("List<T> t, U u")
                        .imports("java.util.List")
                        .build()
                        .apply(updateCursor(j), method.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst()).getMethodType();
              assertThat(type).isNotNull();
              var paramTypes = type.getParameterTypes();
              assertThat(paramTypes.getFirst())
                .as("The method declaration's type's genericSignature first argument should have type 'java.util.List'")
                .matches(tType -> tType instanceof JavaType.FullyQualified &&
                                  "java.util.List".equals(TypeUtils.asFullyQualified(tType).getFullyQualifiedName()));

              assertThat(paramTypes.get(1))
                .as("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                .matches(uType ->
                  uType instanceof JavaType.GenericTypeVariable &&
                  "U".equals(TypeUtils.asGeneric(uType).getName()) &&
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
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getTypeParameters() == null) {
                      return JavaTemplate.apply("T, U", getCursor(), classDecl.getCoordinates().replaceTypeParameters());
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
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().getFirst();
                  if (statement instanceof J.Unary) {
                      return JavaTemplate.builder("n = 1;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replaceBody());
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
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (!method.isAbstract()) {
                      return method;
                  }
                  J.MethodDeclaration m = JavaTemplate.<J.MethodDeclaration>apply("", getCursor(), method.getCoordinates().replaceBody())
                    .withModifiers(emptyList())
                    .withReturnTypeExpression(method.getReturnTypeExpression().withPrefix(Space.EMPTY));
                  JavaType.Method type = m.getMethodType();
                  if (type == null) {
                      return m;
                  }
                  Set<Flag> flags = EnumSet.copyOf(type.getFlags());
                  flags.remove(Flag.Abstract);
                  return m.withMethodType(type.withFlags(flags));
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
                  void test() {
                  }
              }
              """
          )
        );
    }
}
