@file:Suppress("NullableProblems")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface UnnecessaryPrimitiveAnnotationsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UnnecessaryPrimitiveAnnotations()

    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion().classpath("jsr305").build()

    @Test
    fun nullableOnNonPrimitive() = assertUnchanged(
        before = """
            import javax.annotation.CheckForNull;
            import javax.annotation.Nullable;
            class A {
                @CheckForNull
                public Object getCount(@Nullable Object val) {
                    return val;
                }
            }
        """
    )

    @Test
    fun unnecessaryNullable() = assertChanged(
        before = """
            import javax.annotation.CheckForNull;
            import javax.annotation.Nullable;
            class A {
                @CheckForNull
                public int getCount(@Nullable int val) {
                    return val;
                }
            }
        """,
        after = """
            class A {
    
                public int getCount(int val) {
                    return val;
                }
            }
        """
    )
}