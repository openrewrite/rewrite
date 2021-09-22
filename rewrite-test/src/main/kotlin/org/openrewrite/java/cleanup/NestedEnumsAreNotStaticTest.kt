@file:Suppress("UnnecessaryEnumModifier")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface NestedEnumsAreNotStaticTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NestedEnumsAreNotStatic()

    @Test
    fun enumIsNotNested() = assertUnchanged(
        before = """
            static enum ABC {
                A, B, C
            }
        """
    )

    @Test
    fun nestedEnumIsNotStatic() = assertUnchanged(
        before = """
            class A {
                enum ABC {
                    A, B, C
                }
            }
        """
    )

    @Test
    fun nestedEnumIsStatic() = assertChanged(
        before = """
            class A {
            
                static enum ABC {
                    A, B, C
                }
            
                private static enum DEF {
                    D, E, F
                }
            }
        """,
        after = """
            class A {
            
                enum ABC {
                    A, B, C
                }
            
                private enum DEF {
                    D, E, F
                }
            }
        """
    )
}
