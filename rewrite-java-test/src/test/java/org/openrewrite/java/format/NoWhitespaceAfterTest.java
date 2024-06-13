/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.NoWhitespaceAfterStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "StringConcatenationMissingWhitespace",
  "ConstantConditions",
  "UnusedAssignment",
  "UnaryPlus",
  "ReturnOfThis",
  "SimplifiableAnnotation"
})
class NoWhitespaceAfterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoWhitespaceAfter());
    }

    private static List<NamedStyles> noWhitespaceAfterStyle() {
        return noWhitespaceAfterStyle(style -> style);
    }

    private static List<NamedStyles> noWhitespaceAfterStyle(UnaryOperator<NoWhitespaceAfterStyle> with) {
        return Collections.singletonList(
          new NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(),
            singletonList(with.apply(Checkstyle.noWhitespaceAfterStyle()))
          )
        );
    }

    @Test
    void arrayAccess() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static void method(int[] n) {
                      int m = n[0];
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @DocumentExample
    @Test
    void variableDeclaration() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static void method() {
                      int [] [] a;
                      int [] b;
                      int c, d = 0;
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int[][] a;
                      int[] b;
                      int c, d = 0;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void arrayVariableDeclaration() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static void method() {
                      int[] n = { 1, 2};
                      int[] p = {1, 2 };
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int[] n = {1, 2};
                      int[] p = {1, 2};
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static void method(int m) {
                      long o = - m;
                  }
              }
              """,
            """
              class Test {
                  static void method(int m) {
                      long o = -m;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void unaryOperation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static void method(int m) {
                      ++ m;
                      -- m;
                      int o = + m;
                      o = ~ m;
                      boolean b = false;
                      b = ! b;
                  }
              }
              """,
            """
              class Test {
                  static void method(int m) {
                      ++m;
                      --m;
                      int o = +m;
                      o = ~m;
                      boolean b = false;
                      b = !b;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void typecastOperation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle(style ->
            style.withTypecast(true)))),
          java(
            """
              class Test {
                  static void method(int m) {
                      long o = - m;
                      m = (int) o;
                  }
              }
              """,
            """
              class Test {
                  static void method(int m) {
                      long o = -m;
                      m = (int)o;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void methodReference() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle(style ->
            style.withMethodRef(true)))),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> stream) {
                      stream.forEach(System.out:: println);
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> stream) {
                      stream.forEach(System.out::println);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void methodReturnTypeSignatureAsArray() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  static int [] [] methodArr() {
                      return null;
                  }
              }
              """,
            """
              class Test {
                  static int[][] methodArr() {
                      return null;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void fieldAccess() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle(style ->
            style.withDot(true)))),
          java(
            """
              class Test {
                  int m = 0;

                  void method0() {
                      int n = this. m;
                  }

                  static void method1() {
                      new Test()
                              .m = 2;
                  }
              }
              """,
            """
              class Test {
                  int m = 0;

                  void method0() {
                      int n = this.m;
                  }

                  static void method1() {
                      new Test()
                              .m = 2;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  @ Override
                  public boolean equals(Object o) {
                      return false;
                  }
              }
              """,
            """
              class Test {
                  @Override
                  public boolean equals(Object o) {
                      return false;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void doNotAllowLinebreak() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle(style ->
            style.withAllowLineBreaks(false).withDot(true)))),
          java(
            """
              class Test {
                  int m;

                  static void fieldAccess() {
                      new Test().
                              m = 2;
                  }

                  void methodInvocationChain() {
                      test().
                              test();
                  }

                  Test test() {
                      return this;
                  }
              }
              """,
            """
              class Test {
                  int m;

                  static void fieldAccess() {
                      new Test().m = 2;
                  }

                  void methodInvocationChain() {
                      test().test();
                  }

                  Test test() {
                      return this;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void doNotChangeAnnotationValueInNewArray() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              @SuppressWarnings(value = {
                      "all",
                      "unchecked"
              })
              class Test {
              }
              """
          )
        );
    }

    @Test
    void doNotChangeFirstAndLastValuesOfArrayInitializer() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              class Test {
                  int[] ns = {
                          0,
                          1
                  };
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2911")
    @Test
    void dontWronglyHandleArray() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceAfterStyle())),
          java(
            """
              package sample;
              
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;
              
              public class ArrayNotNull {
              
                  byte[] bytes = new byte[0];
              
                  public byte @NotNull [] getBytes() {
                      return bytes;
                  }
              
                  int[] ints = new int[0];
              
                  public int @NotNull [] getInts() {
                      return ints;
                  }
              
                  Object[] objects = new Object[0];
              
                  public Object @NotNull [] getObjects() {
                      return objects;
                  }
              
              }
              
              @Target(ElementType.TYPE_USE)
              @interface NotNull {}
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          assertThat(new AutoFormatVisitor<>().visit(cu, 0)).isEqualTo(cu));
    }
}
