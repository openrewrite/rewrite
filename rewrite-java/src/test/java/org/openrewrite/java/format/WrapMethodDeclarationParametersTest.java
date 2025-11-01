/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.style.LineWrapSetting.*;
import static org.openrewrite.test.RewriteTest.toRecipe;

class WrapMethodDeclarationParametersTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
          120,
          new WrappingAndBracesStyle.IfStatement(false),
          new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("builder", "newBuilder"), false),
          new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, true, false),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap)))));
    }

    @DocumentExample
    @Test
    void formatMethodWithMultipleParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              String name,
              int age,
              boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatInterfaceMethodWithMultipleParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              interface Test {
                  void method(String name, int age, boolean active);
              }
              """,
            """
              package com.example;
              
              interface Test {
                  void method(
              String name,
              int age,
              boolean active);
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithTwoParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, int age) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              String name,
              int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveAlreadyFormattedMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(
                          String name,
                          int age,
                          boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithGenericTypes() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.Map;
              
              class Test {
                  void method(List<String> names, Map<String, Integer> ages) {
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.Map;
              
              class Test {
                  void method(
              List<String> names,
              Map<String, Integer> ages) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithArrayParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(String[] names, int[] ages, boolean[][] flags) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              String[] names,
              int[] ages,
              boolean[][] flags) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithVarargs() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, int... values) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              String name,
              int... values) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatConstructor() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  Test(String name, int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  Test(
              String name,
              int age,
              boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithAnnotatedParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              import org.jspecify.annotations.Nullable;
              
              class Test {
                  void method(@Nullable String name, int age) {
                  }
              }
              """,
            """
              package com.example;
              
              import org.jspecify.annotations.Nullable;
              
              class Test {
                  void method(
              @Nullable String name,
              int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithParameterComments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, /* age parameter */ int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              String name,
              /* age parameter */ int age,
              boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithReturnType() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  String method(String name, int age) {
                      return name;
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  String method(
              String name,
              int age) {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithThrowsClause() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.io.IOException;
              
              class Test {
                  void method(String name, int age) throws IOException {
                  }
              }
              """,
            """
              package com.example;
              
              import java.io.IOException;
              
              class Test {
                  void method(
              String name,
              int age) throws IOException {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatPublicMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  public void method(String name, int age) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  public void method(
              String name,
              int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStaticMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  static void method(String name, int age) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  static void method(
              String name,
              int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatAbstractMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              abstract class Test {
                  abstract void method(String name, int age);
              }
              """,
            """
              package com.example;
              
              abstract class Test {
                  abstract void method(
              String name,
              int age);
              }
              """
          )
        );
    }

    @Test
    void formatInterfaceMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              interface Test {
                  void method(String name, int age);
              }
              """,
            """
              package com.example;
              
              interface Test {
                  void method(
              String name,
              int age);
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithComplexGenerics() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.Map;
              import java.util.function.Function;
              
              class Test {
                  <T, R> Map<T, List<R>> method(List<T> input, Function<T, List<R>> mapper) {
                      return null;
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.Map;
              import java.util.function.Function;
              
              class Test {
                  <T, R> Map<T, List<R>> method(
              List<T> input,
              Function<T, List<R>> mapper) {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotFormatMethodWithNoParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMultipleMethodsInClass() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method1(String name, int age) {
                  }
              
                  void method2(boolean active, double value) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method1(
              String name,
              int age) {
                  }
              
                  void method2(
              boolean active,
              double value) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodInInnerClass() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  static class Inner {
                      void method(String name, int age) {
                      }
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  static class Inner {
                      void method(
              String name,
              int age) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithFinalParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(final String name, final int age) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(
              final String name,
              final int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithMixedParameterTypes() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.util.List;
              
              class Test {
                  void method(String name, int age, List<String> items, boolean active, double[] values) {
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              
              class Test {
                  void method(
              String name,
              int age,
              List<String> items,
              boolean active,
              double[] values) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatLongLinesOnly() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            80,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, emptyList(), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(ChopIfTooLong, false, true, false),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap))))),
          java(
            """
              package com.example;
              
              class Test {
                  void shortMethod(String n, int a) {
                  }
              
                  void veryLongMethodNameThatExceedsTheLimit(String name, int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void shortMethod(String n, int a) {
                  }
              
                  void veryLongMethodNameThatExceedsTheLimit(
              String name,
              int age,
              boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveMethodWithMaxLengthBelowThreshold() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, emptyList(), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(ChopIfTooLong, false, false, false),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap))))),
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void openCloseNewLine() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, emptyList(), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, true),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(WrapAlways),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap),
            new WrappingAndBracesStyle.Annotations(DoNotWrap))))),
          java(
            """
              package com.example;
              
              class Test {
                  void method(String name, int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void method(String name,
              int age,
              boolean active
              ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotFormatArgumentsOnTheirOwnNewLineAlready() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void method(
                      String name,
                      int age) {
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodWithPartialNewlines() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void method(String name,
                      int age, boolean active) {
                  }
              }
              """,
            """
              package com.example;

              class Test {
                  void method(
              String name,
                      int age,
              boolean active) {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotWrapSingleParameterMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void method(String name) {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotWrapNoParameterMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void method() {
                  }
              }
              """
          )
        );
    }
}
