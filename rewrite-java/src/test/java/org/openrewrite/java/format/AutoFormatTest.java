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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.style.LineWrapSetting.DoNotWrap;
import static org.openrewrite.style.LineWrapSetting.WrapAlways;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AutoFormatTest implements RewriteTest {

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
          .recipe(toRecipe(() -> new AutoFormatVisitor<>(null,
            new NamedStyles(UUID.randomUUID(), "junit", "Unit Test style", "Only used in unit tests", emptySet(),
              List.of(
                new WrappingAndBracesStyle(
                  new WrappingAndBracesStyle.IfStatement(false),
                  new WrappingAndBracesStyle.ChainedMethodCalls(WrapAlways, asList("builder", "newBuilder", "stream")),
                  new WrappingAndBracesStyle.Annotations(WrapAlways),
                  new WrappingAndBracesStyle.Annotations(WrapAlways),
                  new WrappingAndBracesStyle.Annotations(WrapAlways),
                  new WrappingAndBracesStyle.Annotations(DoNotWrap),
                  new WrappingAndBracesStyle.Annotations(DoNotWrap),
                  new WrappingAndBracesStyle.Annotations(DoNotWrap)
                )
              )
              )
          )));
    }

    @DocumentExample
    @SuppressWarnings({"ClassInitializerMayBeStatic", "ReassignedVariable", "UnusedAssignment"})
    @Test
    void blockLevelStatements() {
        rewriteRun(
          java(
            """
              public class Test {
                  {        int n = 0;
                      n++;
                  }
              }
              """,
            """
              public class Test {
                  {
                      int n = 0;
                      n++;
                  }
              }
              """
          )
        );
    }

    @Test
    void blockEndOnOwnLine() {
        rewriteRun(
          java(
            """
              class Test {
                  int n = 0;}
              """,
            """
              class Test {
                  int n = 0;
              }
              """
          )
        );
    }

    @Test
    void annotatedMethod() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) Object method() {
                      return new Object();
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  Object method() {
                      return new Object();
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedMethodWithModifier() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) public Object method() {
                      return new Object();
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  public Object method() {
                      return new Object();
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedMethodWithModifiers() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) public final Object method() {
                      return new Object();
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  public final Object method() {
                      return new Object();
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedMethodWithTypeParameter() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) <T> T method() {
                      return null;
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  <T> T method() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleAnnotatedMethod() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) @Deprecated Object method() {
                      return new Object();
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  @Deprecated
                  Object method() {
                      return new Object();
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedConstructor() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings({"ALL"}) @Deprecated Test() {
                  }
              }
              """,
            """
              public class Test {
                  @SuppressWarnings({"ALL"})
                  @Deprecated
                  Test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedClassDecl() {
        rewriteRun(
          java(
            """
              @SuppressWarnings({"ALL"}) class Test {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotatedClassDeclAlreadyCorrect() {
        rewriteRun(
          java(
            """
              @SuppressWarnings({"ALL"})
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotatedClassDeclWithModifiers() {
        rewriteRun(
          java(
            """
              @SuppressWarnings({"ALL"}) public class Test {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void annotatedVariableDecl() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void doSomething() {
                      @SuppressWarnings("ALL")
                      int foo;
                  }
              }
              """,
            """
              public class Test {
                  public void doSomething() {
                      @SuppressWarnings("ALL") int foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedVariableAlreadyCorrect() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void doSomething() {
                      @SuppressWarnings("ALL") int foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedVariableDeclWithModifier() {
        rewriteRun(
          java(
            """
              public class Test {
                  @SuppressWarnings("ALL") private int foo;
              }
              """,
            """
              public class Test {
                  @SuppressWarnings("ALL")
                  private int foo;
              }
              """
          )
        );
    }

    @Test
    void annotatedVariableDeclInMethodDeclaration() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void doSomething(@SuppressWarnings("ALL") int foo) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/375")
    @Test
    void retainTrailingComments() {
        rewriteRun(
          java(
            """
              public class Test {
              int m; /* comment */ int n;}
              """,
            """
              public class Test {
                  int m; /* comment */
                  int n;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3191")
    @Test
    void emptyLineBeforeEnumConstants() {
        rewriteRun(
          java(
            """
              public enum Status {
                  NOT_STARTED,
                  STARTED
              }
              """
          )
        );
    }

    @Test
    void annotationWrapping() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              @Foo @Foo class Test {
                  @Foo @Foo int field;
              
                  @Foo @Foo void method(
                          @Foo
                          @Foo
                          int param) {
                      @Foo
                      @Foo
                      int localVar;
                  }
              }
              
              enum MyEnum {
                  @Foo
                  @Foo
                  VALUE
              }
              
              record someRecord(
                      @Foo
                      @Foo
                      String name) {
              }
              """,
            """
              @Foo
              @Foo
              class Test {
                  @Foo
                  @Foo
                  int field;
              
                  @Foo
                  @Foo
                  void method(
                          @Foo @Foo int param) {
                      @Foo @Foo int localVar;
                  }
              }
              
              enum MyEnum {
                  @Foo @Foo VALUE
              }
              
              record someRecord(
                      @Foo @Foo String name) {
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingAlreadyCorrect() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              @Foo
              @Foo
              class Test {
                  @Foo
                  @Foo
                  int field;
              
                  @Foo
                  @Foo
                  void method(@Foo @Foo int param) {
                      @Foo @Foo int localVar;
                  }
              }
              
              enum MyEnum {
                  @Foo @Foo VALUE
              }
              
              record someRecord(
                      @Foo @Foo String name) {
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingModifiers() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              @Foo @Foo final class Test {
                  @Foo @Foo private int field;
              
                  @Foo @Foo public void method(
                          @Foo
                          @Foo
                          final int param) {
                      @Foo
                      @Foo
                      final int localVar;
                  }
              }
              """,
            """
              @Foo
              @Foo
              final class Test {
                  @Foo
                  @Foo
                  private int field;
              
                  @Foo
                  @Foo
                  public void method(
                          @Foo @Foo final int param) {
                      @Foo @Foo final int localVar;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingModifiersAlreadyCorrect() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(

            """
              @Foo
              @Foo
              final class Test {
                  @Foo
                  @Foo
                  private int field;
              
                  @Foo
                  @Foo
                  public void method(
                          @Foo @Foo final int param) {
                      @Foo @Foo final int localVar;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingGenerics() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              @Foo @Foo class Test<T> {
                  @Foo @Foo private int field;
              
                  @Foo @Foo Test(int field) {
                      this.field = field;
                  }
              
                  @Foo @Foo T method(
                          @Foo
                          @Foo
                          T param) {
                      @Foo
                      @Foo
                      T localVar;
                      return param;
                  }
              }
              """,
            """
              @Foo
              @Foo
              class Test<T> {
                  @Foo
                  @Foo
                  private int field;
              
                  @Foo
                  @Foo
                  Test(int field) {
                      this.field = field;
                  }
              
                  @Foo
                  @Foo
                  T method(
                          @Foo @Foo T param) {
                      @Foo @Foo T localVar;
                      return param;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingGenericsAlreadyCorrect() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              @Foo
              @Foo
              class Test<T> {
                  @Foo
                  @Foo
                  private int field;
              
                  @Foo
                  @Foo
                  Test(int field) {
                      this.field = field;
                  }
              
                  @Foo
                  @Foo
                  T method(
                          @Foo @Foo T param) {
                      @Foo @Foo T localVar;
                      return param;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingWithComments() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              class Test {
                  @Foo //comment
                  String method1() {
                      return "test";
                  }
              
                  @Foo /* comment
                  on multiple
                  lines */
                  String method2() {
                      return "test";
                  }
              
                  @Foo
                  //comment
                  String method3() {
                      return "test";
                  }
              
                  @Foo
                  /* comment
                  on multiple
                  lines */
                  String method4() {
                      return "test";
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingWithCommentsWithModifiers() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Repeatable;
              
              @Repeatable(Foo.Foos.class)
              @interface Foo {
                  @interface Foos {
                      Foo[] value();
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              class Test {
                  @Foo //comment
                  final String method1() {
                      return "test";
                  }
              
                  @Foo /* comment
                  on multiple
                  lines */
                  final String method2() {
                      return "test";
                  }
              
                  @Foo
                  //comment
                  final String method3() {
                      return "test";
                  }
              
                  @Foo
                  /* comment
                  on multiple
                  lines */
                  final String method4() {
                      return "test";
                  }
              }
              """
          )
        );
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
    void doNotFormatNonBuilderChainedCalls() {
        rewriteRun(
          java(
            """
              package com.example;
              
              class Test {
                  void test() {
                      String result = "hello".toUpperCase().substring(1).trim();
                      String sb = new StringBuilder().append("a").append("b").toString();
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
                              .reduce(0, (a, b) -> {
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
}
