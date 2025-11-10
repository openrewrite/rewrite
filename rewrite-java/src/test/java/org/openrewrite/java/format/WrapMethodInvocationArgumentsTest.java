/*
 * Copyright 2025 the original author or authors.
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

class WrapMethodInvocationArgumentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrapMethodInvocationArguments<>(new WrappingAndBracesStyle(
          120,
          new WrappingAndBracesStyle.IfStatement(false),
          new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("builder", "newBuilder"), false),
          new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
          new WrappingAndBracesStyle.MethodCallArguments(WrapAlways, false, true, false),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap)))));
    }

    @DocumentExample
    @Test
    void formatMethodInvocationWithMultipleArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("hello", 42, true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              "hello",
              42,
              true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithTwoArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("name", 25);
                  }

                  void method(String name, int age) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              "name",
              25);
                  }

                  void method(String name, int age) {}
              }
              """
          )
        );
    }

    @Test
    void preserveAlreadyFormattedMethodCall() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method(
                              "hello",
                              42,
                              true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithComplexArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              import java.util.Arrays;

              class Test {
                  void test() {
                      method(Arrays.asList("a", "b"), 42 + 10, true && false);
                  }

                  void method(Object list, int value, boolean flag) {}
              }
              """,
            """
              package com.example;

              import java.util.Arrays;

              class Test {
                  void test() {
                      method(
              Arrays.asList(              
              "a",
              "b"),
              42 + 10,
              true && false);
                  }

                  void method(Object list, int value, boolean flag) {}
              }
              """
          )
        );
    }

    @Test
    void formatChainedMethodInvocations() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      String result = getString().substring(0, 5).toUpperCase();
                  }

                  String getString() {
                      return "hello";
                  }
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      String result = getString().substring(
              0,
              5).toUpperCase();
                  }

                  String getString() {
                      return "hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void formatConstructorInvocation() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Person {
                  Person(String name, int age, boolean active) {}
              }

              class Test {
                  void test() {
                      Person p = new Person("John", 30, true);
                  }
              }
              """,
            """
              package com.example;

              class Person {
                  Person(String name, int age, boolean active) {}
              }

              class Test {
                  void test() {
                      Person p = new Person(
              "John",
              30,
              true);
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStaticMethodInvocation() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      staticMethod("hello", 42, true);
                  }

                  static void staticMethod(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      staticMethod(
              "hello",
              42,
              true);
                  }

                  static void staticMethod(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithVarargs() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("prefix", 1, 2, 3);
                  }

                  void method(String prefix, int... values) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              "prefix",
              1,
              2,
              3);
                  }

                  void method(String prefix, int... values) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithComments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("hello", /* age */ 42, true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              "hello",
              /* age */ 42,
              true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void formatNestedMethodInvocations() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      outer(inner("value", 10), 42);
                  }

                  void outer(String s, int i) {}
                  String inner(String s, int i) { return s; }
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      outer(
              inner(
              "value",
              10),
              42);
                  }

                  void outer(String s, int i) {}
                  String inner(String s, int i) { return s; }
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithArrayArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method(new String[]{"a", "b"}, new int[]{1, 2});
                  }

                  void method(String[] names, int[] values) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              new String[]{"a", "b"},
              new int[]{1, 2});
                  }

                  void method(String[] names, int[] values) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithLambdaArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              import java.util.function.Function;

              class Test {
                  void test() {
                      method(x -> x.toString(), y -> y * 2);
                  }

                  void method(Function<Object, String> f1, Function<Integer, Integer> f2) {}
              }
              """,
            """
              package com.example;

              import java.util.function.Function;

              class Test {
                  void test() {
                      method(
              x -> x.toString(),
              y -> y * 2);
                  }

                  void method(Function<Object, String> f1, Function<Integer, Integer> f2) {}
              }
              """
          )
        );
    }

    @Test
    void doNotFormatMethodInvocationWithNoArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method();
                  }

                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void doNotFormatMethodInvocationWithSingleArgument() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("hello");
                  }

                  void method(String s) {}
              }
              """
          )
        );
    }

    @Test
    void formatMultipleMethodInvocationsInClass() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test1() {
                      method("hello", 42);
                  }

                  void test2() {
                      method("world", 100);
                  }

                  void method(String s, int i) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test1() {
                      method(
              "hello",
              42);
                  }

                  void test2() {
                      method(
              "world",
              100);
                  }

                  void method(String s, int i) {}
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
            65,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, emptyList(), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
            new WrappingAndBracesStyle.MethodCallArguments(ChopIfTooLong, false, false, true),
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
                  void test() {
                      shortMethod("a", 1);
                      veryLongMethodNameThatExceedsTheLimits("hello", 42, true);
                  }

                  void shortMethod(String s, int i) {}
                  void veryLongMethodNameThatExceedsTheLimits(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      shortMethod("a", 1);
                      veryLongMethodNameThatExceedsTheLimits("hello",
              42,
              true
              );
                  }

                  void shortMethod(String s, int i) {}
                  void veryLongMethodNameThatExceedsTheLimits(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void preserveMethodCallWithLengthBelowThreshold() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, emptyList(), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
            new WrappingAndBracesStyle.MethodCallArguments(ChopIfTooLong, false, false, false),
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
                  void test() {
                      method("name", 25);
                  }

                  void method(String name, int age) {}
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
            new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
            new WrappingAndBracesStyle.MethodCallArguments(WrapAlways, false, false, true),
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
                  void test() {
                      method("hello", 42, true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method("hello",
              42,
              true
              );
                  }

                  void method(String s, int i, boolean b) {}
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
                  void test() {
                      method(
                          "hello",
                          42);
                  }

                  void method(String s, int i) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodCallWithPartialNewlines() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Test {
                  void test() {
                      method("hello",
                          42, true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """,
            """
              package com.example;

              class Test {
                  void test() {
                      method(
              "hello",
                          42,
              true);
                  }

                  void method(String s, int i, boolean b) {}
              }
              """
          )
        );
    }

    @Test
    void formatMethodInvocationWithGenericTypeArguments() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              import java.util.List;

              class Test {
                  void test() {
                      this.<String>method("hello", 42);
                  }

                  <T> void method(T value, int count) {}
              }
              """,
            """
              package com.example;

              import java.util.List;

              class Test {
                  void test() {
                      this.<String>method(
              "hello",
              42);
                  }

                  <T> void method(T value, int count) {}
              }
              """
          )
        );
    }

    @Test
    void formatSuperMethodInvocation() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              class Base {
                  void method(String s, int i) {}
              }

              class Test extends Base {
                  void test() {
                      super.method("hello", 42);
                  }
              }
              """,
            """
              package com.example;

              class Base {
                  void method(String s, int i) {}
              }

              class Test extends Base {
                  void test() {
                      super.method(
              "hello",
              42);
                  }
              }
              """
          )
        );
    }

    @Test
    void formatMethodReferenceAsArgument() {
        //language=java
        rewriteRun(
          java(
            """
              package com.example;

              import java.util.function.Function;

              class Test {
                  void test() {
                      method(String::valueOf, Integer::parseInt);
                  }

                  void method(Function<Integer, String> f1, Function<String, Integer> f2) {}
              }
              """,
            """
              package com.example;

              import java.util.function.Function;

              class Test {
                  void test() {
                      method(
              String::valueOf,
              Integer::parseInt);
                  }

                  void method(Function<Integer, String> f1, Function<String, Integer> f2) {}
              }
              """
          )
        );
    }
}