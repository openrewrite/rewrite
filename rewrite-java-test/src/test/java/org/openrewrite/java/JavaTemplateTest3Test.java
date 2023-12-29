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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnusedAssignment"})
class JavaTemplateTest3Test implements RewriteTest {

    @DocumentExample
    @Test
    void replacePackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Package visitPackage(J.Package pkg, ExecutionContext p) {
                  if (pkg.getExpression().printTrimmed(getCursor()).equals("a")) {
                      return JavaTemplate.builder("b").contextSensitive().build()
                        .apply(getCursor(), pkg.getCoordinates().replace());
                  }
                  return super.visitPackage(pkg, p);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  var cd = super.visitClassDeclaration(classDecl, p);
                  if (classDecl.getType().getPackageName().equals("a")) {
                      cd = cd.withType(cd.getType().withFullyQualifiedName("b.${cd.getSimpleName()}"));
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
                  if (method.getSimpleName().equals("test")) {
                      return JavaTemplate.apply("int test2(int n) { return n; }", getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getChangeset().getAllResults().get(0).getAfter();
              var methodType = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
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

    @SuppressWarnings("Convert2MethodRef")
    @Test
    void replaceLambdaWithMethodReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext p) {
                  return JavaTemplate.builder("Object::toString")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), lambda.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = it -> it.toString();
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = Object::toString;
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CodeBlock2Expr"})
    void replaceStatementInLambdaBodySingleStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("return n == 1;").contextSensitive().build();

              @Override
              public J visitReturn(J.Return return_, ExecutionContext p) {
                  if (return_.getExpression() instanceof J.Binary) {
                      J.Binary binary = (J.Binary) return_.getExpression();
                      if (binary.getRight() instanceof J.Literal &&
                          ((J.Literal) binary.getRight()).getValue().equals(0)) {
                          return t.apply(getCursor(), return_.getCoordinates().replace());
                      }
                  }
                  return return_;
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  int n;

                  void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          return n == 0;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  int n;

                  void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          return n == 1;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    void replaceStatementInLambdaBodyWithVariableDeclaredInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("return n == 1;").contextSensitive().build();

              @Override
              public J visitReturn(J.Return return_, ExecutionContext p) {
                  if (return_.getExpression() instanceof J.Binary) {
                      J.Binary binary = (J.Binary) return_.getExpression();
                      if (binary.getRight() instanceof J.Literal && ((J.Literal) binary.getRight()).getValue().equals(0)) {
                          return t.apply(getCursor(), return_.getCoordinates().replace());
                      }
                  }
                  return return_;
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          int n = 0;
                          return n == 0;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          int n = 0;
                          return n == 1;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    void replaceStatementInLambdaBodyMultiStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("#{any(java.lang.String)}.toUpperCase()").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getSimpleName().equals("toLowerCase")) {
                      return t.apply(getCursor(), method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.map(o -> {
                          String str = o;
                          str = o.toLowerCase();
                          return str;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.map(o -> {
                          String str = o;
                          str = o.toUpperCase();
                          return str;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "SizeReplaceableByIsEmpty"})
    void replaceSingleExpressionInLambdaBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getSimpleName().equals("toLowerCase")) {
                      return JavaTemplate.apply("#{any(java.lang.String)}.toUpperCase()", getCursor(),
                        method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.filter(o -> o.toLowerCase().length() > 0);
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.filter(o -> o.toUpperCase().length() > 0);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2176")
    @Test
    void replaceSingleExpressionInLambdaBodyWithExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher enumEquals = new MethodMatcher("java.lang.Enum equals(java.lang.Object)");
              final JavaTemplate t = JavaTemplate.builder("#{any()} == #{any()}").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (enumEquals.matches(method)) {
                      return t.apply(getCursor(), method.getCoordinates().replace(), method.getSelect(), method.getArguments().get(0));
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  enum Abc {A,B,C}
                  static void method(Stream<Abc> obj) {
                      Object a = obj.filter(o -> o.equals(Abc.A));
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  enum Abc {A,B,C}
                  static void method(Stream<Abc> obj) {
                      Object a = obj.filter(o -> o == Abc.A);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ClassInitializerMayBeStatic")
    @Test
    void replaceMethodNameAndArgumentsSimultaneously() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (mi.getSimpleName().equals("acceptInteger")) {
                      mi = JavaTemplate.builder("acceptString(#{any()}.toString())").contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion()
                          .dependsOn(
                            """
                                  package org.openrewrite;
                                  public class A {
                                      public A acceptInteger(Integer i) { return this; }
                                      public A acceptString(String s) { return this; }
                                      public A someOtherMethod() { return this; }
                                  }
                              """
                          )
                        )
                        .build()
                        .apply(updateCursor(mi), mi.getCoordinates().replaceMethod(), mi.getArguments().get(0));
                      mi = mi.withName(mi.getName().withType(mi.getMethodType()));
                  }
                  return mi;
              }
          })),
          java(
            """
              package org.openrewrite;
              public class A {
                  public A acceptInteger(Integer i) { return this; }
                  public A acceptString(String s) { return this; }
                  public A someOtherMethod() { return this; }
              }
              """
          ),
          java(
            """
              package org.openrewrite;
                            
              public class Foo {
                  {
                      Integer i = 1;
                      new A().someOtherMethod()
                              .acceptInteger(i)
                              .someOtherMethod();
                  }
              }
              """,
            """
              package org.openrewrite;
                            
              public class Foo {
                  {
                      Integer i = 1;
                      new A().someOtherMethod()
                              .acceptString(i.toString())
                              .someOtherMethod();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodInvocationWithArray() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, p);
                  if (m.getSimpleName().equals("method") && m.getArguments().size() == 2) {
                      m = JavaTemplate.apply("#{anyArray(int)}", getCursor(), m.getCoordinates().replaceArguments(), m.getArguments().get(0));
                  }
                  return m;
              }
          })),
          java(
            """
              package org.openrewrite;
              public class Test {
                  public void method(int[] val) {}
                  public void method(int[] val1, String val2) {}
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              class A {
                  public void method() {
                      Test test = new Test();
                      int[] arr = new int[]{};
                      test.method(arr, null);
                  }
              }
              """,
            """
              import org.openrewrite.Test;
              class A {
                  public void method() {
                      Test test = new Test();
                      int[] arr = new int[]{};
                      test.method(arr);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/602")
    @Test
    void replaceMethodInvocationWithMethodReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  return JavaTemplate.builder("Object::toString")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = getToString();
                  
                  static Function<Object, String> getToString() {
                      return Object::toString;
                  }
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = Object::toString;
                  
                  static Function<Object, String> getToString() {
                      return Object::toString;
                  }
              }
              """
          )
        );
    }
}
