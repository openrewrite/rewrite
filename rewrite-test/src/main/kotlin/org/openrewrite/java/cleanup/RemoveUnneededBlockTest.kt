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
}
