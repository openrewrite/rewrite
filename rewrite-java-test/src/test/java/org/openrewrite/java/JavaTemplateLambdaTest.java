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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "UnusedAssignment", "ResultOfMethodCallIgnored"})
class JavaTemplateLambdaTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/66")
    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    void lambdaIsNewClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                  var a = assignment;
                  if (a.getAssignment() instanceof J.MethodInvocation) {
                      var mi = (J.MethodInvocation) a.getAssignment();
                      a = JavaTemplate.apply("1", getCursor(), mi.getCoordinates().replace());
                  }
                  return a;
              }
          })),
          java(
            """
              class T {
                  public T (int a, Runnable r, String s) { }
                  static void method() {
                      new T(1,() -> {
                          int i;
                          i = Integer.valueOf(1);
                      }, "hello" );
                  }
              }
              """,
            """
              class T {
                  public T (int a, Runnable r, String s) { }
                  static void method() {
                      new T(1,() -> {
                          int i;
                          i = 1;
                      }, "hello" );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2540")
    @Test
    void replaceMemberReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                  return JavaTemplate.builder("() -> new ArrayList<>(1)")
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build()
                    .apply(getCursor(), memberRef.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.ArrayList;
              import java.util.function.Supplier;
              class Test {
                  void consumer(Supplier<?> supplier) {}
                  void test(int i) {
                      consumer(ArrayList::new);
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.function.Supplier;
              class Test {
                  void consumer(Supplier<?> supplier) {}
                  void test(int i) {
                      consumer(() -> new ArrayList<>(1));
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5289")
    @Test
    void recursiveType() {
        rewriteRun(
          // a context-sensitive template types a lambda argument by a placeholder method's parameter
          spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build())
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                    if (!(lambda.getBody() instanceof J.Block)) {
                        return lambda;
                    }
                    var param = (J.VariableDeclarations) lambda.getParameters().getParameters().getFirst();
                    J.VariableDeclarations.NamedVariable variable = param.getVariables().getFirst();

                    return JavaTemplate.builder("reference -> System.out.println(#{any()})")
                      .contextSensitive()
                      .build()
                      .apply(getCursor(), lambda.getCoordinates().replace(), variable.getName());
                }
            })),
          java(
            """
              import java.util.Optional;

              class BugTest {
                  void run(One<?, ?> firstBuild) {
                      Optional.of(firstBuild).ifPresent(reference -> {});
                  }

                  abstract static class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
                  abstract static class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              }
              """,
            """
              import java.util.Optional;

              class BugTest {
                  void run(One<?, ?> firstBuild) {
                      Optional.of(firstBuild).ifPresent(reference -> System.out.println(reference));
                  }

                  abstract static class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
                  abstract static class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              }
              """
          )
        );
    }

    @Test
    void replaceExpressionLambdaBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                  if (!(lambda.getBody() instanceof J.Literal) ||
                      !Boolean.TRUE.equals(((J.Literal) lambda.getBody()).getValue())) {
                      return super.visitLambda(lambda, ctx);
                  }
                  J.Literal body = (J.Literal) lambda.getBody();
                  return JavaTemplate.builder("Boolean.FALSE")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), body.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> true;
              }
              """,
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> Boolean.FALSE;
              }
              """
          )
        );
    }

    @Test
    void replaceStatementInsideLambdaBlockBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                  if (!(lambda.getBody() instanceof J.Block)) {
                      return super.visitLambda(lambda, ctx);
                  }
                  J.Block block = (J.Block) lambda.getBody();
                  if (block.getStatements().size() != 1 ||
                      !(block.getStatements().get(0) instanceof J.Return) ||
                      !(((J.Return) block.getStatements().get(0)).getExpression() instanceof J.Literal) ||
                      !Boolean.TRUE.equals(((J.Literal) ((J.Return) block.getStatements().get(0)).getExpression()).getValue())) {
                      return super.visitLambda(lambda, ctx);
                  }
                  return JavaTemplate.builder("return Boolean.FALSE;")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), block.getStatements().get(0).getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> {
                      return true;
                  };
              }
              """,
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> {
                      return Boolean.FALSE;
                  };
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CodeBlock2Expr"})
    @Test
    void replaceStatementInLambdaBodySingleStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("return n == 1;")
                .contextSensitive()
                .build();

              @Override
              public J visitReturn(J.Return return_, ExecutionContext p) {
                  if (return_.getExpression() instanceof J.Binary) {
                      var binary = (J.Binary) return_.getExpression();
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    @Test
    void replaceStatementInLambdaBodyWithVariableDeclaredInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("return n == 1;")
                .contextSensitive()
                .build();

              @Override
              public J visitReturn(J.Return return_, ExecutionContext p) {
                  if (return_.getExpression() instanceof J.Binary) {
                      var binary = (J.Binary) return_.getExpression();
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @Test
    void replaceStatementInLambdaBodyMultiStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("#{any(java.lang.String)}.toUpperCase()").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if ("toLowerCase".equals(method.getSimpleName())) {
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "SizeReplaceableByIsEmpty"})
    @Test
    void replaceSingleExpressionInLambdaBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if ("toLowerCase".equals(method.getSimpleName())) {
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
                      return t.apply(getCursor(), method.getCoordinates().replace(), method.getSelect(), method.getArguments().getFirst());
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @SuppressWarnings({
      "CachedNumberConstructorCall",
      "Convert2MethodRef"
      , "removal"})
    @Test
    void lambdaIsVariableInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher matcher = new MethodMatcher("Integer valueOf(..)");

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (matcher.matches(method)) {
                      return JavaTemplate.apply("new Integer(#{any()})", getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.function.Function;
              class Test {
                  Function<String, Integer> asInteger = it -> Integer.valueOf(it);
              }
              """,
            """
              import java.util.function.Function;
              class Test {
                  Function<String, Integer> asInteger = it -> new Integer(it);
              }
              """
          )
        );
    }
}
