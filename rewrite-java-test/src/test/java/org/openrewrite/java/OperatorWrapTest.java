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
package org.openrewrite.java;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.cleanup.OperatorWrap;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.OperatorWrapStyle;
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
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"StringConcatenationMissingWhitespace", "ConstantConditions", "CStyleArrayDeclaration"})
class OperatorWrapTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OperatorWrap());
    }

    private static List<NamedStyles> operatorWrapStyle() {
        return operatorWrapStyle(style -> style);
    }

    private static List<NamedStyles> operatorWrapStyle(UnaryOperator<OperatorWrapStyle> with) {
        return Collections.singletonList(
          new NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(),
            singletonList(with.apply(Checkstyle.operatorWrapStyle()))
          )
        );
    }

    @Test
    void binaryOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle())),
          java(
            """
              class Test {
                  static void method() {
                      String s = "aaa" +
                              "b" + "c";
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      String s = "aaa"
                              + "b" + "c";
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void binaryOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL)))),
          java(
            """
              class Test {
                  static void method() {
                      String s = "aaa"
                              + "b" + "c";
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      String s = "aaa" +
                              "b" + "c";
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void typeParameterOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle())),
          java(
            """
              import java.io.Serializable;

              class Test {
                  static <T extends Serializable &
                          Comparable<T>> T method0() {
                      return null;
                  }

                  static <T extends Serializable> T method1() {
                      return null;
                  }
              }
              """,
            """
              import java.io.Serializable;

              class Test {
                  static <T extends Serializable
                          & Comparable<T>> T method0() {
                      return null;
                  }

                  static <T extends Serializable> T method1() {
                      return null;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void typeParameterOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL)))),
          java(
            """
              import java.io.Serializable;

              class Test {
                  static <T extends Serializable
                          & Comparable<T>> T method0() {
                      return null;
                  }

                  static <T extends Serializable> T method1() {
                      return null;
                  }
              }
              """,
            """
              import java.io.Serializable;

              class Test {
                  static <T extends Serializable &
                          Comparable<T>> T method0() {
                      return null;
                  }

                  static <T extends Serializable> T method1() {
                      return null;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void instanceOfOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle())),
          java(
            """
              class Test {
                  static Object method(Object s) {
                      if (s instanceof
                              String) {
                          return null;
                      }
                      return s;
                  }
              }
              """,
            """
              class Test {
                  static Object method(Object s) {
                      if (s
                              instanceof String) {
                          return null;
                      }
                      return s;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void instanceOfOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL)))),
          java(
            """
              class Test {
                  static Object method(Object s) {
                      if (s
                              instanceof String) {
                          return null;
                      }
                      return s;
                  }
              }
              """,
            """
              class Test {
                  static Object method(Object s) {
                      if (s instanceof
                              String) {
                          return null;
                      }
                      return s;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void ternaryOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle())),
          java(
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a") ?
                              "truePart" :
                              "falsePart";
                  }
              }
              """,
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a")
                              ? "truePart"
                              : "falsePart";
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void ternaryOnNewlineIgnoringColon() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withColon(false)))),
          java(
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a") ?
                              "truePart" :
                              "falsePart";
                  }
              }
              """,
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a")
                              ? "truePart" :
                              "falsePart";
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void ternaryOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL)))),
          java(
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a")
                              ? "truePart"
                              : "falsePart";
                  }
              }
              """,
            """
              class Test {
                  static String method(String s) {
                      return s.contains("a") ?
                              "truePart" :
                              "falsePart";
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void assignmentOperatorOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withAssign(true)
              .withDivAssign(true)
              .withPlusAssign(true)
              .withMinusAssign(true)
              .withStarAssign(true)
              .withModAssign(true)
              .withSrAssign(true)
              .withBsrAssign(true)
              .withSlAssign(true)
              .withBxorAssign(true)
              .withBorAssign(true)
              .withBandAssign(true)
          ))),
          java(
            """
              class Test {
                  static int method() {
                      int a = 0;
                      a /=
                              1;
                      a +=
                              1;
                      return a;
                  }
              }
              """,
            """
              class Test {
                  static int method() {
                      int a = 0;
                      a
                              /= 1;
                      a
                              += 1;
                      return a;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void assignmentOperatorOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL)
              .withAssign(true)
              .withDivAssign(true)
              .withPlusAssign(true)
              .withMinusAssign(true)
              .withStarAssign(true)
              .withModAssign(true)
              .withSrAssign(true)
              .withBsrAssign(true)
              .withSlAssign(true)
              .withBxorAssign(true)
              .withBorAssign(true)
              .withBandAssign(true)
          ))),
          java(
            """
              class Test {
                  static int method() {
                      int a = 0;
                      a
                              /= 1;
                      a
                              += 1;
                      return a;
                  }
              }
              """,
            """
              class Test {
                  static int method() {
                      int a = 0;
                      a /=
                              1;
                      a +=
                              1;
                      return a;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void memberReferenceOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withMethodRef(true)))),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void methodStream(Stream<Object> stream) {
                      stream.forEach(System.out::
                              println);
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void methodStream(Stream<Object> stream) {
                      stream.forEach(System.out
                              ::println);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void memberReferenceOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL).withMethodRef(true)))),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void methodStream(Stream<Object> stream) {
                      stream.forEach(System.out
                              ::println);
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void methodStream(Stream<Object> stream) {
                      stream.forEach(System.out::
                              println);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void assignmentOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withAssign(true)))),
          java(
            """
              class Test {
                  static int method() {
                      int n;
                      n =
                              1;
                      return n;
                  }
              }
              """,
            """
              class Test {
                  static int method() {
                      int n;
                      n
                              = 1;
                      return n;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void assignmentOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL).withAssign(true)))),
          java(
            """
              class Test {
                  static int method() {
                      int n;
                      n
                              = 1;
                      return n;
                  }
              }
              """,
            """
              class Test {
                  static int method() {
                      int n;
                      n =
                              1;
                      return n;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void variableOnNewline() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withAssign(true)))),
          java(
            """
              class Test {
                  static void method() {
                      int n =
                              1;
                      int nArr[] =
                              new int[0];
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int n
                              = 1;
                      int nArr[]
                              = new int[0];
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void variableOnEndOfLine() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(operatorWrapStyle(style ->
            style.withWrapOption(OperatorWrapStyle.WrapOption.EOL).withAssign(true)))),
          java(
            """
              class Test {
                  static void method() {
                      int n
                              = 1;
                      int nArr[]
                              = new int[0];
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int n =
                              1;
                      int nArr[] =
                              new int[0];
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          Assertions.assertThat(new AutoFormatVisitor<>().visit(cu, 0)).isEqualTo(cu));
    }
}
