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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.IntelliJ

interface TabsAndIndentsTest : RecipeTest {
    override val recipe: Recipe
        get() = TabsAndIndents()

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @Test
    fun tabsAndIndents(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
        before = """
            public class Test {
            public int[] X = new int[]{1, 3, 5, 7, 9, 11};

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
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
    fun forLoop(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents().apply {
            continuationIndent = 2
        })).build(),
        before = """
            public class Test {
                public void test() {
                int m = 0;
                int n = 0;
                for (
                 int i = 0;
                 i < 5;
                 i++, m++,
                 n++);
                for (int i = 0;
                 i < 5;
                 i++, m++,
                 n++);
                labeled: for (int i = 0;
                 i < 5;
                 i++, m++,
                 n++);
                }
            }
        """,
        after = """
            public class Test {
                public void test() {
                    int m = 0;
                    int n = 0;
                    for (
                      int i = 0;
                      i < 5;
                      i++, m++,
                        n++);
                    for (int i = 0;
                         i < 5;
                         i++, m++,
                           n++);
                    labeled: for (int i = 0;
                                  i < 5;
                                  i++, m++,
                                    n++);
                }
            }
        """
    )

    @Test
    fun methodDeclaration(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents().apply {
            continuationIndent = 2
        })).build(),
        before = """
            public class Test {
            public void test(int a,
            int b) {}

            public void test2(
            int a,
            int b) {}
            }
        """,
        after = """
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
    fun expressions(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents().apply {
            continuationIndent = 2
        })).build(),
        before = """
            public class Test {
            int X[];
            public void test(int a, int x, int y) {
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
            public class Test {
                int X[];
                public void test(int a, int x, int y) {
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
    fun lineComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
    fun blockComment(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
    fun javadoc(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents())).build(),
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
        jp.styles(listOf(IntelliJ.defaultTabsAndIndents().apply {
            isUseTabCharacter = true
        })).build(),
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
}
