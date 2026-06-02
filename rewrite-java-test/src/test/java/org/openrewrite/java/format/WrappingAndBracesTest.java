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
import static org.openrewrite.style.LineWrapSetting.*;

@SuppressWarnings("all")
class WrappingAndBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        autoFormat(
          spaces -> spaces,
          wrapping -> wrapping
            .withChainedMethodCalls(wrapping.getChainedMethodCalls().withWrap(WrapAlways).withBuilderMethods(List.of("builder", "newBuilder")))
            .withMethodDeclarationParameters(wrapping.getMethodDeclarationParameters().withWrap(WrapAlways))).accept(spec);
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

    private static Consumer<RecipeSpec> autoFormat(UnaryOperator<SpacesStyle> spaces,
                                                   UnaryOperator<WrappingAndBracesStyle> wrapping) {
        return spec -> spec.recipe(new AutoFormat(null))
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              List.of(
                spaces.apply(IntelliJ.spaces()),
                wrapping.apply(IntelliJ.wrappingAndBraces().withKeepWhenFormatting(IntelliJ.wrappingAndBraces().getKeepWhenFormatting().withLineBreaks(false)))
              )
            )
          )));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/804")
    @SuppressWarnings({"StatementWithEmptyBody", "ConstantConditions"})
    @Test
    void conditionalsShouldStartOnNewLines() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping
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
    void blocksShouldCloseOnNextLine() {
        rewriteRun(
          java(
            """
              public class TestClass {}
              """,
            """
              public class TestClass {
              }
              """
          ),
          java(
            """
              public class TestMethod {
                  void method() {}
              }
              """,
            """
              public class TestMethod {
                  void method() {
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
          autoFormat(
            spaces -> spaces.withBeforeKeywords(spaces.getBeforeKeywords().withElseKeyword(true)),
            wrap -> wrap.withIfStatement(IntelliJ.wrappingAndBraces().getIfStatement().withElseOnNewLine(true))),
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
          autoFormat(
            spaces -> spaces.withBeforeKeywords(spaces.getBeforeKeywords().withElseKeyword(true)),
            wrap -> wrap),
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

    @Test
    void ifAlwaysForceBraces() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withIfStatement(wrap.getIfStatement().withForceBraces(WrappingAndBracesStyle.ForceBraces.Always))),
          java(
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0)
                          System.out.println("if");
                      else if (arg0 == 1) {
                          System.out.println("else if 1");
                          System.out.println("else if 2");
                      }
                      else
                          System.out.println("else");
                  }
              }
              """,
            """
              public class Test {
                  void method(int arg0) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      } else if (arg0 == 1) {
                          System.out.println("else if 1");
                          System.out.println("else if 2");
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
                  void method(@Foo @Foo int param) {
                      @Foo @Foo int localVar;
                  }
              }
              
              enum MyEnum {
                  @Foo @Foo VALUE
              }
              
              record someRecord(@Foo @Foo String name) {
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
              
              record someRecord(@Foo @Foo String name) {
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
                  public void method(@Foo @Foo final int param) {
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
                  public void method(@Foo @Foo final int param) {
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
                  T method(@Foo @Foo T param) {
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
                  T method(@Foo @Foo T param) {
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
                  lines */ final String method2() {
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
                  lines */ final String method4() {
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
            autoFormat(
              spaces -> spaces,
              wrapping -> wrapping.withMethodDeclarationParameters(IntelliJ.wrappingAndBraces().getMethodDeclarationParameters().withWrap(WrapAlways))
            ),
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
                  lines */ final String method2() {
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
                  lines */ final String method4() {
                      return "test";
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingRecords() {
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
              record someRecord(
                      @Foo
                      @Foo
                      String name,
                      @Foo
                      @Foo
                      String place
                      ) {
              }
              """,
            """
              record someRecord(@Foo @Foo String name, @Foo @Foo String place) {
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingRecordsWithOpenNewLineAndCloseNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withRecordComponents(wrap.getRecordComponents().withWrap(WrapAlways).withOpenNewLine(true).withCloseNewLine(true))
          ),
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
              record someRecord(@Foo
                                @Foo
                                String name,
                                @Foo
                                @Foo
                                String place) {
              }
              """,
            """
              record someRecord(
                      @Foo @Foo String name,
                      @Foo @Foo String place
              ) {
              }
              """
          )
        );
    }

    @Test
    void annotationWrappingRecordsWithNewLineAnnotations() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withRecordComponents(wrap.getRecordComponents().withWrap(WrapAlways).withNewLineForAnnotations(true))
          ),
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
              record someRecord(@Foo @Foo String name, @Foo @Foo String place) {
              }
              """,
            """
              record someRecord(@Foo
                                @Foo
                                String name,
                                @Foo
                                @Foo
                                String place) {
              }
              """
          )
        );
    }

    @Test
    void doNotWrapImplementsList() {
        rewriteRun(
          java(
            """
              public class Interfaces {
                  public interface I1 {
                  }
              
                  public interface I2 {
                  }
              
                  public interface I3 {
                  }
              }
              """
          ),
          java(
            """
              public class Test implements Interfaces.I1, Interfaces.I2, Interfaces.I3 {
              }
              """
          )
        );
    }

    @Test
    void alwaysWrapImplementsList() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping.withExtendsImplementsPermitsList(wrapping.getExtendsImplementsPermitsList().withWrap(WrapAlways))
          ),
          java(
            """
              public class Interfaces {
                  public interface I1 {
                  }
                  public interface I2 {
                  }
                  public interface I3 {
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              public class Test implements Interfaces.I1, Interfaces.I2, Interfaces.I3 {
              }
              """,
            """
              public class Test implements Interfaces.I1,
                      Interfaces.I2,
                      Interfaces.I3 {
              }
              """
          )
        );
    }

    @Test
    void alignWrappedImplementsList() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping.withExtendsImplementsPermitsList(wrapping.getExtendsImplementsPermitsList().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              public class Interfaces {
                  public interface I1 {
                  }
                  public interface I2 {
                  }
                  public interface I3 {
                  }
              }
              """,
            SourceSpec::skip),
          java(
            """
              public class Test implements Interfaces.I1, Interfaces.I2, Interfaces.I3 {
              }
              """,
            """
              public class Test implements Interfaces.I1,
                                           Interfaces.I2,
                                           Interfaces.I3 {
              }
              """
          )
        );
    }

    @Test
    void forAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withForStatement(wrap.getForStatement().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      for (int i = 0;
                           i < 10;
                           i++) {
                          System.out.println(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void forNoAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withForStatement(wrap.getForStatement().withWrap(WrapAlways).withAlignWhenMultiline(false))
          ),
          java(
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      for (int i = 0;
                              i < 10;
                              i++) {
                          System.out.println(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void forOpenAndCloseOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withForStatement(wrap.getForStatement().withWrap(WrapAlways).withOpenNewLine(true).withCloseNewLine(true))
          ),
          java(
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      for (
                              int i = 0;
                              i < 10;
                              i++
                      ) {
                          System.out.println(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void forOnlyOpenAndCloseOnNewLineWhenWrapping() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withForStatement(wrap.getForStatement().withOpenNewLine(true).withCloseNewLine(true))
          ),
          java(
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void forWithForcedBlock() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withForStatement(wrap.getForStatement().withForceBraces(WrappingAndBracesStyle.ForceBraces.Always))
          ),
          java(
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++)
                          System.out.println(i);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      for (int i = 0; i < 10; i++) {
                          System.out.println(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void whileWithForcedBlock() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withWhileStatement(wrap.getWhileStatement().withForceBraces(WrappingAndBracesStyle.ForceBraces.Always))
          ),
          java(
            """
              class Test {
                  void test() {
                      while (true)
                          System.out.println("TESTING");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      while (true) {
                          System.out.println("TESTING");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhileWithForcedBlock() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withDoWhileStatement(wrap.getDoWhileStatement().withForceBraces(WrappingAndBracesStyle.ForceBraces.Always))
          ),
          java(
            """
              class Test {
                  void test() {
                      do
                          System.out.println("TESTING");
                      while (true);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      do {
                          System.out.println("TESTING");
                      } while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhileWithWhileOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withDoWhileStatement(wrap.getDoWhileStatement().withWhileOnNewLine(true))
          ),
          java(
            """
              class Test {
                  void test() {
                      do {
                          System.out.println("TESTING");
                      } while (true);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      do {
                          System.out.println("TESTING");
                      }
                      while (true);
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResourcesAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryWithResources(wrap.getTryWithResources().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt"); BufferedReader br = new BufferedReader(fr); FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt");
                           BufferedReader br = new BufferedReader(fr);
                           FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResourcesNoAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryWithResources(wrap.getTryWithResources().withWrap(WrapAlways).withAlignWhenMultiline(false))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt"); BufferedReader br = new BufferedReader(fr); FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt");
                              BufferedReader br = new BufferedReader(fr);
                              FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResourcesOpenAndCloseOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryWithResources(wrap.getTryWithResources().withWrap(WrapAlways).withOpenNewLine(true).withCloseNewLine(true))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt"); BufferedReader br = new BufferedReader(fr); FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (
                              FileReader fr = new FileReader("input.txt");
                              BufferedReader br = new BufferedReader(fr);
                              FileWriter fw = new FileWriter("output.txt")
                      ) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResourcesOpenAndCloseOnNewLineOnlyWhenWrapping() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryWithResources(wrap.getTryWithResources().withOpenNewLine(true).withCloseNewLine(true))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() {
                      try (FileReader fr = new FileReader("input.txt"); BufferedReader br = new BufferedReader(fr); FileWriter fw = new FileWriter("output.txt")) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryWithResourcesWithBlockInside() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryWithResources(wrap.getTryWithResources().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              import java.util.function.Supplier;
              
              public class Test {
                  void test() {
                      try (Resource r1 = new Resource(() -> true); Resource r2 = new Resource(() -> {
                          assert true;
                          return true;
                      }); Resource r3 = new Resource()) {
                      }
                  }
              
                  class Resource implements AutoCloseable {
                      Resource() {
                      }
              
                      Resource(Supplier<Boolean> supplier) {
                      }
              
                      @Override
                      public void close() {
                      }
                  }
              }
              """,
            """
              import java.util.function.Supplier;
              
              public class Test {
                  void test() {
                      try (Resource r1 = new Resource(() -> true);
                           Resource r2 = new Resource(() -> {
                               assert true;
                               return true;
                           });
                           Resource r3 = new Resource()) {
                      }
                  }
              
                  class Resource implements AutoCloseable {
                      Resource() {
                      }
              
                      Resource(Supplier<Boolean> supplier) {
                      }
              
                      @Override
                      public void close() {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryStatement(wrap.getTryStatement().withCatchOnNewLine(true).withFinallyOnNewLine(true))
          ),
          java(
            """
              class Test {
                  void test() {
                      try {
                          System.out.println("try");
                      } catch (RuntimeException e) {
                          System.out.println("catch");
                      } catch (Exception e) {
                          System.out.println("exception");
                      } catch (Throwable | Error e) {
                          System.out.println("error or throwable");
                      } finally {
                          System.out.println("finally");
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      try {
                          System.out.println("try");
                      }
                      catch (RuntimeException e) {
                          System.out.println("catch");
                      }
                      catch (Exception e) {
                          System.out.println("exception");
                      }
                      catch (Throwable | Error e) {
                          System.out.println("error or throwable");
                      }
                      finally {
                          System.out.println("finally");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchNotOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withTryStatement(wrap.getTryStatement().withCatchOnNewLine(false).withFinallyOnNewLine(false))
          ),
          java(
            """
              class Test {
                  void test() {
                      try {
                          System.out.println("try");
                      }
                      catch (RuntimeException e) {
                          System.out.println("catch");
                      }
                      catch (Exception e) {
                          System.out.println("exception");
                      }
                      catch (Throwable | Error e) {
                          System.out.println("error or throwable");
                      }
                      finally {
                          System.out.println("finally");
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      try {
                          System.out.println("try");
                      } catch (RuntimeException e) {
                          System.out.println("catch");
                      } catch (Exception e) {
                          System.out.println("exception");
                      } catch (Throwable | Error e) {
                          System.out.println("error or throwable");
                      } finally {
                          System.out.println("finally");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void wrapThrowsKeyword() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withThrowsKeyword(wrap.getThrowsKeyword().withWrap(WrapAlways))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() throws FileNotFoundException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test()
                          throws FileNotFoundException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """
          )
        );
    }

    @Test
    void alignThrowsToMethodStart() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withThrowsList(wrap.getThrowsList().withAlignThrowsToMethodStart(true))
              .withThrowsKeyword(wrap.getThrowsKeyword().withWrap(WrapAlways))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() throws FileNotFoundException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test()
                  throws FileNotFoundException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """
          )
        );
    }

    @Test
    void wrapThrowsListAlways() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withThrowsList(wrap.getThrowsList().withWrap(WrapAlways))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() throws FileNotFoundException, IOException, RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test() throws
                          FileNotFoundException,
                          IOException,
                          RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """
          )
        );
    }

    @Test
    void wrapThrowsListAlwaysAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withThrowsList(wrap.getThrowsList().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() throws FileNotFoundException, IOException, RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test() throws
                              FileNotFoundException,
                              IOException,
                              RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """
          )
        );
    }

    @Test
    void wrapThrowsListWithThrowsKeywordWrap() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withThrowsKeyword(wrap.getThrowsKeyword().withWrap(WrapAlways))
              .withThrowsList(wrap.getThrowsList().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              import java.io.*;
              
              class Test {
                  void test() throws FileNotFoundException, IOException, RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """,
            """
              import java.io.*;
              
              class Test {
                  void test()
                          throws
                          FileNotFoundException,
                          IOException,
                          RuntimeException {
                      FileReader fr = new FileReader("input.txt");
                  }
              }
              """
          )
        );
    }

    @Test
    void enumConstantsWrapAlways() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(WrapAlways))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED, STARTED, COMPLETED
              }
              """,
            """
              enum Status {
                  NOT_STARTED,
                  STARTED,
                  COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsWrapAlwaysAlreadyCorrect() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(WrapAlways))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED,
                  STARTED,
                  COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsChopIfTooLong() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withHardWrapAt(60)
              .withEnumConstants(wrap.getEnumConstants().withWrap(ChopIfTooLong))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED, STARTED_BUT_NAMED_VERY_LONG_TO_MAKE_SURE_THAT_WE_WRAP, COMPLETED
              }
              """,
            """
              enum Status {
                  NOT_STARTED,
                  STARTED_BUT_NAMED_VERY_LONG_TO_MAKE_SURE_THAT_WE_WRAP,
                  COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsChopIfTooLongNotLongEnough() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(ChopIfTooLong))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED, STARTED, COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsSingleLineChopIfTooLong() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withHardWrapAt(40)
              .withEnumConstants(wrap.getEnumConstants().withWrap(ChopIfTooLong))
          ),
          java(
            """
              enum Status { NOT_STARTED, STARTED, COMPLETED }
              """,
            """
              enum Status {
                  NOT_STARTED,
                  STARTED,
                  COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsSingleLineChopIfTooLongNotLongEnough() {
        rewriteRun(
          autoFormat(
            spaces -> spaces.withOther(spaces.getOther().withInsideOneLineEnumBraces(true)),
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(ChopIfTooLong))
          ),
          java(
            """
              enum Status { NOT_STARTED, STARTED, COMPLETED }
              """
          )
        );
    }

    @Test
    void enumConstantsDoNotWrap() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(DoNotWrap))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED, STARTED, COMPLETED
              }
              """
          )
        );
    }

    @Test
    void enumConstantsWithArguments() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(WrapAlways))
          ),
          java(
            """
              enum Status {
                  NOT_STARTED("Not Started"), STARTED("Started"), COMPLETED("Completed");
              
                  private final String displayName;
              
                  Status(String displayName) {
                      this.displayName = displayName;
                  }
              }
              """,
            """
              enum Status {
                  NOT_STARTED("Not Started"),
                  STARTED("Started"),
                  COMPLETED("Completed");
              
                  private final String displayName;
              
                  Status(String displayName) {
                      this.displayName = displayName;
                  }
              }
              """
          )
        );
    }

    @Test
    void enumConstantsSingleLineToWrapped() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withEnumConstants(wrap.getEnumConstants().withWrap(WrapAlways))
          ),
          java(
            """
              enum Status { NOT_STARTED, STARTED, COMPLETED }
              """,
            """
              enum Status {
                  NOT_STARTED,
                  STARTED,
                  COMPLETED
              }
              """
          )
        );
    }

    @Test
    void modifierListWrapAfterModifierListClass() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withModifierList(wrap.getModifierList().withWrapAfterModifierList(true))
          ),
          java(
            """
              public final class Test {
              }
              """,
            """
              public final
              class Test {
              }
              """
          )
        );
    }

    @Test
    void modifierListWrapAfterModifierListMethod() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withModifierList(wrap.getModifierList().withWrapAfterModifierList(true))
          ),
          java(
            """
              class Test {
                  public static void method() {
                  }
              }
              """,
            """
              class Test {
                  public static
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierListWrapAfterModifierListField() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withModifierList(wrap.getModifierList().withWrapAfterModifierList(true))
          ),
          java(
            """
              class Test {
                  public static final int CONSTANT = 1;
              }
              """,
            """
              class Test {
                  public static final
                  int CONSTANT = 1;
              }
              """
          )
        );
    }

    @Test
    void modifierListWrapAfterModifierListAlreadyCorrect() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withModifierList(wrap.getModifierList().withWrapAfterModifierList(true))
          ),
          java(
            """
              public final
              class Test {
                  public static final
                  int CONSTANT = 1;
              
                  public static
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierListNoWrapAfterModifierList() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withModifierList(wrap.getModifierList().withWrapAfterModifierList(false))
          ),
          java(
            """
              public final class Test {
                  public static final int CONSTANT = 1;
              
                  public static void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerWrapAlways() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(WrapAlways))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = {1,
                          2,
                          3,
                          4,
                          5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerWrapAlwaysAlreadyCorrect() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(WrapAlways))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1,
                          2,
                          3,
                          4,
                          5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerWrapAlwaysAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = {1,
                                   2,
                                   3,
                                   4,
                                   5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerNewLineAfterOpeningCurly() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(WrapAlways).withNewLineAfterOpeningCurly(true))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = {
                          1,
                          2,
                          3,
                          4,
                          5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerPlaceClosingCurlyOnNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(WrapAlways).withPlaceClosingCurlyOnNewLine(true))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = {1,
                          2,
                          3,
                          4,
                          5
                  };
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerAllOptions() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer()
              .withWrap(WrapAlways)
              .withNewLineAfterOpeningCurly(true)
              .withPlaceClosingCurlyOnNewLine(true))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = {
                          1,
                          2,
                          3,
                          4,
                          5
                  };
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerDoNotWrap() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(DoNotWrap))
          ),
          java(
            """
              class Test {
                  int[] numbers = {
                    1, 
                    2, 
                    3, 
                    4, 
                    5
                  };
              }
              """,
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerChopIfTooLong() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withHardWrapAt(40)
              .withArrayInitializer(wrap.getArrayInitializer().withWrap(ChopIfTooLong))
          ),
          java(
            """
              class Test {
                  Object[] numbers = {1, 2, "SOME VERY LONG VALUE CAUSING THE LINE TO BE LONG", 4, 5};
              }
              """,
            """
              class Test {
                  Object[] numbers = {1,
                          2,
                          "SOME VERY LONG VALUE CAUSING THE LINE TO BE LONG",
                          4,
                          5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerChopIfTooLongNotLongEnough() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer().withWrap(ChopIfTooLong))
          ),
          java(
            """
              class Test {
                  int[] numbers = {1, 2, 3, 4, 5};
              }
              """
          )
        );
    }

    @Test
    void arrayInitializerWithNewArray() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withArrayInitializer(wrap.getArrayInitializer()
              .withWrap(WrapAlways)
              .withNewLineAfterOpeningCurly(true)
              .withPlaceClosingCurlyOnNewLine(true))
          ),
          java(
            """
              class Test {
                  int[] numbers = new int[]{1, 2, 3, 4, 5};
              }
              """,
            """
              class Test {
                  int[] numbers = new int[]{
                          1,
                          2,
                          3,
                          4,
                          5
                  };
              }
              """
          )
        );
    }

    @Test
    void annotationParametersWrapAlways() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(WrapAlways))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = "all",
                      justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersWrapAlwaysAlreadyCorrect() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(WrapAlways))
          ),
          java(
            """
              @SuppressWarnings(value = "all",
                      justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersWrapAlwaysAlignWhenMultiline() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(WrapAlways).withAlignWhenMultiline(true))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = "all",
                                justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersOpenNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(WrapAlways).withOpenNewLine(true))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """,
            """
              @SuppressWarnings(
                      value = "all",
                      justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersCloseNewLine() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(WrapAlways).withCloseNewLine(true))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = "all",
                      justification = "test"
              )
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersAllOptions() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters()
              .withWrap(WrapAlways)
              .withOpenNewLine(true)
              .withCloseNewLine(true))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """,
            """
              @SuppressWarnings(
                      value = "all",
                      justification = "test"
              )
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersDoNotWrap() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(DoNotWrap))
          ),
          java(
            """
              @SuppressWarnings(
                value = "all", 
                justification = "test"
              )
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersChopIfTooLong() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap
              .withHardWrapAt(60)
              .withAnnotationParameters(wrap.getAnnotationParameters().withWrap(ChopIfTooLong))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "this is a very long justification")
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = "all",
                      justification = "this is a very long justification")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void annotationParametersChopIfTooLongNotLongEnough() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters().withWrap(ChopIfTooLong))
          ),
          java(
            """
              @SuppressWarnings(value = "all", justification = "test")
              class Test {
              }
              """
          )
        );
    }

    @Test
    void standaloneWrappingAndBracesPreservesIndentation() {
        rewriteRun(
          spec -> spec.recipe(new WrappingAndBraces()),
          java(
            """
              class Test {
                  void method() {
                      System.out.println("hello");
                      int x = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void standaloneWrappingAndBracesPreservesIndentationWithLineComments() {
        rewriteRun(
          spec -> spec.recipe(new WrappingAndBraces()),
          java(
            """
              class Test {
                  // a comment
                  void method() {
                      // another comment
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void standaloneWrappingAndBracesPreservesIndentationWithMultilineComments() {
        rewriteRun(
          spec -> spec.recipe(new WrappingAndBraces()),
          java(
            """
              class Test {
                  /* a multiline
                     comment */
                  void method() {
                      System.out.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationParametersOnMethod() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters()
              .withWrap(WrapAlways)
              .withOpenNewLine(true)
              .withCloseNewLine(true))
          ),
          java(
            """
              class Test {
                  @SuppressWarnings(value = "all", justification = "test")
                  void method() {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings(
                          value = "all",
                          justification = "test"
                  )
                  void method() {
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationParametersOnField() {
        rewriteRun(
          autoFormat(
            spaces -> spaces,
            wrap -> wrap.withAnnotationParameters(wrap.getAnnotationParameters()
              .withWrap(WrapAlways)
              .withOpenNewLine(true)
              .withCloseNewLine(true))
          ),
          java(
            """
              class Test {
                  @SuppressWarnings(value = "all", justification = "test")
                  private int field;
              }
              """,
            """
              class Test {
                  @SuppressWarnings(
                          value = "all",
                          justification = "test"
                  )
                  private int field;
              }
              """
          )
        );
    }
}
