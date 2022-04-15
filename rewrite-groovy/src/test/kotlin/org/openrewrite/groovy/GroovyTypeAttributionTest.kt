package org.openrewrite.groovy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.asFullyQualified
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest

class GroovyTypeAttributionTest : RewriteTest {

    @Disabled
    @Test
    fun defTypeAttributed() = rewriteRun(
        groovy(
            """
                import java.util.*
                class Test {
                    def l = new ArrayList()
                }
            """
        ) { spec ->
            spec.afterRecipe { cu ->
                val field = cu.classes[0].body.statements[0] as J.VariableDeclarations
                assertThat(field.variables[0].variableType!!.type.asFullyQualified()!!.fullyQualifiedName)
                    .isEqualTo("java.util.ArrayList")
            }
        }
    )
}
