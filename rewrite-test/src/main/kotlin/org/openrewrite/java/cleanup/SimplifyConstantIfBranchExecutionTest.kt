package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface SimplifyConstantIfBranchExecutionTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = SimplifyConstantIfBranchExecution()

    @Test
    fun doNotChangeNonIf(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                public void test() {
                    boolean b = true;
                    if (!b) {
                        System.out.println("hello");
                    }
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (true) {
                        System.out.println("hello");
                    }
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                    System.out.println("hello");
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfFalse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (false) {
                        System.out.println("hello");
                    }
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfElseTrue(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (true) {
                    } else {
                        System.out.println("hello");
                    }
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfElseFalse(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (false) {
                    } else {
                        System.out.println("hello");
                    }
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                    System.out.println("hello");
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfTrueNoBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (true) System.out.println("hello");
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                    System.out.println("hello");
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfFalseNoBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (false) System.out.println("hello");
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfTrueEmptyBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (true) {}
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                }
            }
        """
    )

    @Test
    fun simplifyConstantIfFalseEmptyBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            public class A {
                public void test() {
                    if (false) {}
                }
            }
        """,
        after = """
            public class A {
                public void test() {
                }
            }
        """
    )
}
