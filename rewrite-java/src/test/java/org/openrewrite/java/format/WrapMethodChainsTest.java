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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.style.LineWrapSetting.*;
import static org.openrewrite.test.RewriteTest.toRecipe;

class WrapMethodChainsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().dependsOn("""
            package com.example;
            
            public class MyObject {
                public static Builder builder() { return new Builder(); }
                public static Builder newBuilder() { return new Builder(); }
                public static class Builder {
                    Builder name(String n) { return this; }
                    Builder age(int a) { return this; }
                    Builder items(java.util.List<String> items) { return this; }
                    Builder nested(MyObject nested) { return this; }
                    MyObject build() { return new MyObject(); }
                }
            }
            """))
          .recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(
            new WrappingAndBracesStyle(
              120,
              new WrappingAndBracesStyle.IfStatement(false),
              new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("builder", "newBuilder"), false),
              new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
              new WrappingAndBracesStyle.Annotations(WrapAlways),
              new WrappingAndBracesStyle.Annotations(WrapAlways),
              new WrappingAndBracesStyle.Annotations(WrapAlways),
              new WrappingAndBracesStyle.Annotations(DoNotWrap),
              new WrappingAndBracesStyle.Annotations(DoNotWrap),
              new WrappingAndBracesStyle.Annotations(DoNotWrap))
          )));
    }

    @Test
    void formatBuilderMethod() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().name("test").age(25).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .name("test")
              .age(25)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatNewBuilderMethod() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.newBuilder().name("test").age(25).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.newBuilder()
              .name("test")
              .age(25)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveAlreadyFormattedBuilder() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
                              .name("test")
                              .age(25)
                              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatAlreadyNewlinedBuilder() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
                          .name("test")
                          .age(25)
                          .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void reindentIncorrectlyIndented() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
                                             .name("test")
                                             .age(25)
                                             .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatNestedBuilders() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().name("test").nested(MyObject.builder().name("nested").build()).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .name("test")
              .nested(MyObject.builder()
              .name("nested")
              .build())
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatBuilderInFieldDeclaration() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  private final MyObject value = MyObject.builder().name("hello").age(30).build();
              }
              """,
            """
              package com.example;
              
              class Test {
                  private final MyObject value = MyObject.builder()
              .name("hello")
              .age(30)
              .build();
              }
              """
          )
        );
    }

    @Test
    void formatBuilderInReturn() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  MyObject test() {
                      return MyObject.builder().name("hello").age(30).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  MyObject test() {
                      return MyObject.builder()
              .name("hello")
              .age(30)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatSingleMethodCallAfterBuilder() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatBuilderWithComments() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().name("hello") /* comment */ .age(30).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .name("hello") /* comment */
              .age(30)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatBuilderInLambda() {
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.util.function.Supplier;
              
              class Test {
                  void test() {
                      Supplier<MyObject> supplier = () -> MyObject.builder().name("hello").age(30).build();
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.function.Supplier;
              
              class Test {
                  void test() {
                      Supplier<MyObject> supplier = () -> MyObject.builder()
              .name("hello")
              .age(30)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoFormatNonBuilderChainedCalls() {
        rewriteRun(
          java(
            """
              package com.example;
             
             class Test {
                 void test() {
                     String result = "hello".toUpperCase()
             .substring(1)
             .trim();
                     String sb = new StringBuilder().append("a")
             .append("b")
             .toString();
                 }
             }
              """
          )
        );
    }

    @Test
    void formatMultipleBuilderChainsInMethod() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject s1 = MyObject.builder().name("a").age(1).build();
                      MyObject s2 = MyObject.newBuilder().name("b").age(2).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject s1 = MyObject.builder()
              .name("a")
              .age(1)
              .build();
                      MyObject s2 = MyObject.newBuilder()
              .name("b")
              .age(2)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatBuilderWithMethodArguments() {
        rewriteRun(
          java(
            """
              package com.example;
              
              import java.util.Arrays;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().name("test").items(Arrays.asList("a", "b", "c")).build();
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.Arrays;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .name("test")
              .items(Arrays.asList("a", "b", "c"))
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatNestedBuilderImmediatelyCallingBuild() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder().name("test").nested(MyObject.builder().build()).age(30).build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject obj = MyObject.builder()
              .name("test")
              .nested(MyObject.builder()
              .build())
              .age(30)
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamChain() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  void test(List<String> list) {
                      List<String> result = list.stream().filter(s -> s.length() > 3)
                      .map(String::toUpperCase).sorted()
                      .collect(Collectors.toList());
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  void test(List<String> list) {
                      List<String> result = list.stream()
              .filter(s -> s.length() > 3)
                      .map(String::toUpperCase)
              .sorted()
                      .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void formatDeeplyNestedBuilders() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject root = MyObject.builder().name("root").nested(
                          MyObject.builder().name("level1").nested(
                              MyObject.builder().name("level2").nested(
                                  MyObject.builder().name("level3").nested(
                                      MyObject.builder().name("level4")
                                      .build())
                                  .build())
                              .build())
                          .build())
                      .build();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      MyObject root = MyObject.builder()
              .name("root")
              .nested(MyObject.builder()
              .name("level1")
              .nested(MyObject.builder()
              .name("level2")
              .nested(MyObject.builder()
              .name("level3")
              .nested(MyObject.builder()
              .name("level4")
                                      .build())
                                  .build())
                              .build())
                          .build())
                      .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithMultilineFilterLambda() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.Collection;
              import java.util.Optional;
              
              class Test {
                  Optional<Item> findItem(Collection<Item> collection) {
                      return collection.stream().filter(item -> {
                          if (someCondition(item)) {
                              return true;
                          } else if (otherCondition(item)) {
                              return true;
                          }
                          return false;
                      }).findFirst();
                  }
              
                  boolean someCondition(Item item) { return true; }
                  boolean otherCondition(Item item) { return false; }
              
                  static class Item {}
              }
              """,
            """
              package com.example;
              
              import java.util.Collection;
              import java.util.Optional;
              
              class Test {
                  Optional<Item> findItem(Collection<Item> collection) {
                      return collection.stream()
              .filter(item -> {
                          if (someCondition(item)) {
                              return true;
                          } else if (otherCondition(item)) {
                              return true;
                          }
                          return false;
                      })
              .findFirst();
                  }
              
                  boolean someCondition(Item item) {
              return true;
              }
                  boolean otherCondition(Item item) {
              return false;
              }
              
                  static class Item {
              }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithMultipleMultilineLambdas() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<Item> items) {
                      return items.stream().filter(item -> {
                          boolean valid = item.isValid();
                          if (valid) {
                              System.out.println("Valid: " + item);
                          }
                          return valid;
                      }).map(item -> {
                          String result = item.toString();
                          System.out.println("Mapping: " + result);
                          return result.toUpperCase();
                      }).collect(Collectors.toList());
                  }
              
                  static class Item {
                      boolean isValid() { return true; }
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<Item> items) {
                      return items.stream()
              .filter(item -> {
                          boolean valid = item.isValid();
                          if (valid) {
                              System.out.println("Valid: " + item);
                          }
                          return valid;
                      })
              .map(item -> {
                          String result = item.toString();
                          System.out.println("Mapping: " + result);
                          return result.toUpperCase();
                      })
              .collect(Collectors.toList());
                  }
              
                  static class Item {
                      boolean isValid() {
              return true;
              }
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithMixedLambdaStyles() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<Integer> process(List<String> items) {
                      return items.stream().filter(s -> s.length() > 3).map(s -> {
                          try {
                              return Integer.parseInt(s);
                          } catch (NumberFormatException e) {
                              return 0;
                          }
                      }).filter(i -> i > 0).sorted().collect(Collectors.toList());
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<Integer> process(List<String> items) {
                      return items.stream()
              .filter(s -> s.length() > 3)
              .map(s -> {
                          try {
                              return Integer.parseInt(s);
                          } catch (NumberFormatException e) {
                              return 0;
                          }
                      })
              .filter(i -> i > 0)
              .sorted()
              .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithComplexNestedLambda() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<Department> departments) {
                      return departments.stream().flatMap(dept -> {
                          System.out.println("Processing department: " + dept.name);
                          return dept.employees.stream()
                              .filter(emp -> emp.active)
                              .map(emp -> dept.name + ": " + emp.name);
                      }).sorted().distinct().collect(Collectors.toList());
                  }
              
                  static class Department {
                      String name;
                      List<Employee> employees;
                  }
              
                  static class Employee {
                      String name;
                      boolean active;
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<Department> departments) {
                      return departments.stream()
              .flatMap(dept -> {
                          System.out.println("Processing department: " + dept.name);
                          return dept.employees.stream()
                              .filter(emp -> emp.active)
                              .map(emp -> dept.name + ": " + emp.name);
                      })
              .sorted()
              .distinct()
              .collect(Collectors.toList());
                  }
              
                  static class Department {
                      String name;
                      List<Employee> employees;
                  }
              
                  static class Employee {
                      String name;
                      boolean active;
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithMethodReferencesAndLambdas() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<String> items) {
                      return items.stream().filter(this::isValid).map(s -> {
                          String processed = preprocess(s);
                          return processed.toUpperCase();
                      }).sorted(String::compareTo).collect(Collectors.toList());
                  }
              
                  boolean isValid(String s) { return s != null && !s.isEmpty(); }
                  String preprocess(String s) { return s.trim(); }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<String> items) {
                      return items.stream()
              .filter(this::isValid)
              .map(s -> {
                          String processed = preprocess(s);
                          return processed.toUpperCase();
                      })
              .sorted(String::compareTo)
              .collect(Collectors.toList());
                  }
              
                  boolean isValid(String s) {
              return s != null && !s.isEmpty();
              }
                  String preprocess(String s) {
              return s.trim();
              }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithPeekAndMultilineLambda() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<String> items) {
                      return items.stream().peek(item -> {
                          System.out.println("Before: " + item);
                          if (item.length() > 10) {
                              System.out.println("Long item detected");
                          }
                      }).map(String::toUpperCase).peek(System.out::println).collect(Collectors.toList());
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  List<String> process(List<String> items) {
                      return items.stream()
              .peek(item -> {
                          System.out.println("Before: " + item);
                          if (item.length() > 10) {
                              System.out.println("Long item detected");
                          }
                      })
              .map(String::toUpperCase)
              .peek(System.out::println)
              .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveAlreadyFormattedStreamWithMultilineLambda() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.Collection;
              import java.util.Optional;
              
              class Test {
                  Optional<Item> findItem(Collection<Item> collection) {
                      return collection.stream()
                              .filter(item -> {
                                  if (someCondition(item)) {
                                      return true;
                                  } else if (otherCondition(item)) {
                                      return true;
                                  }
                                  return false;
                              })
                              .findFirst();
                  }
              
                  boolean someCondition(Item item) { return true; }
                  boolean otherCondition(Item item) { return false; }
              
                  static class Item {}
              }
              """,
            """
              package com.example;
              
              import java.util.Collection;
              import java.util.Optional;
              
              class Test {
                  Optional<Item> findItem(Collection<Item> collection) {
                      return collection.stream()
                              .filter(item -> {
                                  if (someCondition(item)) {
                                      return true;
                                  } else if (otherCondition(item)) {
                                      return true;
                                  }
                                  return false;
                              })
                              .findFirst();
                  }
              
                  boolean someCondition(Item item) {
              return true;
              }
                  boolean otherCondition(Item item) {
              return false;
              }
              
                  static class Item {
              }
              }
              """
          )
        );
    }

    @Test
    void formatStreamWithReduceMultilineLambda() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              
              class Test {
                  Integer sum(List<Integer> numbers) {
                      return numbers.stream().filter(n -> n > 0).reduce(0, (a, b) -> {
                          int sum = a + b;
                          System.out.println("Current sum: " + sum);
                          return sum;
                      });
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              
              class Test {
                  Integer sum(List<Integer> numbers) {
                      return numbers.stream()
              .filter(n -> n > 0)
              .reduce(0,(a, b) -> {
                          int sum = a + b;
                          System.out.println("Current sum: " + sum);
                          return sum;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void formatStreamInBuilderArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            120,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, Arrays.asList("builder", "stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  MyObject process(List<String> items) {
                      return MyObject.builder().items(items.stream().filter(s -> s.length() > 3).map(String::toUpperCase).collect(Collectors.toList())).name("name").build();
                  }
              }
              """,
            """
              package com.example;
              
              import java.util.List;
              import java.util.stream.Collectors;
              
              class Test {
                  MyObject process(List<String> items) {
                      return MyObject.builder()
              .items(items.stream()
              .filter(s -> s.length() > 3)
              .map(String::toUpperCase)
              .collect(Collectors.toList()))
              .name("name")
              .build();
                  }
              }
              """
          )
        );
    }

    @Test
    void chopIfTooLong() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            79,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(ChopIfTooLong, Arrays.asList("builder", "stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      String obj = new StringBuilder().append("test").append("25").toString();
                  }
              }
              """,
            """
              package com.example;
              
              class Test {
                  void test() {
                      String obj = new StringBuilder().append("test")
              .append("25")
              .toString();
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {80, 81})
    void doNotChopIfNotTooLong(int length) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
            length,
            new WrappingAndBracesStyle.IfStatement(false),
            new WrappingAndBracesStyle.ChainedMethodCalls(ChopIfTooLong, Arrays.asList("builder", "stream"), false),
            new WrappingAndBracesStyle.MethodDeclarationParameters(WrapAlways, false, false, false),
            null,
            null,
            null,
            null,
            null,
            null)))),
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      String obj = new StringBuilder().append("test").append("25").toString();
                  }
              }
              """
          )
        );
    }
}
