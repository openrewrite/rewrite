package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.search.FindMissingTypes
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation

interface FindMissingTypesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindMissingTypes())
            .typeValidationOptions(TypeValidation.none())
    }

    @Test
    fun findsMissingAnnotationType(jp: JavaParser) {
        val cu = jp.parse("""
            import org.junit.Test;
            
            class A {
                @Test
                void foo() {}
            }
        """)[0]
        assertThat(FindMissingTypes.findMissingTypes(cu).size).isEqualTo(1)
    }
}
