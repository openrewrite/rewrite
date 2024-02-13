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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({
  "InfiniteRecursion", "UnusedAssignment", "ConstantConditions", "StatementWithEmptyBody", "RedundantThrows",
  "UnusedLabel", "SwitchStatementWithTooFewBranches", "InfiniteLoopStatement", "rawtypes", "ResultOfMethodCallIgnored",
  "CodeBlock2Expr", "DuplicateThrows", "EmptyTryBlock", "CatchMayIgnoreException", "EmptyFinallyBlock",
  "PointlessBooleanExpression", "ClassInitializerMayBeStatic", "MismatchedReadAndWriteOfArray"
  , "TypeParameterExplicitlyExtendsObject"})
class TabsAndIndentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TabsAndIndents());
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<TabsAndIndentsStyle> with) {
        return spec -> spec.recipe(new TabsAndIndents())
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.tabsAndIndents()))
            )
          )));
    }

    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/2251")
    @Test
    void multilineCommentStartPositionIsIndented() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      if(true)
                          foo();
                          foo();
                        /*
                     line-one
                   line-two
                   */
                  }
                  static void foo() {}
              }
              """,
            """
              class A {
                  {
                      if(true)
                          foo();
                      foo();
                      /*
                 line-one
                line-two
                */
                  }
                  static void foo() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    void alignMethodDeclarationParamsWhenMultiple() {
        rewriteRun(
          java(
            """
              class Test {
                  @SuppressWarnings
                  private void firstArgNoPrefix(String first,
                          int times,
                          String third
                       ) {
                  }
                  private void firstArgOnNewLine(
                          String first,
                          int times,
                       String third
                       ) {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings
                  private void firstArgNoPrefix(String first,
                                                int times,
                                                String third
                  ) {
                  }
                  private void firstArgOnNewLine(
                          String first,
                          int times,
                          String third
                  ) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    void alignMethodDeclarationParamsWhenContinuationIndent() {
        rewriteRun(
          tabsAndIndents(style -> style.withMethodDeclarationParameters(new TabsAndIndentsStyle.MethodDeclarationParameters(false))),
          java(
            """
              class Test {
                  private void firstArgNoPrefix(String first,
                                                int times,
                                                String third) {
                  }
                  private void firstArgOnNewLine(
                                                 String first,
                                                 int times,
                                                 String third) {
                  }
              }
              """,
            """
              class Test {
                  private void firstArgNoPrefix(String first,
                          int times,
                          String third) {
                  }
                  private void firstArgOnNewLine(
                          String first,
                          int times,
                          String third) {
                  }
              }
              """
          )
        );
    }

    @Test
    void alignMethodDeclarationParamsWhenContinuationIndentUsingTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          java(
            """
            import java.util.*;
            
            class Foo {
            	Foo(
            			String var1,
            			String var2,
            			String var3
            	) {
            	}
            }
            """
          )
        );
    }

    // https://rules.sonarsource.com/java/tag/confusing/RSPEC-3973
    @DocumentExample
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Test
    void rspec3973() {
        rewriteRun(
          java(
            """
              class Test {{
                  if (true == false)
                  doTheThing();
                            
                  doTheOtherThing();
                  somethingElseEntirely();
                            
                  foo();
              }
                  public static void doTheThing() {}
                  public static void doTheOtherThing() {}
                  public static void somethingElseEntirely() {}
                  public static void foo() {}
              }
              """,
            """
              class Test {{
                  if (true == false)
                      doTheThing();
                            
                  doTheOtherThing();
                  somethingElseEntirely();
                            
                  foo();
              }
                  public static void doTheThing() {}
                  public static void doTheOtherThing() {}
                  public static void somethingElseEntirely() {}
                  public static void foo() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/623")
    @Test
    void ifElseWithComments() {
        rewriteRun(
          java(
            """
              public class B {
                  void foo(int input) {
                      // First case
                      if (input == 0) {
                          // do things
                      }
                      // Second case
                      else if (input == 1) {
                          // do things
                      }
                      // Otherwise
                      else {
                          // do other things
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationArguments() {
        rewriteRun(
          java(
            """
              class Test {
                  @SuppressWarnings({
                          "unchecked",
                          "ALL"
                  })
                  String id;
              }
              """
          )
        );
    }

    @Test
    void methodChain() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          java(
            """
              class Test {
                  void method(Test t) {
                      this
                        .method(
                          t
                        );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentOnOpeningLineWithMethodSelect() {
        rewriteRun(
          java(
            """
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }

                  void method(Test t) {
                      t = t.withData(withData()
                                      .withData()
                                      .withData(),
                              withData()
                                      .withData()
                                      .withData()
                      );
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentOnNewLineWithMethodSelect() {
        rewriteRun(
          java(
            """
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }

                  void method(Test t) {
                      t = t.withData(
                              withData(), withData()
                                      .withData()
                                      .withData()
                      );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentsWithMethodSelectsOnEachNewLine() {
        rewriteRun(
          java(
            """
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }
              
                  void method(Test t) {
                      t = t.withData(withData()
                              .withData(t
                                      .
                                              withData()
                              )
                              .withData(
                                      t
                                              .
                                                      withData()
                              )
                      );
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentsContinuationIndentsAssorted() {
        rewriteRun(
          java(
            """
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }

                  void method(Test t) {
                      t = t.withData(withData()
                                      .withData(
                                              t.withData()
                                      ).withData(
                                      t.withData()
                                      )
                                      .withData(),
                              withData()
                      );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    @Test
    void methodInvocationLambdaBlockWithClosingBracketOnSameLineIndent() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }

                  void method(Test t, Collection<String> c) {
                      t = t.withData(c.stream().map(a -> {
                          if (!a.isEmpty()) {
                              return a.toLowerCase();
                          }
                          return a;
                      }));
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    void methodInvocationLambdaBlockWithClosingBracketOnNewLineIndent() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                  Test withData(Object... arg0) {
                      return this;
                  }

                  void method(Test t, Collection<String> c) {
                      t = t.withData(c.stream().map(a -> {
                                  if (!a.isEmpty()) {
                                      return a.toLowerCase();
                                  }
                                  return a;
                              }
                      ));
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1173")
    @Test
    void methodInvocationLambdaBlockOnSameLine() {
        rewriteRun(
          java(
            """
              import java.util.function.Predicate;

              class SomeUtility {
                  static boolean test(String property, Predicate<String> test) {
                      return false;
                  }
              }
              """
          ),
          java(
            """
              class Test {

                  void method() {
                      SomeUtility.test(
                              "hello", s -> {
                                  return true;
                              });
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    @Test
    void lambdaBodyWithNestedMethodInvocationLambdaStatementBodyIndent() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.stream.Collectors;
              class Test {
                  void method(Collection<List<String>> c) {
                      c.stream().map(x -> x.stream().max((r1, r2) -> {
                                  return 0;
                              })
                      )
                      .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    void lambdaBodyWithNestedMethodInvocationLambdaExpressionBodyIndent() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.stream.Collectors;
              class Test {
                  void method(Collection<List<String>> c) {
                      c.stream().map(x -> x.stream().max((r1, r2) ->
                                      0
                              )
                      )
                      .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    @Test
    void methodInvocationLambdaArgumentIndent() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;

              abstract class Test {
                  abstract Test a(Function<String, String> f);

                  void method(String s) {
                      a(
                              f -> s.toLowerCase()
                      );
                  }
              }
              """
          )
        );
    }

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @SuppressWarnings("EnhancedSwitchMigration")
    @Test
    void tabsAndIndents() {
        rewriteRun(
          java("public interface I1{}"),
          java("public interface I2{}"),
          java("public class E1 extends Exception{}"),
          java("public class E2 extends Exception{}"),
          java(
            """
              public class Test {
              public int[] X = new int[]{1, 3, 5, 7, 9, 11};
              public void doSomething(int i) {}
              public void doCase0() {}
              public void doDefault() {}
              public void processException(Object a, Object b, Object c, Object d) {}
              public void processFinally() {}
              public void test(boolean a, int x, int y, int z) {
              label1:
              do {
              try {
              if (x > 0) {
              int someVariable = a ? x : y;
              int anotherVariable = a ? x : y;
              } else if (x < 0) {
              int someVariable = (y + z);
              someVariable = x = x + y;
              } else {
              label2:
              for (int i = 0; i < 5; i++) doSomething(i);
              }
              switch (a) {
              case 0:
              doCase0();
              break;
              default:
              doDefault();
              }
              } catch (Exception e) {
              processException(e.getMessage(), x + y, z, a);
              } finally {
              processFinally();
              }
              }
              while (true);

              if (2 < 3) return;
              if (3 < 4) return;
              do {
              x++;
              }
              while (x < 10000);
              while (x < 50000) x++;
              for (int i = 0; i < 5; i++) System.out.println(i);
              }

              private class InnerClass implements I1, I2 {
              public void bar() throws E1, E2 {
              }
              }
              }
              """,
            """
              public class Test {
                  public int[] X = new int[]{1, 3, 5, 7, 9, 11};
                  public void doSomething(int i) {}
                  public void doCase0() {}
                  public void doDefault() {}
                  public void processException(Object a, Object b, Object c, Object d) {}
                  public void processFinally() {}
                  public void test(boolean a, int x, int y, int z) {
                      label1:
                      do {
                          try {
                              if (x > 0) {
                                  int someVariable = a ? x : y;
                                  int anotherVariable = a ? x : y;
                              } else if (x < 0) {
                                  int someVariable = (y + z);
                                  someVariable = x = x + y;
                              } else {
                                  label2:
                                  for (int i = 0; i < 5; i++) doSomething(i);
                              }
                              switch (a) {
                                  case 0:
                                      doCase0();
                                      break;
                                  default:
                                      doDefault();
                              }
                          } catch (Exception e) {
                              processException(e.getMessage(), x + y, z, a);
                          } finally {
                              processFinally();
                          }
                      }
                      while (true);

                      if (2 < 3) return;
                      if (3 < 4) return;
                      do {
                          x++;
                      }
                      while (x < 10000);
                      while (x < 50000) x++;
                      for (int i = 0; i < 5; i++) System.out.println(i);
                  }

                  private class InnerClass implements I1, I2 {
                      public void bar() throws E1, E2 {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          java(
            """
              public class Test {
              public void test(boolean a, int x, int y) {
              try {
              int someVariable = a ? x : y;
              } catch (Exception e) {
              e.printStackTrace();
              } finally {
              a = false;
              }
              }
              }
              """,
            """
              public class Test {
                  public void test(boolean a, int x, int y) {
                      try {
                          int someVariable = a ? x : y;
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          a = false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhile() {
        rewriteRun(
          java(
            """
              public class Test {
              public void test() {
              do {
              }
              while(true);

              labeled: do {
              }
              while(false);
              }
              }
              """,
            """
              public class Test {
                  public void test() {
                      do {
                      }
                      while(true);

                      labeled: do {
                      }
                      while(false);
                  }
              }
              """
          )
        );
    }

    @Test
    void elseBody() {
        rewriteRun(
          java(
            """
              public class Test {
              public void test(boolean a, int x, int y, int z) {
              if (x > 0) {
              } else if (x < 0) {
              y += z;
              }
              }
              }
              """,
            """
              public class Test {
                  public void test(boolean a, int x, int y, int z) {
                      if (x > 0) {
                      } else if (x < 0) {
                          y += z;
                      }
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void forLoop() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          java(
            """
              public class Test {
                  public void test() {
                  int m = 0;
                  int n = 0;
                  for (
                   int i = 0
                   ;
                   i < 5
                   ;
                   i++, m++, n++
                  );
                  for (int i = 0;
                   i < 5;
                   i++, m++, n++);
                  labeled: for (int i = 0;
                   i < 5;
                   i++, m++, n++);
                  }
              }
              """,
            """
              public class Test {
                  public void test() {
                      int m = 0;
                      int n = 0;
                      for (
                        int i = 0
                        ;
                        i < 5
                        ;
                        i++, m++, n++
                      );
                      for (int i = 0;
                           i < 5;
                           i++, m++, n++);
                      labeled: for (int i = 0;
                                    i < 5;
                                    i++, m++, n++);
                  }
              }
              """
          )
        );
    }

    @Test
    void methodDeclaration() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          java(
            """
              public class Test {
                  public void test(int a,
                                   int b) {}

                  public void test2(
                    int a,
                    int b) {}
              }
              """
          )
        );
    }

    @Test
    void lineComment() {
        rewriteRun(
          java(
            """
              public class A {
                // comment at indent 2
              public void method() {}
              }
              """,
            """
              public class A {
                  // comment at indent 2
                  public void method() {}
              }
              """
          )
        );
    }

    @Test
    void noIndexOutOfBoundsUsingSpaces() {
        rewriteRun(
          java(
            """
              public class A {
                // length = 1 from new line.
                    int valA = 10; // text.length = 1 + shift -2 == -1.
              }
              """,
            """
              public class A {
                  // length = 1 from new line.
                  int valA = 10; // text.length = 1 + shift -2 == -1.
              }
              """
          )
        );
    }

    @Test
    void noIndexOutOfBoundsUsingTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true).withTabSize(1).withIndentSize(1)),
          java(
            """
              class Test {
              	void test() {
              		System.out.println(); // comment
              	}
              }
              """
          )
        );
    }

    @Test
    void blockComment() {
        rewriteRun(
          java(
            """
              public class A {
              /*a
                b*/
              public void method() {}
              }
              """,
            """
              public class A {
                  /*a
                    b*/
                  public void method() {}
              }
              """
          )
        );
    }

    @SuppressWarnings("TextBlockMigration")
    @Test
    void blockCommentCRLF() {
        rewriteRun(
          java(
            "public class A {\r\n" +
            "/*a\r\n" +
            "  b*/\r\n" +
            "public void method() {}\r\n" +
            "}",
            "public class A {\r\n" +
            "    /*a\r\n" +
            "      b*/\r\n" +
            "    public void method() {}\r\n" +
            "}"
          )
        );
    }

    @SuppressWarnings("EmptyClassInitializer")
    @Test
    void initBlocks() {
        rewriteRun(
          java(
            """
              class Test {
                  static {
                      System.out.println("hi");
                  }
                  
                  {
                  }
              }
              """
          )
        );
    }

    @Test
    void moreAnnotations() {
        rewriteRun(
          spec -> spec.afterTypeValidationOptions(TypeValidation.none()),
          java(
            """
              import lombok.EqualsAndHashCode;
              import java.util.UUID;
              class Test {
                  @SuppressWarnings(
                          value = "unchecked"
                  )
                  @EqualsAndHashCode.Include
                  UUID id;
              }
              """
          )
        );
    }

    @Test
    void annotations() {
        rewriteRun(
          java(
            """
              @Deprecated
              @SuppressWarnings("ALL")
              public class A {
              @Deprecated
              @SuppressWarnings("ALL")
                  class B {
                  }
              }
              """,
            """
              @Deprecated
              @SuppressWarnings("ALL")
              public class A {
                  @Deprecated
                  @SuppressWarnings("ALL")
                  class B {
                  }
              }
              """
          )
        );
    }

    @Test
    void javadoc() {
        rewriteRun(
          java(
            """
              public class A {
              /**
                      * This is a javadoc
                          */
                  public void method() {}
              }
              """,
            """
              public class A {
                  /**
                   * This is a javadoc
                   */
                  public void method() {}
              }
              """
          )
        );
    }

    @Test
    void tabs() {
        rewriteRun(
          // TIP: turn on "Show Whitespaces" in the IDE to see this test clearly
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          java(
            """
              public class A {
              	public void method() {
              	int n = 0;
              	}
              }
              """,
            """
              public class A {
              	public void method() {
              		int n = 0;
              	}
              }
              """
          )
        );
    }

    @Test
    void shiftRight() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void test(boolean a, int x, int y) {
                      try {
                  int someVariable = a ? x : y;
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          a = false;
                      }
                  }
              }
              """,
            """
              public class Test {
                  public void test(boolean a, int x, int y) {
                      try {
                          int someVariable = a ? x : y;
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          a = false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shiftRightTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          java(
            """
              public class Test {
              	public void test(boolean a, int x, int y) {
              		try {
              	int someVariable = a ? x : y;
              		} catch (Exception e) {
              			e.printStackTrace();
              		} finally {
              			a = false;
              		}
              	}
              }
              """,
            """
              public class Test {
              	public void test(boolean a, int x, int y) {
              		try {
              			int someVariable = a ? x : y;
              		} catch (Exception e) {
              			e.printStackTrace();
              		} finally {
              			a = false;
              		}
              	}
              }
              """
          )
        );
    }

    @Test
    void shiftLeft() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void test(boolean a, int x, int y) {
                      try {
                                              int someVariable = a ? x : y;
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          a = false;
                      }
                  }
              }
              """,
            """
              public class Test {
                  public void test(boolean a, int x, int y) {
                      try {
                          int someVariable = a ? x : y;
                      } catch (Exception e) {
                          e.printStackTrace();
                      } finally {
                          a = false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shiftLeftTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          java(
            """
              public class Test {
              	public void test(boolean a, int x, int y) {
              		try {
              				int someVariable = a ? x : y;
              		} catch (Exception e) {
              			e.printStackTrace();
              		} finally {
              			a = false;
              		}
              	}
              }
              """,
            """
              public class Test {
              	public void test(boolean a, int x, int y) {
              		try {
              			int someVariable = a ? x : y;
              		} catch (Exception e) {
              			e.printStackTrace();
              		} finally {
              			a = false;
              		}
              	}
              }
              """
          )
        );
    }

    @Test
    void nestedIfElse() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      if (true) { // comment
                          if (true) {
                          } else {
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationOnSameLine() {
        rewriteRun(
          java(
            """
              class Test {
                  @Deprecated int method() {
                      return 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void newClassAsMethodArgument() {
        rewriteRun(
          java(
            """
              class Test {
                  Test(String s, int m) {
                  }
                            
                  void method(Test t) {
                      method(new Test("hello" +
                              "world",
                              1));
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentsThatDontStartOnNewLine() {
        rewriteRun(
          java(
            """
              import java.io.File;
              class Test {
                  void method(int n, File f, int m, int l) {
                      method(n, new File(
                                      "test"
                              ),
                              m,
                              l);
                  }
                            
                  void method2(int n, File f, int m) {
                      method(n, new File(
                                      "test"
                              ), m,
                              0);
                  }
                            
                  void method3(int n, File f) {
                      method2(n, new File(
                              "test"
                      ), 0);
                  }
                            
                  void method4(int n) {
                      method3(n, new File(
                              "test"
                      ));
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentsThatDontStartOnNewLine2() {
        rewriteRun(
          java(
            """
              class Test {
                  int method5(int n, int m) {
                      method5(1,
                              2);
                      return method5(method5(method5(method5(3,
                              4),
                              5),
                              6),
                              7);
                  }
              }
              """
          )
        );
    }

    @Test
    void identAndFieldAccess() {
        rewriteRun(

          java(
            """
              import java.util.stream.Stream;
              class Test {
                  Test t = this;
                  Test method(Stream n, int m) {
                      this.t.t
                              .method(null, 1)
                              .t
                              .method(null, 2);
                      Stream
                              .of("a");
                      method(Stream
                                      .of("a"),
                              3
                      );
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void lambda() {
        rewriteRun(
          java(
            """
              import java.util.function.Supplier;
              public class Test {
                  public void method(int n) {
                      Supplier<Integer> ns = () ->
                          n;
                  }
              }
              """,
            """
              import java.util.function.Supplier;
              public class Test {
                  public void method(int n) {
                      Supplier<Integer> ns = () ->
                              n;
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaWithBlock() {
        rewriteRun(
          java(
            """
              import java.util.function.Supplier;
              class Test {
                  void method(Supplier<String> s, int n) {
                      method(() -> {
                                  return "hi";
                              },
                              n);
                  }
              }
              """
          )
        );
    }

    @Test
    void enums() {
        rewriteRun(
          java(
            """
              enum Scope {
                  None, // the root of a resolution tree
                  Compile
              }
              """
          )
        );
    }

    @Test
    void twoThrows() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
              class Test {
                  void method() throws IOException,
                          Exception {
                  }
                  
                  void method2()
                          throws IOException,
                          Exception {
                  }
              }
               """
          )
        );
    }

    @Test
    void twoTypeParameters() {
        rewriteRun(
          java("interface A {}"),
          java("interface B{}"),
          java(
            """
              class Test<A,
                      B> {
              }
              """
          )
        );
    }

    @Test
    void twoImplements() {
        rewriteRun(
          java("interface A {}"),
          java("interface B{}"),
          java(
            """
              class Test implements A,
                      B {
              }
              """
          )
        );
    }

    @Test
    void fieldsWhereClassHasAnnotation() {
        rewriteRun(
          java(
            """
              @Deprecated
              class Test {
                  String groupId;
                  String artifactId;
              }
              """
          )
        );
    }

    @Test
    void methodWithAnnotation() {
        rewriteRun(
          java(
            """
              class Test {
                  @Deprecated
                  @SuppressWarnings("all")
              String getOnError() {
                      return "uh oh";
                  }
              }
              """,
            """
              class Test {
                  @Deprecated
                  @SuppressWarnings("all")
                  String getOnError() {
                      return "uh oh";
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"CStyleArrayDeclaration", "EnhancedSwitchMigration"})
    @Test
    void containers() {
        rewriteRun(
          java(
            """
              import java.io.ByteArrayInputStream;
              import java.io.InputStream;
              import java.io.Serializable;
              import java.lang.annotation.Retention;
              @Retention
              (value = "1.0")
              public
              class
              Test
              <T
              extends Object>
              implements
              Serializable {
                  Test method
                  ()
                  throws Exception {
                      try
                      (InputStream is = new ByteArrayInputStream(new byte[0])) {}
                      int n[] = 
                      {0};
                      switch (1) {
                      case 1:
                      n
                      [0]++;
                      }
                      return new Test
                      ();
                  }
              }
              """,
            """
              import java.io.ByteArrayInputStream;
              import java.io.InputStream;
              import java.io.Serializable;
              import java.lang.annotation.Retention;
              @Retention
                      (value = "1.0")
              public
              class
              Test
                      <T
                              extends Object>
                      implements
                      Serializable {
                  Test method
                          ()
                          throws Exception {
                      try
                              (InputStream is = new ByteArrayInputStream(new byte[0])) {}
                      int n[] = 
                              {0};
                      switch (1) {
                          case 1:
                              n
                                      [0]++;
                      }
                      return new Test
                              ();
                  }
              }
              """
          )
        );
    }

    @Test
    void methodInvocations() {
        rewriteRun(
          java(
            """
              class Test {
                  Test method(int n) {
                      return method(n)
                              .method(n)
                              .method(n);
                  }
                            
                  Test method2() {
                      return method2().
                              method2().
                              method2();
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaries() {
        rewriteRun(
          java(
            """
              public class Test {
                  public Test method(int n) {
                      return n > 0 ?
                          this :
                          method(n).method(n);
                  }
              }
              """,
            """
              public class Test {
                  public Test method(int n) {
                      return n > 0 ?
                              this :
                              method(n).method(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void newClassAsArgument() {
        rewriteRun(
          java(
            """
              import java.io.File;
              class Test {
                  void method(int m, File f, File f2) {
                      method(m, new File(
                                      "test"
                              ),
                              new File("test",
                                      "test"
                              ));
                  }
              }
              """
          )
        );
    }

    @Test
    void variableWithAnnotation() {
        rewriteRun(
          java(
            """
              public class Test {
                  @Deprecated
                  final String scope;
                            
                  @Deprecated
                  String classifier;
              }
              """
          )
        );
    }

    @Test
    void lambdaMethodParameter2() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;

              abstract class Test {
                  abstract Test a(Function<Test, Test> f);
                  abstract Test b(Function<Test, Test> f);
                  abstract Test c(Function<Test, Test> f);

                  Test method(Function<Test, Test> f) {
                      return a(f)
                              .b(
                                      t ->
                                              c(f)
                              );
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaMethodParameter() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;
              abstract class Test {
                  abstract Test a(Function<Test, Test> f);
                  abstract Test b(Function<Test, Test> f);
                  abstract Test c(Function<Test, Test> f);
                  
                  Test method(Function<Test, Test> f) {
                      return a(f)
                              .b(t ->
                                      c(f)
                              );
                  }
              }
              """
          )
        );
    }

    @Test
    void failure1() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              public class Test {
                  public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system, // comments here
                                                                                          @Nullable File localRepositoryDir) {
                      DefaultRepositorySystemSession repositorySystemSession = org.apache.maven.repository.internal.MavenRepositorySystemUtils
                              .newSession();
                      repositorySystemSession.setDependencySelector(
                              new AndDependencySelector(
                                      new ExclusionDependencySelector(), // some comments
                                      new ScopeDependencySelector(emptyList(), Arrays.asList("provided", "test")),
                                      // more comments
                                      new OptionalDependencySelector()
                              )
                      );
                      return repositorySystemSession;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("DuplicateCondition")
    @Test
    void methodInvocationsNotContinuationIndentedWhenPartOfBinaryExpression() {
        rewriteRun(
          java(
            """
              import java.util.stream.Stream;
              public class Test {
                  boolean b;
                  public Stream<Test> method() {
                      if (b && method()
                              .anyMatch(t -> b ||
                                      b
                              )) {
                          // do nothing
                      }
                      return Stream.of(this);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("CStyleArrayDeclaration")
    @Test
    void punctuation() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          java(
            """
              import java.util.function.Function;
              public class Test {
              int X[];
              public int plus(int x) {
                  return 0;
              }
              public void test(boolean a, int x, int y) {
              Function<Integer, Integer> op = this
              ::
              plus;
              if (x
              >
              0) {
              int someVariable = a ?
              x :
              y;
              int anotherVariable = a
              ?
              x
              :
              y;
              }
              x
              ++;
              X
              [
              1
              ]
              =
              0;
              }
              }
              """,
            """
              import java.util.function.Function;
              public class Test {
                  int X[];
                  public int plus(int x) {
                      return 0;
                  }
                  public void test(boolean a, int x, int y) {
                      Function<Integer, Integer> op = this
                        ::
                        plus;
                      if (x
                        >
                        0) {
                          int someVariable = a ?
                            x :
                            y;
                          int anotherVariable = a
                            ?
                            x
                            :
                            y;
                      }
                      x
                        ++;
                      X
                        [
                        1
                        ]
                        =
                        0;
                  }
              }
              """
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(
            """
              class Test {
                  Test(Test t) {}
                  Test() {}
                  void method(Test t) {
                      method(
                          new Test(
                              new Test()
                          )
                      );
                  }
              }
              """,
            """
              class Test {
                  Test(Test t) {}
                  Test() {}
                  void method(Test t) {
                      method(
                              new Test(
                                      new Test()
                              )
                      );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/642")
    @Test
    void alignLineComments() {
        rewriteRun(
          java(
            """
                      // shift left.
              package org.openrewrite; // trailing comment.
                            
                      // shift left.
                      public class A { // trailing comment at class.
                // shift right.
                      // shift left.
                              public int method(int value) { // trailing comment at method.
                  // shift right.
                          // shift left.
                  if (value == 1) { // trailing comment at if.
                // suffix contains new lines with whitespace.
                      
                      
                      // shift right.
                                   // shift left.
                              value += 10; // trailing comment.
                      // shift right at end of block.
                              // shift left at end of block.
                                      } else {
                          value += 30;
                      // shift right at end of block.
                              // shift left at end of block.
                 }
                            
                              if (value == 11)
                      // shift right.
                              // shift left.
                          value += 1;
                            
                  return value;
                  // shift right at end of block.
                          // shift left at end of block.
                          }
                // shift right at end of block.
                      // shift left at end of block.
                          }
              """,
            """
              // shift left.
              package org.openrewrite; // trailing comment.
                            
              // shift left.
              public class A { // trailing comment at class.
                  // shift right.
                  // shift left.
                  public int method(int value) { // trailing comment at method.
                      // shift right.
                      // shift left.
                      if (value == 1) { // trailing comment at if.
                          // suffix contains new lines with whitespace.
                      
                      
                          // shift right.
                          // shift left.
                          value += 10; // trailing comment.
                          // shift right at end of block.
                          // shift left at end of block.
                      } else {
                          value += 30;
                          // shift right at end of block.
                          // shift left at end of block.
                      }
                            
                      if (value == 11)
                          // shift right.
                          // shift left.
                          value += 1;
                            
                      return value;
                      // shift right at end of block.
                      // shift left at end of block.
                  }
                  // shift right at end of block.
                  // shift left at end of block.
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    void alignMultipleBlockCommentsOnOneLine() {
        rewriteRun(
          java(
            """
              public class A {
                  public void method() {
                              /* comment 1 */ /* comment 2 */ /* comment 3 */
                  }
              }
              """,
            """
              public class A {
                  public void method() {
                      /* comment 1 */ /* comment 2 */ /* comment 3 */
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    void alignMultipleBlockComments() {
        rewriteRun(
          java(
            """
              public class A {
              /* Preserve whitespace
                 alignment */
              
                     /* Shift next blank line left
              
                      * This line should be aligned
                      */
              
              /* This comment
               * should be aligned */
              public void method() {}
              }
              """,
            """
              public class A {
                  /* Preserve whitespace
                     alignment */
              
                  /* Shift next blank line left
              
                   * This line should be aligned
                   */
              
                  /* This comment
                   * should be aligned */
                  public void method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/641")
    @Test
    void alignTryCatchFinally() {
        rewriteRun(
          java(
            """
              public class Test {
                  public void method() {
                      // inline try, catch, finally.
                      try {
                            
                      } catch (Exception ex) {
                            
                      } finally {
                            
                      }
                            
                      // new line try, catch, finally.
                      try {
                            
                      }
                      catch (Exception ex) {
                            
                      }
                      finally {
                            
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/663")
    @Test
    void alignBlockPrefixes() {
        rewriteRun(
          spec -> spec.recipe(new AutoFormat()),
          java(
            """
              public class Test {
                              
                  public void practiceA()
                  {
                      for (int i = 0; i < 10; ++i)
                      {
                          if (i % 2 == 0)
                          {
                              try
                              {
                                  Integer value = Integer.valueOf("100");
                              }
                              catch (Exception ex)
                              {
                                  throw new RuntimeException();
                              }
                              finally
                              {
                                  System.out.println("out");
                              }
                          }
                      }
                  }
                              
                  public void practiceB() {
                      for (int i = 0; i < 10; ++i) {
                          if (i % 2 == 0) {
                              try {
                                  Integer value = Integer.valueOf("100");
                              } catch (Exception ex) {
                                  throw new RuntimeException();
                              } finally {
                                  System.out.println("out");
                              }
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void alignInlineBlockComments() {
        rewriteRun(
          java(
            """
              public class WhitespaceIsHard {
              /* align comment */ public void method() { /* tricky */
              /* align comment */ int var = 10; /* tricky */
              // align comment and end paren.
              }
              }
              """,
            """
              public class WhitespaceIsHard {
                  /* align comment */ public void method() { /* tricky */
                      /* align comment */ int var = 10; /* tricky */
                      // align comment and end paren.
                  }
              }
              """
          )
        );
    }

    @Test
    void trailingMultilineString() {
        rewriteRun(
          java(
            """
              public class WhitespaceIsHard {
                  public void method() { /* tricky */
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
    @Test
    void javaDocsWithMultipleLeadingAsterisks() {
        rewriteRun(
          java(
            """
                  /******** Align JavaDoc with multiple leading '*' in margin left.
                   **** Align left
                   */
              public class Test {
              /******** Align JavaDoc with multiple leading '*' in margin right.
               **** Align right
               */
                  void method() {
                  }
              }
              """,
            """
              /******** Align JavaDoc with multiple leading '*' in margin left.
               **** Align left
               */
              public class Test {
                  /******** Align JavaDoc with multiple leading '*' in margin right.
                   **** Align right
                   */
                  void method() {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("TextBlockMigration")
    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    void alignJavaDocsWithCRLF() {
        rewriteRun(
          java(
            "        /**\r\n" +
            "         * Align JavaDoc left that starts on 2nd line.\r\n" +
            "         */\r\n" +
            "public class A {\r\n" +
            "/** Align JavaDoc right that starts on 1st line.\r\n" +
            "  * @param value test value.\r\n" +
            "  * @return value + 1 */\r\n" +
            "        public int methodOne(int value) {\r\n" +
            "            return value + 1;\r\n" +
            "        }\r\n" +
            "\r\n" +
            "                /** Edge case formatting test.\r\n" +
            "   @param value test value.\r\n" +
            "                 @return value + 1\r\n" +
            "                 */\r\n" +
            "        public int methodTwo(int value) {\r\n" +
            "            return value + 1;\r\n" +
            "        }\r\n" +
            "}"
            ,
            "/**\r\n" +
            " * Align JavaDoc left that starts on 2nd line.\r\n" +
            " */\r\n" +
            "public class A {\r\n" +
            "    /** Align JavaDoc right that starts on 1st line.\r\n" +
            "     * @param value test value.\r\n" +
            "     * @return value + 1 */\r\n" +
            "    public int methodOne(int value) {\r\n" +
            "        return value + 1;\r\n" +
            "    }\r\n" +
            "\r\n" +
            "    /** Edge case formatting test.\r\n" +
            "     @param value test value.\r\n" +
            "     @return value + 1\r\n" +
            "     */\r\n" +
            "    public int methodTwo(int value) {\r\n" +
            "        return value + 1;\r\n" +
            "    }\r\n" +
            "}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    void alignJavaDocs() {
        rewriteRun(
          java(
            """
                      /**
                       * Align JavaDoc left that starts on 2nd line.
                       */
              public class A {
              /** Align JavaDoc right that starts on 1st line.
                * @param value test value.
                * @return value + 1 */
                      public int methodOne(int value) {
                          return value + 1;
                      }
                            
                              /** Edge case formatting test.
                 @param value test value.
                               @return value + 1
                               */
                      public int methodTwo(int value) {
                          return value + 1;
                      }
              }
              """,
            """
              /**
               * Align JavaDoc left that starts on 2nd line.
               */
              public class A {
                  /** Align JavaDoc right that starts on 1st line.
                   * @param value test value.
                   * @return value + 1 */
                  public int methodOne(int value) {
                      return value + 1;
                  }
                            
                  /** Edge case formatting test.
                   @param value test value.
                   @return value + 1
                   */
                  public int methodTwo(int value) {
                      return value + 1;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/709")
    @Test
    void useContinuationIndentExtendsOnNewLine() {
        rewriteRun(
          java("package org.a; public class A {}"),
          java(
            """
              package org.b;
              import org.a.A;
              class B
                      extends A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/883")
    @Test
    void alignIdentifierOnNewLine() {
        rewriteRun(
          java("package org.a; public class A {}"),
          java(
            """
              package org.b;
              import org.a.A;
              class B extends
                      A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1526")
    @Test
    void doNotFormatSingleLineCommentAtCol0() {
        rewriteRun(
          java(
            """
              class A {
              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
              // DOES shift the suffix of comment 2.
              void shiftRight() {}
              }
              """,
            """
              class A {
              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
              // DOES shift the suffix of comment 2.
                  void shiftRight() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2968")
    @Test
    void recordComponents() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              public record RenameRequest(
                  @NotBlank
                  @JsonProperty("name") String name) {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3089")
    @Test
    void enumConstants() {
        rewriteRun(
          java(
            """
              public enum WorkflowStatus {
                  @SuppressWarnings("ALL")
                  VALUE1
              }
              """
          )
        );
    }

    @Test
    void longMethodChainWithMultiLineParameters_Correct() {
        rewriteRun(
          java(
            """
              class Foo {
                  String test() {
                      return "foobar"
                              .substring(
                                      1
                              ).substring(
                                      2
                              ).substring(
                                      3
                              );
                  }
              }
              """
          )
        );
    }

    @Test
    void longMethodChainWithMultiLineParameters_Incorrect() {
        rewriteRun(
          java(
            """
              class Foo {
              String test() {
              return "foobar"
              .substring(
              1
              ).substring(
              2
              ).substring(
              3
              );
              }
              }
              """,
            """
              class Foo {
                  String test() {
                      return "foobar"
                              .substring(
                                      1
                              ).substring(
                                      2
                              ).substring(
                                      3
                              );
                  }
              }
              """
          )
        );
    }
}
