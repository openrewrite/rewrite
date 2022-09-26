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
package org.openrewrite.java.format

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.java.style.TabsAndIndentsStyle
import org.openrewrite.style.NamedStyles

@Suppress("InfiniteRecursion", "UnusedAssignment", "ConstantConditions", "StatementWithEmptyBody", "RedundantThrows",
    "UnusedLabel", "SwitchStatementWithTooFewBranches", "InfiniteLoopStatement", "rawtypes", "ResultOfMethodCallIgnored",
    "CodeBlock2Expr", "DuplicateThrows", "EmptyTryBlock", "CatchMayIgnoreException", "EmptyFinallyBlock",
    "PointlessBooleanExpression", "ClassInitializerMayBeStatic", "MismatchedReadAndWriteOfArray"
)
interface TabsAndIndentsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = TabsAndIndents()

    fun tabsAndIndents(with: TabsAndIndentsStyle.() -> TabsAndIndentsStyle = { this }) = listOf(
        NamedStyles(
            randomId(), "test", "test", "test", emptySet(), listOf(
            IntelliJ.tabsAndIndents().run { with(this) })
        )
    )

    @Suppress("SuspiciousIndentAfterControlStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/2251")
    @Test
    fun multilineCommentStartPositionIsIndented(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                {
                    if(true)
                        foo();
                        foo();
                      /*
                   * line-one
                   * line-two
                   */
                }
                static void foo() {}
            }
        """,
        after = """
            class A {
                {
                    if(true)
                        foo();
                    foo();
                    /*
                 * line-one
                 * line-two
                   */
                }
                static void foo() {}
            }
        """
    )
    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    fun alignMethodDeclarationParamsWhenMultiple(jp: JavaParser) = assertChanged(
        jp,
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    fun alignMethodDeclarationParamsWhenContinuationIndent(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withMethodDeclarationParameters(
            TabsAndIndentsStyle.MethodDeclarationParameters(false)) }).build(),
        before = """
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
        after = """
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

    // https://rules.sonarsource.com/java/tag/confusing/RSPEC-3973
    @Test
    fun rspec3973(jp: JavaParser) = assertChanged(
        jp,
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/623")
    @Test
    fun ifElseWithComments(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    fun annotationArguments(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            class Test {
                @SuppressWarnings({
                        "unchecked",
                        "ALL"
                })
                String id;
            }
        """
    )

    @Test
    fun methodChain(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    fun methodInvocationArgumentOnOpeningLineWithMethodSelect(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Disabled("https://github.com/openrewrite/rewrite/issues/636")
    fun methodInvocationArgumentOnNewLineWithMethodSelect(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    fun methodInvocationArgumentsWithMethodSelectsOnEachNewLine(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Disabled("https://github.com/openrewrite/rewrite/issues/636")
    fun methodInvocationArgumentsContinuationIndentsAssorted(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    fun methodInvocationLambdaBlockWithClosingBracketOnSameLineIndent(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    fun methodInvocationLambdaBlockWithClosingBracketOnNewLineIndent(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1173")
    fun methodInvocationLambdaBlockOnSameLine(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        dependsOn = arrayOf("""
            import java.util.function.Predicate;

            class SomeUtility {
                static boolean test(String property, Predicate<String> test) {
                    return false;
                }
            }
        """.trimIndent()),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    fun lambdaBodyWithNestedMethodInvocationLambdaStatementBodyIndent(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    fun lambdaBodyWithNestedMethodInvocationLambdaExpressionBodyIndent(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    fun methodInvocationLambdaArgumentIndent(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @Test
    fun tabsAndIndents(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        dependsOn = arrayOf(
            "public interface I1{}",
            "public interface I2{}",
            "public class E1 extends Exception{}",
            "public class E2 extends Exception{}"
        ),
        before = """
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
        after = """
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

    @Test
    fun tryCatchFinally(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Test
    fun doWhile(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Test
    fun elseBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class Test {
            public void test(boolean a, int x, int y, int z) {
            if (x > 0) {
            } else if (x < 0) {
            y += z;
            }
            }
            }
        """,
        after = """
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

    @Test
    @Disabled
    fun forLoop(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
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
        after = """
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

    @Test
    fun methodDeclaration(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
            public class Test {
                public void test(int a,
                                 int b) {}

                public void test2(
                  int a,
                  int b) {}
            }
        """
    )

    @Test
    fun lineComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
              // comment at indent 2
            public void method() {}
            }
        """,
        after = """
            public class A {
                // comment at indent 2
                public void method() {}
            }
        """
    )

    @Test
    fun noIndexOutOfBoundsUsingSpaces(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
              // length = 1 from new line.
                  int valA = 10; // text.length = 1 + shift -2 == -1.
            }
        """,
        after = """
            public class A {
                // length = 1 from new line.
                int valA = 10; // text.length = 1 + shift -2 == -1.
            }
        """
    )

    @Test
    fun noIndexOutOfBoundsUsingTabs(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents {
            withUseTabCharacter(true)
                .withTabSize(1)
                .withIndentSize(1)
        }).build(),
        before = """
            class Test {
            	void test() {
            		System.out.println(); // comment
            	}
            }
        """
    )

    @Test
    fun blockComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /*a
              b*/
            public void method() {}
            }
        """,
        after = """
            public class A {
                /*a
                  b*/
                public void method() {}
            }
        """
    )

    @Test
    fun blockCommentCRLF(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before =
            "public class A {\r\n" +
            "/*a\r\n" +
            "  b*/\r\n" +
            "public void method() {}\r\n" +
            "}",
        after =
            "public class A {\r\n" +
            "    /*a\r\n" +
            "      b*/\r\n" +
            "    public void method() {}\r\n" +
            "}",
    )

    @Suppress("EmptyClassInitializer")
    @Test
    fun initBlocks(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                static {
                    System.out.println("hi");
                }
                
                {
                }
            }
        """
    )

    @Test
    fun moreAnnotations(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                @Incubating(
                        since = "7.0.0"
                )
                @SuppressWarnings("unchecked")
                @EqualsAndHashCode.Include
                UUID id;
            }
        """
    )

    @Test
    fun annotations(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            @Deprecated
            @SuppressWarnings("ALL")
            public class A {
            @Deprecated
            @SuppressWarnings("ALL")
                class B {
                }
            }
        """,
        after = """
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

    @Test
    fun javadoc(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /**
                    * This is a javadoc
                        */
                public void method() {}
            }
        """,
        after = """
            public class A {
                /**
                 * This is a javadoc
                 */
                public void method() {}
            }
        """
    )

    @Test
    fun tabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        // TIP: turn on "Show Whitespaces" in the IDE to see this test clearly
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
            public class A {
            	public void method() {
            	int n = 0;
            	}
            }
        """,
        after = """
            public class A {
            	public void method() {
            		int n = 0;
            	}
            }
        """
    )

    @Test
    fun shiftRight(jp: JavaParser) = assertChanged(
        jp,
        before = """
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
        after = """
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

    @Test
    fun shiftRightTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
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
        after = """
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

    @Test
    fun shiftLeft(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Test
    fun shiftLeftTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withUseTabCharacter(true) }).build(),
        before = """
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
        after = """
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

    @Test
    fun nestedIfElse(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun annotationOnSameLine(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test { 
                @Bean int method() {
                    return 1;
                }
            }
        """
    )

    @Test
    fun newClassAsMethodArgument(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun methodArgumentsThatDontStartOnNewLine(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun methodArgumentsThatDontStartOnNewLine2(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun identAndFieldAccess(jp: JavaParser) = assertUnchanged(
        jp,

        before = """
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

    @Test
    fun lambda(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Supplier;
            public class Test {
                public void method(int n) {
                    Supplier<Integer> ns = () ->
                        n;
                }
            }
        """,
        after = """
            import java.util.function.Supplier;
            public class Test {
                public void method(int n) {
                    Supplier<Integer> ns = () ->
                            n;
                }
            }
        """
    )

    @Test
    fun lambdaWithBlock(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun enums(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            enum Scope {
                None, // the root of a resolution tree
                Compile,
            }
        """
    )

    @Test
    fun twoThrows(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun twoTypeParameters(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test<A,
                    B> {
            }
        """,
        dependsOn = arrayOf("interface A {}", "interface B{}")
    )

    @Test
    fun twoImplements(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test implements A,
                    B {
            }
        """,
        dependsOn = arrayOf("interface A {}", "interface B{}")
    )

    @Test
    fun fieldsWhereClassHasAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            @Deprecated
            class Test {
                String groupId;
                String artifactId;
            }
        """
    )

    @Test
    fun methodWithAnnotation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.build(),
        before = """
            class Test {
                @Deprecated
                @SuppressWarnings("all")
            String getOnError() {
                    return "uh oh";
                }
            }
        """,
        after = """
            class Test {
                @Deprecated
                @SuppressWarnings("all")
                String getOnError() {
                    return "uh oh";
                }
            }
        """
    )

    @Suppress("CStyleArrayDeclaration", "TypeParameterExplicitlyExtendsObject")
    @Test
    fun containers(jp: JavaParser) = assertChanged(
        jp,
        before = """
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
        after = """
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

    @Test
    fun methodInvocations(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun ternaries(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class Test {
                public Test method(int n) {
                    return n > 0 ?
                        this :
                        method(n).method(n);
                }
            }
        """,
        after = """
            public class Test {
                public Test method(int n) {
                    return n > 0 ?
                            this :
                            method(n).method(n);
                }
            }
        """
    )

    @Test
    fun newClassAsArgument(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun variableWithAnnotation(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class Test {
                @Deprecated
                final Scope scope;
            
                @Deprecated
                String classifier;
            }
        """
    )

    @Test
    fun lambdaMethodParameter2(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun lambdaMethodParameter(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Test
    fun failure1(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Suppress("DuplicateCondition")
    @Test
    fun methodInvocationsNotContinuationIndentedWhenPartOfBinaryExpression(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
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

    @Suppress("CStyleArrayDeclaration")
    @Test
    fun punctuation(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents { withContinuationIndent(2) }).build(),
        before = """
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
        after = """
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

    @Test
    fun newClass(jp: JavaParser) = assertChanged(
        jp,
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/642")
    @Test
    fun alignLineComments(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    fun alignMultipleBlockCommentsOnOneLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
                public void method() {
                            /* comment 1 */ /* comment 2 */ /* comment 3 */
                }
            }
        """,
        after = """
            public class A {
                public void method() {
                    /* comment 1 */ /* comment 2 */ /* comment 3 */
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    fun alignMultipleBlockComments(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/641")
    @Test
    fun alignTryCatchFinally(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/663")
    @Test
    fun alignBlockPrefixes(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        recipe = AutoFormat(),
        before = """
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

    @Test
    fun alignInlineBlockComments() = assertChanged(
        before = """
            public class WhitespaceIsHard {
            /* align comment */ public void method() { /* tricky */
            /* align comment */ int val = 10; /* tricky */
            // align comment and end paren.
            }
            }
        """,
        after = """
            public class WhitespaceIsHard {
                /* align comment */ public void method() { /* tricky */
                    /* align comment */ int val = 10; /* tricky */
                    // align comment and end paren.
                }
            }
        """
    )

    @Test
    fun trailingMultilineString() = assertUnchanged(
        before = """
            public class WhitespaceIsHard {
                public void method() { /* tricky */
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
    @Test
    fun javaDocsWithMultipleLeadingAsterisks() = assertChanged(
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/980")
    @Test
    fun alignJavaDocsWithCRLF(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before =
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
            "                 */\r\n"+
            "        public int methodTwo(int value) {\r\n" +
            "            return value + 1;\r\n" +
            "        }\r\n" +
            "}"
        ,
        after =
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
            "     */\r\n"+
            "    public int methodTwo(int value) {\r\n" +
            "        return value + 1;\r\n" +
            "    }\r\n" +
            "}"
    )

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    fun alignJavaDocs(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
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
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/709")
    @Test
    fun useContinuationIndentExtendsOnNewLine() = assertUnchanged(
        dependsOn = arrayOf("package org.a; public class A {}"),
        before = """
            package org.b;
            import org.a.A;
            class B
                    extends A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/883")
    @Test
    fun alignIdentifierOnNewLine() = assertUnchanged(
        dependsOn = arrayOf("package org.a; public class A {}"),
        before = """
            package org.b;
            import org.a.A;
            class B extends
                    A {
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1526")
    @Test
    fun doNotFormatSingleLineCommentAtCol0() = assertChanged(
        before = """
            class A {
            // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
            // DOES shift the suffix of comment 2.
            void shiftRight() {}
            }
        """,
        after = """
            class A {
            // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
            // DOES shift the suffix of comment 2.
                void shiftRight() {}
            }
        """
    )
}
