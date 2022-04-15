package org.openrewrite.groovy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.asFullyQualified
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpec
import java.util.function.Consumer

class GroovyTypeAttributionTest : RewriteTest {

    @Test
    fun defTypeAttributed() = rewriteRun(
        groovy(
            """
                class Test {
                    static void test() {
                        def l = new ArrayList()
                    }
                }
            """,
            isAttributed(true)
        )
    )

    @Test
    fun defFieldNotTypeAttributed() = rewriteRun(
        groovy(
            """
                class Test {
                    def l = new ArrayList()
                }
            """,
            isAttributed(false)
        )
    )

    @Test
    fun globalTypeAttributed() = rewriteRun(
        groovy(
            """
                def l = new ArrayList()
            """,
            isAttributed(true)
        )
    )

    private fun isAttributed(attributed: Boolean) = Consumer<SourceSpec<G.CompilationUnit>> { spec ->
        spec.afterRecipe { cu ->
            object : JavaVisitor<Int>() {
                override fun visitVariable(variable: J.VariableDeclarations.NamedVariable, p: Int): J {
                    assertThat(variable.variableType!!.type.asFullyQualified()!!.fullyQualifiedName)
                        .isEqualTo(if (attributed) "java.util.ArrayList" else "java.lang.Object")
                    return variable
                }
            }.visit(cu, 0)
        }
    }
}
