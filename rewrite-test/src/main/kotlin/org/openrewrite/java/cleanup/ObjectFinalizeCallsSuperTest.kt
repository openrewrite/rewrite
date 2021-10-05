@file:Suppress("deprecation", "RedundantThrows")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface ObjectFinalizeCallsSuperTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = ObjectFinalizeCallsSuper()

    @Test
    fun hasSuperFinalizeInvocation() = assertUnchanged(
        before = """
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                    super.finalize();
                }
            }
        """
    )

    @Test
    fun addsSuperFinalizeInvocation() = assertChanged(
        before = """
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                }
            }
        """,
        after = """
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                    super.finalize();
                }
            }
        """
    )
}
