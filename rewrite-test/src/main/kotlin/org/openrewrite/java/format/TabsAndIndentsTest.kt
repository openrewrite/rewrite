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

interface TabsAndIndentsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = TabsAndIndents()

    fun tabsAndIndents(with: TabsAndIndentsStyle.() -> TabsAndIndentsStyle = { this }) = listOf(
        NamedStyles(
            randomId(), "test", "test", "test", emptySet(), listOf(
                IntelliJ.tabsAndIndents().run { with(this) })
        )
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
        """.trimIndent()
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
        """.trimIndent()
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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
        jp.styles(tabsAndIndents { withContinuationIndent(8) }).build(),
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

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @Test
    fun tabsAndIndents(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
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
            // this is a comment
            public void method() {}
            }
        """,
        after = """
            public class A {
                // this is a comment
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
        """.trimIndent(),
        after = """
            public class A {
                // length = 1 from new line.
                int valA = 10; // text.length = 1 + shift -2 == -1.
            }
        """.trimIndent()
    )

    @Test
    fun noIndexOutOfBoundsUsingTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
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
        """.trimIndent(),
        after = """
            class Test {
            	void test() {
            		System.out.println();// comment
            	}
            }
        """.trimIndent(),
        expectedCyclesThatMakeChanges = 2
    )

    @Test
    fun blockComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /* this is a comment
               that extends onto another line */
            public void method() {}
            }
        """,
        after = """
            public class A {
                /* this is a comment
                that extends onto another line */
                public void method() {}
            }
        """
    )

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
        """,
        expectedCyclesThatMakeChanges = 2
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
    fun mixedToTabs(jp: JavaParser.Builder<*, *>) = assertChanged(
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
    fun mixedToSpaces(jp: JavaParser) = assertChanged(
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

    @Test
    fun containers(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.io.ByteArrayInputStream;
            import java.io.InputStream;
            import java.io.Serializable;
            @Deprecated
            (since = "1.0")
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
            @Deprecated
                    (since = "1.0")
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
        """.trimIndent()
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

    @Test
    fun tabsFileWithSpacesFormat(jp: JavaParser) = assertChanged(
        jp,
        before = """
		public class ZuulRouteApplicationContextInitializer {
				public ZuulRouteApplicationContextInitializer() {
						return null;
				}
			}
        """,
        after = """
        public class ZuulRouteApplicationContextInitializer {
            public ZuulRouteApplicationContextInitializer() {
                return null;
            }
        }
        """
    )

    @Test
    fun mixedTabsSpacesFileWithSpacesFormat(jp: JavaParser) = assertChanged(
        jp,
        before = """
		public class ZuulRouteApplicationContextInitializer {
				public ZuulRouteApplicationContextInitializer() {
	    				  return null;
				    }
			}
        """,
        after = """
        public class ZuulRouteApplicationContextInitializer {
            public ZuulRouteApplicationContextInitializer() {
                return null;
            }
        }
        """
    )

    @Test
    fun spaceToTab(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents {
            withUseTabCharacter(true)
                .withTabSize(1)
                .withIndentSize(1)
                .withContinuationIndent(2)
                .withIndentsRelativeToExpressionStart(false)
        }).build(),
        before = """
        public class A {
        	@Deprecated
         void normalizeWorks() {
        	}
        }
        """,
        after = """
        public class A {
        	@Deprecated
        	void normalizeWorks() {
        	}
        }
        """.trimIndent()
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
        """.trimIndent(),
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
        """.trimIndent(),
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    fun alignMultilineStyleCommentLeft(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
                    /**
                     * JavaDoc starts on 2nd line
                     */
            public class A {
                    /** JavaDoc starts on first line.
                     * @param value test value.
                     * @return value + 1 */
                    public int methodOne(int value) {
                        return value + 1;
                    }
            
                            /** Edge case odd formatting.
              * @param value test value.
                            * @return value + 1
                                        */
                    public int methodTwo(int value) {
                        return value + 1;
                    }
            }
        """.trimIndent(),
        after = """
            /**
             * JavaDoc starts on 2nd line
             */
            public class A {
                /** JavaDoc starts on first line.
                 * @param value test value.
                 * @return value + 1 */
                public int methodOne(int value) {
                    return value + 1;
                }
            
                /** Edge case odd formatting.
                 * @param value test value.
                 * @return value + 1
                 */
                public int methodTwo(int value) {
                    return value + 1;
                }
            }
        """.trimIndent(),
        expectedCyclesThatMakeChanges = 2
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
        """.trimIndent(),
        after = """
            public class A {
                public void method() {
                    /* comment 1 */ /* comment 2 */ /* comment 3 */
                }
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Test
    fun alignMultipleBlockComments(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(tabsAndIndents()).build(),
        before = """
            public class A {
            /* this comment should be left alone
             since a line doesn't start with a * */
            
                /* this comment maintains the whitespace from the next `blank` line.
                 
               * should be formatted, since extra lines start with * 
                    */
            
                    /* this comment
                        * should be formatted, since extra lines start with * */
            public void method() {}
            }
        """.trimIndent(),
        after = """
            public class A {
                /* this comment should be left alone
                since a line doesn't start with a * */
            
                /* this comment maintains the whitespace from the next `blank` line.
                
                 * should be formatted, since extra lines start with * 
                 */
            
                /* this comment
                 * should be formatted, since extra lines start with * */
                public void method() {}
            }
        """.trimIndent(),
        expectedCyclesThatMakeChanges = 2
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
        """.trimIndent()
    )

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/663")
    @Test
    fun preVisitAlignmentConditions(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        recipe = AutoFormat(),
        before = """
            public class Test {
                // Practice A.
                public void methodA()
                {
                    for (int i = 0; i < 10; ++i)
                    {
                        if (i % 2 == 0)
                        {
                            try
                            {
                                Integer value = Integer.valueOf("100");
                            } catch (Exception ex)
                            {
                                throw new RuntimeException();
                            } finally
                            {
                                System.out.println("out");
                            }
                        }
                    }
                }
            
                // Practice B.
                public void methodB() {
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
            
                // How IntelliJ formats arguments on new lines.
                public void methodB
                        () { // We do this now, but it'll break after the changes.
                    for
                    (int i = 0; i < 10; ++i) {
                        if
                        (i % 2 == 0) {
                            try {
                                Integer value = Integer.valueOf("123");
                            } catch
                            (Exception ex) {
                                throw new RuntimeException();
                            } finally {
                                System.out.println("out");
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    )
}
