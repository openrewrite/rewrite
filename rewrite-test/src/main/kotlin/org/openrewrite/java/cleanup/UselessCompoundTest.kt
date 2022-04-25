package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface UselessCompoundTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UselessCompound()

    @Test
    fun removeUselessCompoundAnd(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    boolean b = true;
                    b &= true;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    boolean b = true;
                }
            }
        """
    )

    @Test
    fun fixUselessCompoundAnd(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    boolean b = true;
                    b &= false;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    boolean b = true;
                    b = false;
                }
            }
        """
    )

    @Test
    fun removeUselessCompoundOr(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    boolean b = true;
                    b |= false;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    boolean b = true;
                }
            }
        """
    )

    @Test
    fun fixUselessCompoundOr(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    boolean b = true;
                    b |= true;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    boolean b = true;
                    b = true;
                }
            }
        """
    )

    @Test
    fun removeUselessCompoundOrComplex(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    boolean b = true;
                    b |= false && true && true;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    boolean b = true;
                }
            }
        """
    )
}
