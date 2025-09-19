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
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.style.LineWrapSetting.DoNotWrap;
import static org.openrewrite.style.LineWrapSetting.WrapAlways;
import static org.openrewrite.test.RewriteTest.toRecipe;

class WrappingAndBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
          new WrappingAndBracesStyle.IfStatement(false),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(WrapAlways),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap),
          new WrappingAndBracesStyle.Annotations(DoNotWrap)))));
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

    private static Consumer<RecipeSpec> wrappingAndBraces(UnaryOperator<SpacesStyle> spaces,
                                                          UnaryOperator<WrappingAndBracesStyle> wrapping) {
        return spec -> spec
          .recipes(
            new WrappingAndBraces(),
            new TabsAndIndents(),
            new Spaces()
          )
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              List.of(
                spaces.apply(IntelliJ.spaces()),
                wrapping.apply(IntelliJ.wrappingAndBraces())
              )
            )
          )));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/804")
    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions"})
    @Test
    void conditionalsShouldStartOnNewLines() {
        rewriteRun(
          spec -> spec.recipes(
            new WrappingAndBraces(),
            new TabsAndIndents()
          ),
          java(
            """
              class Test {
                  void test() {
                      if (1 == 2) {
                      } if (1 == 3) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      if (1 == 2) {
                      }
                      if (1 == 3) {
                      }
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Test
    void elseOnNewLine() {
        rewriteRun(
          wrappingAndBraces(
            spaces -> spaces.withBeforeKeywords(spaces.getBeforeKeywords().withElseKeyword(true)),
            wrap -> wrap.withIfStatement(new WrappingAndBracesStyle.IfStatement(true))),
          java(
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      } else if (arg0 == 1) {
                          System.out.println("else if");
                      } else {
                          System.out.println("else");
                      }
                  }
              }
              """,
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      }
                      else if (arg0 == 1) {
                          System.out.println("else if");
                      }
                      else {
                          System.out.println("else");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Test
    void elseNotOnNewLine() {
        rewriteRun(
          wrappingAndBraces(
            spaces -> spaces.withBeforeKeywords(spaces.getBeforeKeywords().withElseKeyword(true)),
            wrap -> wrap.withIfStatement(new WrappingAndBracesStyle.IfStatement(false))),
          java(
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      }
                      else if (arg0 == 1) {
                          System.out.println("else if");
                      }
                      else {
                          System.out.println("else");
                      }
                  }
              }
              """,
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      } else if (arg0 == 1) {
                          System.out.println("else if");
                      } else {
                          System.out.println("else");
                      }
                  }
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
                  String method1(){
                      return "test";
                  }

                  @Foo /* comment
                  on multiple
                  lines */
                  String method2(){
                      return "test";
                  }

                  @Foo
                  //comment
                  String method3(){
                      return "test";
                  }

                  @Foo
                  /* comment
                  on multiple
                  lines */
                  String method4(){
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
                  final String method1(){
                      return "test";
                  }

                  @Foo /* comment
                  on multiple
                  lines */
                  final String method2(){
                      return "test";
                  }

                  @Foo
                  //comment
                  final String method3(){
                      return "test";
                  }

                  @Foo
                  /* comment
                  on multiple
                  lines */
                  final String method4(){
                      return "test";
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingWithNulls() {
        rewriteRun(spec ->
            spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(new WrappingAndBracesStyle(
              new WrappingAndBracesStyle.IfStatement(false),
              null,
              null,
              null,
              null,
              null,
              null)))),
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
                  final String method1(){
                      return "test";
                  }

                  @Foo /* comment
                  on multiple
                  lines */
                  final String method2(){
                      return "test";
                  }

                  @Foo
                  //comment
                  final String method3(){
                      return "test";
                  }

                  @Foo
                  /* comment
                  on multiple
                  lines */
                  final String method4(){
                      return "test";
                  }
              }
              """
          )
        );
    }
}
