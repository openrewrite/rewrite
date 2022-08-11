/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface RemoveUnneededBlockTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveUnneededBlock()

    @Test
    fun doNotChangeMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class A {
                void test() {
                }
            }
        """
    )

    @Test
    fun doNotChangeLabeledBlock(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class A {
                void test() {
                    testLabel: {
                        System.out.println("hello!");
                    }
                }
            }
        """
    )

    @Test
    fun doNotChangeEmptyIfBlock(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class A {
                void test() {
                    if(true) { }
                }
            }
        """
    )

    @Test
    fun doNotRemoveDoubleBraceInitBlocksInMethod(jp: JavaParser) = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            public class T {
                public void whenInitializeSetWithDoubleBraces_containsElements() {
                    Set<String> countries = new HashSet<String>() {
                        {
                           add("a");
                           add("b");
                        }
                    };
                }
            }
        """
    )

    @Test
    fun doNotRemoveDoubleBraceInitBlocks(jp: JavaParser) = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            public class T {
                final Set<String> countries = new HashSet<String>() {
                    {
                       add("a");
                       add("b");
                    }
                };
            }
        """
    )

    @Test
    fun doNotRemoveObjectArrayInitializer(jp: JavaParser) = assertUnchanged(
        before = """
            public class A {
                Object[] a = new Object[] {
                    "a",
                    "b"
                };
            }
        """
    )

    @Test
    fun doNotRemoveObjectArrayArrayInitializer(jp: JavaParser) = assertUnchanged(
        before = """
            public class A {
                Object[][] a = new Object[][] {
                    { "a", "b" },
                    { "c", "d" }
                };
            }
        """
    )

    @Test
    fun simplifyNestedBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                void test() {
                    {
                        System.out.println("hello!");
                    }
                }
            }
        """,
        after = """
            public class A {
                void test() {
                    System.out.println("hello!");
                }
            }
        """
    )

    @Test
    fun simplifyDoublyNestedBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                void test() {
                    {
                         { System.out.println("hello!"); }
                    }
                }
            }
        """,
        after = """
            public class A {
                void test() {
                    System.out.println("hello!");
                }
            }
        """
    )

    @Test
    fun simplifyBlockNestedInIfBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                void test() {
                    if (true) {
                         { System.out.println("hello!"); }
                    }
                }
            }
        """,
        after = """
            public class A {
                void test() {
                    if (true) {
                        System.out.println("hello!");
                    }
                }
            }
        """
    )

    @Test
    fun simplifyBlockInStaticInitializerIfBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                static {
                    {
                         {
                            System.out.println("hello static!");
                            System.out.println("goodbye static!");
                         }
                    }
                }

                {
                    {
                        System.out.println("hello init!");
                        System.out.println("goodbye init!");
                    }
                }
            }
        """,
        after = """
            public class A {
                static {
                    System.out.println("hello static!");
                    System.out.println("goodbye static!");
                }

                {
                    System.out.println("hello init!");
                    System.out.println("goodbye init!");
                }
            }
        """
    )

    @Test
    fun simplifyCraziness(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.HashSet;
            import java.util.Set;
            public class A {
                static {
                    {
                         new HashSet<String>() {
                            {
                                add("a");
                                add("b");
                                {
                                    System.out.println("hello static!");
                                    System.out.println("goodbye static!");
                                }
                            }
                         };
                    }
                }

                {
                    {
                         new HashSet<String>() {
                            {
                                add("a");
                                add("b");
                                {
                                    System.out.println("hello init!");
                                    System.out.println("goodbye init!");
                                }
                            }
                         };
                    }
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            public class A {
                static {
                    new HashSet<String>() {
                        {
                            add("a");
                            add("b");
                            System.out.println("hello static!");
                            System.out.println("goodbye static!");
                        }
                    };
                }

                {
                    new HashSet<String>() {
                        {
                            add("a");
                            add("b");
                            System.out.println("hello init!");
                            System.out.println("goodbye init!");
                        }
                    };
                }
            }
        """
    )

    @Test
    fun simplifyDoesNotFormatSurroundingCode(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                static {
                    int[] a = new int[] { 1, 2, 3 };
                    int[] b = new int[] {4,5,6};
                    {
                        System.out.println("hello static!");
                    }
                }
            }
        """,
        after = """
            public class A {
                static {
                    int[] a = new int[] { 1, 2, 3 };
                    int[] b = new int[] {4,5,6};
                    System.out.println("hello static!");
                }
            }
        """
    )

    @Test
    fun simplifyDoesNotFormatInternalCode(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                static {
                    int[] a = new int[] { 1, 2, 3 };
                    int[] b = new int[] {4,5,6};
                    {
                        System.out.println("hello!");
                        System.out.println( "world!" );
                    }
                }
            }
        """,
        after = """
            public class A {
                static {
                    int[] a = new int[] { 1, 2, 3 };
                    int[] b = new int[] {4,5,6};
                    System.out.println("hello!");
                    System.out.println("world!");
                }
            }
        """
    )
}
