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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "UnusedAssignment", "ResultOfMethodCallIgnored"})
class JavaTemplateMethodInvocationTest implements RewriteTest {

    @SuppressWarnings({"BigDecimalLegacyMethod", "deprecation"})
    @Test
    void javaTemplateControlsSemiColons() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher bigDecimalSetScale = new MethodMatcher("java.math.BigDecimal setScale(int, int)");
              final JavaTemplate twoArgScale = JavaTemplate.builder("#{any(int)}, #{}")
                .imports("java.math.RoundingMode").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  var mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                  if (bigDecimalSetScale.matches(mi)) {
                      mi = twoArgScale.apply(updateCursor(mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst(), "RoundingMode.HALF_UP");
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.math.BigDecimal;
              import java.math.RoundingMode;

              class A {
                  void m() {
                      StringBuilder sb = new StringBuilder();
                      sb.append((new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue())).append("|");
                  }
              }
              """,
            """
              import java.math.BigDecimal;
              import java.math.RoundingMode;

              class A {
                  void m() {
                      StringBuilder sb = new StringBuilder();
                      sb.append((new BigDecimal(0).setScale(1, RoundingMode.HALF_UP).doubleValue())).append("|");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodCallWithGenericParameterWithUnknownType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                private final JavaParser.Builder<?, ?> assertionsParser = JavaParser.fromJavaVersion()
                  .classpath("assertj-core");

                private static final MethodMatcher JUNIT_ASSERT_EQUALS = new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(..)");

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!JUNIT_ASSERT_EQUALS.matches(method)) {
                        return method;
                    }

                    List<Expression> args = method.getArguments();
                    Expression expected = args.getFirst();
                    Expression actual = args.get(1);

                    maybeRemoveImport("org.junit.jupiter.api.Assertions");
                    maybeAddImport("org.assertj.core.api.Assertions", "assertThat");

                    if (args.size() == 2) {
                        return JavaTemplate.builder("assertThat(#{any()}).isEqualTo(#{any()});")
                          .staticImports("org.assertj.core.api.Assertions.assertThat")
                          .javaParser(assertionsParser)
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace(), actual, expected);
                    } else {
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
            })),
          java(
            """
              import java.util.Map;
              import org.junit.jupiter.api.Assertions;

              class T {
                  void m(String one, Map<String, ?> map) {
                      Assertions.assertEquals(one, map.get("one"));
                  }
              }
              """,
            """
              import java.util.Map;

              import static org.assertj.core.api.Assertions.assertThat;

              class T {
                  void m(String one, Map<String, ?> map) {
                      assertThat(map.get("one")).isEqualTo(one);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceCallWithUnknownGenericReturnValue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              private static final MethodMatcher OBJECTS_EQUALS = new MethodMatcher("java.util.Objects equals(..)");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!OBJECTS_EQUALS.matches(method)) {
                      return method;
                  }

                  List<Expression> args = method.getArguments();
                  Expression expected = args.getFirst();
                  Expression actual = args.get(1);

                  maybeAddImport("java.util.Objects", "requireNonNull");

                  if (args.size() == 2) {
                      return JavaTemplate.builder("requireNonNull(#{any()}).equals(#{any()});")
                        .staticImports("java.util.Objects.requireNonNull")
                        .javaParser(JavaParser.fromJavaVersion())
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), actual, expected);
                  } else {
                      return super.visitMethodInvocation(method, ctx);
                  }
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.Map;
              import java.util.HashMap;

              class T {
              	void m() {
              		Map<String, ?> map = new HashMap<>();
              		Objects.equals("", map.get("one"));
              	}
              }
              """,
            """
              import java.util.Objects;
              import java.util.Map;

              import static java.util.Objects.requireNonNull;
              import java.util.HashMap;

              class T {
              	void m() {
              		Map<String, ?> map = new HashMap<>();
              \t\trequireNonNull(map.get("one")).equals("");
              	}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-spring/pull/284")
    @Test
    void replaceMethodInChainFollowedByGenericTypeParameters() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (new MethodMatcher("batch.StepBuilder create()").matches(method)) {
                        return JavaTemplate.apply("new StepBuilder()", getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            }))
            // Unclear why
            .recipeQualityValidation(RecipeQualityValidation.builder()
              .types(TypeValidation.builder().constructorInvocations(false).build())
              .build())
            .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package batch;
                  public class StepBuilder {
                      public static StepBuilder create() { return new StepBuilder(); }
                      public StepBuilder() {}
                      public <T> T method() { return null; }
                  }
                  """
              )
            ),
          java(
            """
              import batch.StepBuilder;
              class Foo {
                  void test() {
                      StepBuilder.create()
                          .<String>method();
                  }
              }
              """,
            """
              import batch.StepBuilder;
              class Foo {
                  void test() {
                      new StepBuilder()
                          .<String>method();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/185")
    @Test
    void replaceMethodArgumentsInIfStatementWithoutBraces() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Foo bar(..)").matches(mi) &&
                    mi.getArguments().getFirst() instanceof J.Binary) {
                      return JavaTemplate.apply("\"Hello, {}\", \"World!\"", new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments());
                  }
                  return mi;
              }
          })),
          java(
            """
              class Foo {
                  void foo(boolean condition) {
                      if (condition)
                          bar("Hello, " + "World!");
                  }
                  String bar(String... arg){ return null; }
              }
              """,
            """
              class Foo {
                  void foo(boolean condition) {
                      if (condition)
                          bar("Hello, {}", "World!");
                  }
                  String bar(String... arg){ return null; }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAnotherOverloadsArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Foo bar(String...)").matches(mi)) {
                      return JavaTemplate.apply("#{any(java.lang.String)}.length()", new Cursor(getCursor().getParent(), mi),
                        mi.getCoordinates().replaceArguments(), mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              class Foo {
                  void foo() {
                      bar("Hello");
                  }
                  void bar(String... arg) {}
                  void bar(int length) {}
              }
              """,
            """
              class Foo {
                  void foo() {
                      bar("Hello".length());
                  }
                  void bar(String... arg) {}
                  void bar(int length) {}
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAFixedArityOverloadsArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Loader using(Object...)").matches(mi) && mi.getArguments().size() == 2) {
                      maybeAddImport("java.util.Map");
                      return JavaTemplate.builder("Map.of(#{any()}, #{any()})")
                        .imports("java.util.Map")
                        .build()
                        .apply(new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                          mi.getArguments().toArray());
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.util.Map;

              interface Loader<T> {
                  Loader<T> using(Object... values);
                  Loader<T> using(Map<String, ?> values);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Loader<Integer> loader) {
                      loader.using("a", "x");
                      loader.using(1, 2);
                  }
              }
              """,
            """
              import java.util.Map;

              class Test {
                  void test(Loader<Integer> loader) {
                      loader.using(Map.of("a", "x"));
                      loader.using(Map.of(1, 2));
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithTheMostSpecificFixedArityOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log debug(String, Object...)").matches(mi) && mi.getArguments().size() == 3) {
                      return JavaTemplate.apply("\"failed\", #{any(java.lang.RuntimeException)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getLast());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void debug(String format, Object arg);
                  void debug(String message, Exception e);
                  void debug(String message, Throwable t);
                  void debug(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, RuntimeException e) {
                      log.debug("{} {}", "a", e);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, RuntimeException e) {
                      log.debug("failed", e);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnotherOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi)) {
                      return JavaTemplate.apply("\"{}\", #{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.info(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.info("{}", name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadTakingThemByBoxing() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("\"{}\", #{any(int)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, int count) {
                      log.info("none");
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, int count) {
                      log.info("{}", count);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAVarargsOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi)) {
                      return JavaTemplate.apply("\"{} {}\", #{any(java.lang.String)}, \"x\"",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
                  void info(Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.info(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.info("{} {}", name, "x");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAVarargsOverloadWhoseEmptyVarargsAreMoreSpecific() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Sink put(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().getFirst()
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  void put(int index);
                  void put(String key, Object... values);
                  void put(String key, Integer... values);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(String key, Sink sink) {
                      sink.put(0);
                  }
              }
              """,
            """
              class Test {
                  void test(String key, Sink sink) {
                      sink.put(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadTakingThemWithoutBoxing() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("* remove(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(0)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.Integer)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Bag {
                  void remove(int index);
                  void remove(Object o);
              }
              """
          ),
          java(
            """
              interface Sack {
                  void remove(Object o);
                  void remove(int index);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Integer i, Bag bag, Sack sack) {
                      bag.remove(0);
                      sack.remove(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Integer i, Bag bag, Sack sack) {
                      bag.remove(i);
                      sack.remove(i);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadReturningAnotherType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Names name(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Names {
                  String name(int index);
                  int name(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Names names, String key) {
                      names.name(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Names names, String key) {
                      names.name(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsOnARawReceiverKeepsTheErasedTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Box names(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.util.List;

              interface Box<T> {
                  List<String> names(int index);
                  List<String> names(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Box box, String key) {
                      box.names(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Box box, String key) {
                      box.names(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadThatNamesATypeVariable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if ((new MethodMatcher("Sink put(Object)").matches(mi) ||
                       new MethodMatcher("Sink take(int)").matches(mi)) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  <T extends CharSequence> void put(T value);
                  void put(Object value);
                  Object take(int index);
                  <T> T take(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.put(0);
                      sink.take(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.put(name);
                      sink.take(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAFixedArityOverloadBesideAGenericOne() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Sink add(Object[])").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  void add(Object... values);
                  void add(Object value);
                  <T extends Number> void add(T value);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.add(1, 2);
                  }
              }
              """,
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.add(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithAnOverloadTakingTheNewArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("IOUtils write(StringBuffer, java.io.OutputStream)").matches(mi)) {
                      maybeAddImport("java.nio.charset.StandardCharsets");
                      return JavaTemplate.builder("write(#{any(java.lang.StringBuffer)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})")
                        .imports("java.nio.charset.StandardCharsets")
                        .build()
                        .apply(new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                          mi.getArguments().get(0), mi.getArguments().get(1), "UTF_8");
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.io.OutputStream;
              import java.nio.charset.Charset;

              class IOUtils {
                  static void write(StringBuffer data, OutputStream output) {
                  }

                  static void write(CharSequence data, OutputStream output, Charset charset) {
                  }
              }
              """
          ),
          java(
            """
              import java.io.OutputStream;

              class Test {
                  void test(StringBuffer data, OutputStream output) {
                      IOUtils.write(data, output);
                  }
              }
              """,
            """
              import java.io.OutputStream;
              import java.nio.charset.StandardCharsets;

              class Test {
                  void test(StringBuffer data, OutputStream output) {
                      IOUtils.write(data, output, StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithARenamedMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log warning(String)").matches(mi)) {
                      return JavaTemplate.apply("warn(#{any(java.lang.String)})",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void warning(String message);
                  void warn(Object message);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.warning(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.warn(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithARenamedVarargsMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log warning(String[])").matches(mi)) {
                      return JavaTemplate.apply("warn(#{any(java.lang.String)})",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void warning(String... messages);
                  void warn(Object... messages);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.warning(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.warn(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodNameAndArgumentsSimultaneously() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if ("acceptInteger".equals(mi.getSimpleName())) {
                      mi = JavaTemplate.builder("acceptString(#{any()}.toString())")
                        .javaParser(JavaParser.fromJavaVersion())
                        .build()
                        .apply(updateCursor(mi), mi.getCoordinates().replaceMethod(), mi.getArguments().getFirst());
                      mi = mi.withName(mi.getName().withType(mi.getMethodType()));
                  }
                  return mi;
              }
          })),
          java(
            """
              package org.openrewrite;
              public class A {
                  public A acceptInteger(Number i) { return this; }
                  public A acceptString(String s) { return this; }
                  public A someOtherMethod() { return this; }
              }
              """,
                SourceSpec::skip
          ),
          java(
            """
              package org.openrewrite;

              public class Foo<N extends Number> {
                  void foo(N n) {
                      new A().someOtherMethod()
                              .acceptInteger(n)
                              .someOtherMethod();
                  }
              }
              """,
            """
              package org.openrewrite;

              public class Foo<N extends Number> {
                  void foo(N n) {
                      new A().someOtherMethod()
                              .acceptString(n.toString())
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
                  if ("method".equals(m.getSimpleName()) && m.getArguments().size() == 2) {
                      m = JavaTemplate.apply("#{anyArray(int)}", getCursor(), m.getCoordinates().replaceArguments(), m.getArguments().getFirst());
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
              """,
                SourceSpec::skip
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

    @Test
    void replaceMethodInvocationStatementArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, p);
                  if (m.getArguments().size() == 2) {
                      JavaCoordinates coordinates = m.getCoordinates().replaceArguments();
                      m = JavaTemplate.builder("#{any(java.lang.Integer)}")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), coordinates, m.getArguments().getFirst());
                  }
                  return m;
              }
          })),
          java(
            """
              import java.util.function.Predicate;

              class A {
                  public Predicate<String> foo() {
                      return new Predicate<String>() {
                          @Override public boolean test(String s) {
                              bar(1, 2);
                              return false;
                          }
                      };
                  }
                  public void bar(int... i) {}
              }
              """,
            """
              import java.util.function.Predicate;

              class A {
                  public Predicate<String> foo() {
                      return new Predicate<String>() {
                          @Override public boolean test(String s) {
                              bar(1);
                              return false;
                          }
                      };
                  }
                  public void bar(int... i) {}
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
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (method.getArguments().size() == 1) {
                      method = JavaTemplate.builder("m, Integer.valueOf(n), \"foo\"")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replaceArguments());
                      method = method.withName(method.getName().withType(method.getMethodType()));
                  }
                  return method;
              }
          })).afterRecipe(run -> {
              var cu = (J.CompilationUnit) run.getChangeset().getAllResults().getFirst().getAfter();
              var m = (J.MethodInvocation) ((J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().get(2)).getBody()
                .getStatements().getFirst();
              JavaType.Method type = m.getMethodType();
              assertThat(type.getParameterTypes().getFirst()).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1)).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(2)).matches(jt ->
                "java.lang.String".equals(TypeUtils.asFullyQualified(jt).getFullyQualifiedName()));
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

    @Issue("https://github.com/openrewrite/rewrite/issues/3787")
    @Test
    void changeMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher GET_BYTES = new MethodMatcher("java.lang.String getBytes()");
              final JavaTemplate WITH_ENCODING = JavaTemplate
                .builder("getBytes(StandardCharsets.#{})")
                .imports("java.nio.charset.StandardCharsets")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                  if (GET_BYTES.matches(method)) {
                      maybeAddImport("java.nio.charset.StandardCharsets");
                      m = WITH_ENCODING.apply(updateCursor(m), m.getCoordinates().replaceMethod(), "UTF_8");
                      assertThat(m.getName().getType()).isEqualTo(m.getMethodType());
                  }
                  return m;
              }
          })),
          java(
            """
              public class Test {
                  byte[] test() {
                      String s = "hello";
                      return s.getBytes();
                  }
              }
              """,
            """
              import java.nio.charset.StandardCharsets;

              public class Test {
                  byte[] test() {
                      String s = "hello";
                      return s.getBytes(StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }
}
