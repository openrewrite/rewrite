package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface FinalClassTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = FinalClass()

    @Test
    fun finalizeClass() = assertChanged(
        before = """
            public class A {
                private A(String s) {
                }
            
                private A() {
                }
            }
        """,
        after = """
            public final class A {
                private A(String s) {
                }
            
                private A() {
                }
            }
        """
    )

    @Test
    fun hasPublicConstructor() = assertUnchanged(
        before = """
            public class A {
                private A(String s) {
                }
                
                public A() {
                }
            }
        """
    )

    @Test
    fun hasImplicitConstructor() = assertUnchanged(
        before = """
            public class A {
            }
        """
    )

    @Test
    fun innerClass() = assertChanged(
        before = """
            class A {
            
                class B {
                    private B() {}
                }
            }
        """,
        after = """
            class A {
            
                final class B {
                    private B() {}
                }
            }
        """
    )

    @Test
    fun classInsideInterfaceIsImplicitlyFinal() = assertUnchanged(
         before = """
             public interface A {
                class B {
                    private B() { }
                }
             }
         """
    )
}
