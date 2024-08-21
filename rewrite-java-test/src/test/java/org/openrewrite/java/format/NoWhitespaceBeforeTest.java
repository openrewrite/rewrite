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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.NoWhitespaceBeforeStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "ConstantConditions",
  "UnusedAssignment",
  "ReturnOfThis",
  "InfiniteLoopStatement",
  "StatementWithEmptyBody"
})
class NoWhitespaceBeforeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoWhitespaceBefore());
    }

    private static List<NamedStyles> noWhitespaceBeforeStyle() {
        return noWhitespaceBeforeStyle(style -> style);
    }

    private static List<NamedStyles> noWhitespaceBeforeStyle(UnaryOperator<NoWhitespaceBeforeStyle> with) {
        return Collections.singletonList(
          new NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(),
            singletonList(with.apply(Checkstyle.noWhitespaceBeforeStyle()))
          )
        );
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "java.nio.file.Path does not allow leading or trailing spaces on Windows")
    void packages() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              package org .openrewrite .example . cleanup;

              class Test {
              }
              """
          )
        );
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "java.nio.file.Path does not allow leading or trailing spaces on Windows")
    void imports() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              import java . util . function.*;

              class Test {
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void fieldAccessDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withDot(true)))),
          java(
            """
              class Test {
                  int m;

                  static void method() {
                      new Test()
                              .m = 2;
                      new Test() .m = 2;
                  }
              }
              """,
            """
              class Test {
                  int m;

                  static void method() {
                      new Test().m = 2;
                      new Test().m = 2;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void fieldAccessAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true).withDot(true)))),
          java(
            """
              class Test {
                  int m;

                  static void method() {
                      new Test()
                              .m = 2;
                      new Test() .m = 2;
                  }
              }
              """,
            """
              class Test {
                  int m;

                  static void method() {
                      new Test()
                              .m = 2;
                      new Test().m = 2;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    @Disabled
    void methodDeclarationParametersDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method0(String
                                             ...params) {
                  }
                  
                  static void method1(String ...params) {
                  }
              }
              """,
            """
              class Test {
                  static void method0(String...params) {
                  }
                  
                  static void method1(String...params) {
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    @Disabled
    void methodDeclarationParametersAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true)))),
          java(
            """
              class Test {
                  static void method0(String
                                             ...params) {
                  }
                  
                  static void method1(String ...params) {
                  }
              }
              """,
            """
              class Test {
                  static void method0(String
                                             ...params) {
                  }
                  
                  static void method1(String...params) {
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void methodInvocationDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withDot(true)))),
          java(
            """
              class Test {
                  Test test(int... i) {
                      return this;
                  }

                  void method(Test t) {
                      test(1 , 2) .test(3 , 4) .test( );
                      t .test()
                          .test();
                  }
              }
              """,
            """
              class Test {
                  Test test(int... i) {
                      return this;
                  }

                  void method(Test t) {
                      test(1, 2).test(3, 4).test();
                      t.test().test();
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void methodInvocationAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true).withDot(true)))),
          java(
            """
              class Test {
                  Test test(int... i) {
                      return this;
                  }

                  void method(Test t) {
                      test(1 , 2) .test(3 , 4) .test( );
                      t .test()
                          .test();
                  }
              }
              """,
            """
              class Test {
                  Test test(int... i) {
                      return this;
                  }

                  void method(Test t) {
                      test(1, 2).test(3, 4).test();
                      t.test()
                          .test();
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method() {
                      for (int i = 0 , j = 0 ; i < 2 ; i++ , j++) {
                          // do nothing
                      }
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      for (int i = 0, j = 0; i < 2; i++, j++) {
                          // do nothing
                      }
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void forEachLoop() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              import java.util.List;

              class Test {
                  static void method(List<String> list) {
                      for (String s : list) {
                          // do nothing
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void whileLoop() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method0() {
                      while (true) ;
                  }

                  static void method1() {
                      while (true) {
                          // do nothing
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhileLoop() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method() {
                      do { } while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void variableDeclarationDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method() {
                      int n , o = 0;
                      int x
                              , y = 0;
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int n, o = 0;
                      int x, y = 0;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void variableDeclarationAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true)))),
          java(
            """
              class Test {
                  static void method() {
                      int n , o = 0;
                      int x
                              , y = 0;
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      int n, o = 0;
                      int x
                              , y = 0;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void arrayDeclarations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method() {
                      int[][] array = {{1, 2}
                              , {3, 4}};
                  }
              }
              """
          )
        );
    }

    @Test
    void unaryDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              class Test {
                  static void method(int n) {
                      n ++;
                      n --;
                      n
                              ++;
                  }
              }
              """,
            """
              class Test {
                  static void method(int n) {
                      n++;
                      n--;
                      n++;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void unaryAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true)))),
          java(
            """
              class Test {
                  static void method(int n) {
                      n ++;
                      n --;
                      n
                              ++;
                  }
              }
              """,
            """
              class Test {
                  static void method(int n) {
                      n++;
                      n--;
                      n
                              ++;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void parameterizedTypeDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withGenericStart(true).withGenericEnd(true)))),
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
              import java.util.function.Function;

              class Test {
                  static void method() {
                      List <String > list0 = new ArrayList <>();
                      List <Function <String, String > > list1 = new ArrayList <>();
                      List<String
                              > list2 = new ArrayList <>();
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
              import java.util.function.Function;

              class Test {
                  static void method() {
                      List<String> list0 = new ArrayList<>();
                      List<Function<String, String>> list1 = new ArrayList<>();
                      List<String> list2 = new ArrayList<>();
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void memberReferenceDoNotAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withMethodRef(true)))),
          java(
            """
              import java.util.function.Function;
              import java.util.function.Supplier;

              class Test {
                  static void method() {
                      Supplier<Function<String, String>> a = Function ::identity;
                      Supplier<Function<String, String>> b = Function
                              ::identity;
                  }
              }
              """,
            """
              import java.util.function.Function;
              import java.util.function.Supplier;

              class Test {
                  static void method() {
                      Supplier<Function<String, String>> a = Function::identity;
                      Supplier<Function<String, String>> b = Function::identity;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void memberReferenceAllowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle(style ->
            style.withAllowLineBreaks(true).withMethodRef(true)))),
          java(
            """
              import java.util.function.Function;
              import java.util.function.Supplier;

              class Test {
                  static void method() {
                      Supplier<Function<String, String>> a = Function ::identity;
                      Supplier<Function<String, String>> b = Function
                              ::identity;
                  }
              }
              """,
            """
              import java.util.function.Function;
              import java.util.function.Supplier;

              class Test {
                  static void method() {
                      Supplier<Function<String, String>> a = Function::identity;
                      Supplier<Function<String, String>> b = Function
                              ::identity;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void doNotStripLastParameterSuffixInMethodDeclaration() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              package a;

              abstract class Test {
                  abstract Test method(
                      int n,
                      int m
                  );
              }
              """
          )
        );
    }

    @Test
    void doNotStripLastArgumentSuffixInMethodInvocation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              package a;

              class Test {
                  static void method() {
                      int n = Math.min(
                              1,
                              2
                      );
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotStripStatementSuffixInTernaryConditionAndTrue() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              import java.util.List;

              class Test {
                  static void method(List<Object> l) {
                      int n = l.isEmpty() ? l.size() : 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotStripStatementSuffixPrecedingInstanceof() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  static void method(List<Object> l) {
                      boolean b = l.subList(0, 1) instanceof ArrayList;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotStripTryWithResourcesEndParentheses() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle())),
          java(
            """
              import java.io.FileInputStream;
              import java.io.FileOutputStream;
              import java.io.InputStream;
              import java.io.OutputStream;
              import java.util.zip.GZIPInputStream;

              class Test {
                  public static void main(String[] args) {
                      try (
                              InputStream source = new GZIPInputStream(new FileInputStream(args[0]));
                              OutputStream out = new FileOutputStream(args[1])
                      ) {
                          System.out.println("side effect");
                      } catch (Exception e) {
                          // do nothing
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotStripAnnotationArguments() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(noWhitespaceBeforeStyle()))
            .typeValidationOptions(TypeValidation.none()),
          java(
            """
              import org.graalvm.compiler.core.common.SuppressFBWarnings;

              class Test {
                  @SuppressWarningsFBWarnings(
                          value = "SECPR",
                          justification = "Usages of this method are not meant for cryptographic purposes"
                  )
                  static void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/233")
    void jenkinsLibrary() {
        rewriteRun(groovy("library 'someLibrary@version'"));
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          Assertions.assertThat(new AutoFormatVisitor<>().visit(cu, 0)).isEqualTo(cu));
    }
}
