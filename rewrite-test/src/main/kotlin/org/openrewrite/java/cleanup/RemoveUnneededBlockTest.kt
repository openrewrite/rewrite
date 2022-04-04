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
}
