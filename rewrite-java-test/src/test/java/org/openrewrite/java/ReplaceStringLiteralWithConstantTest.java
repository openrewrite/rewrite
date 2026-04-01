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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceStringLiteralWithConstantTest implements RewriteTest {

    public static String EXAMPLE_STRING_CONSTANT = "Hello World!";
    public static String EXAMPLE_STRING_FQN = ReplaceStringLiteralWithConstantTest.class.getName() + ".EXAMPLE_STRING_CONSTANT";

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true));
    }

    @DocumentExample
    @Test
    void shouldNotAddImportWhenUnnecessary() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;

              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              package org.openrewrite.java;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void doNothingIfStringLiteralNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;

              class Test {
                  String s = "FooBar";
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;

              class ReplaceStringLiteralWithConstantTest {
                  static final String EXAMPLE_STRING_CONSTANT = "Hello World!";
              }
              """
          ));
    }

    @Test
    void replaceStringLiteralWithConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceLiteralWithUserDefinedConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("newValue", EXAMPLE_STRING_FQN)),
          java(
            """
              package com.abc;

              class A {
                  String v = "newValue";
                  private String method() {
                      return "newValue";
                  }
              }
              """,
            """
              package com.abc;

              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class A {
                  String v = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
                  private String method() {
                      return ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsNotConfigured() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(null, EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceWhitespaceStringLiteralWhenLiteralValueIsConfiguredWhitespace() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(" ", EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = " ";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsNotConfiguredYaml() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.ReplaceStringLiteralWithConstantList
              description: Replace string literals with constants.
              recipeList:
                  - org.openrewrite.java.ReplaceStringLiteralWithConstant:
                      fullyQualifiedConstantName: %s
              """.formatted(EXAMPLE_STRING_FQN),
            "org.openrewrite.ReplaceStringLiteralWithConstantList"),
          java(
            """
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsConfiguredYaml() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.ReplaceStringLiteralWithConstantList
              description: Replace string literals with constants.
              recipeList:
                  - org.openrewrite.java.ReplaceStringLiteralWithConstant:
                      literalValue: %s
                      fullyQualifiedConstantName: %s
              """.formatted("Hello Darkness!", EXAMPLE_STRING_FQN),
            "org.openrewrite.ReplaceStringLiteralWithConstantList"),
          java(
            """
              class Test {
                  Object o = "Hello Darkness!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;

              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void missingFieldNoError() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaVisitor<>() {
              @Override
              public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                  // Circumvent validation to match use in rewrite-spring's ReplaceStringLiteralsWithMediaTypeConstants
                  doAfterVisit(new ReplaceStringLiteralWithConstant(null, EXAMPLE_STRING_FQN + "_xyz").getVisitor());
                  return super.visit(tree, ctx);
              }
          })),
          java(
            """
              package org.openrewrite.java;

              class Test {
                  Object o = "Hello World!";
              }
              """
          )
        );
    }

    @Test
    void switchCase() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;

              /** @noinspection ALL*/
              class Test {
                  void foo(String bar) {
                      int i = 0;
                      switch (bar) {
                          case "Hello World!":
                              i = 1;
                              break;
                          default:
                              i = 2;
                              break;
                      }
                  }
              }
              """,
            """
              package org.openrewrite.java;

              /** @noinspection ALL*/
              class Test {
                  void foo(String bar) {
                      int i = 0;
                      switch (bar) {
                          case ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT:
                              i = 1;
                              break;
                          default:
                              i = 2;
                              break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAnnotationValue() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_CONSTANT, EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;

              @interface Foo {
                  String bar();
                  String baz();
              }
              """
          ),
          java(
            """
              package org.openrewrite.java;

              class Test {
                  @Foo(bar = "Goodbye World!", baz = "Hello World!")
                  void foo(String bar) {
                  }
              }
              """,
            """
              package org.openrewrite.java;

              class Test {
                  @Foo(bar = "Goodbye World!", baz = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT)
                  void foo(String bar) {
                  }
              }
              """
          )
        );
    }
}
