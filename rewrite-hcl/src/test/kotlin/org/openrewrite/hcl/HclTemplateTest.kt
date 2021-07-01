package org.openrewrite.hcl

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.hcl.tree.Hcl
import org.openrewrite.java.JavaTemplateTest

class HclTemplateTest : HclRecipeTest {

    @Test
    fun lastBodyContentInBlock() = assertChanged(
        recipe = object : HclVisitor<ExecutionContext>() {
            val t = HclTemplate.builder({ cursor }, "encrypted = true")
                .doBeforeParseTemplate(JavaTemplateTest.print)
                .build()

            override fun visitBody(body: Hcl.Body, p: ExecutionContext): Hcl {
                if (body.contents.size == 1 && cursor.parentOrThrow.getValue<Hcl>() !is Hcl.ConfigFile) {
                    return body.withTemplate(t, body.coordinates.last())
                }
                return super.visitBody(body, p)
            }
        }.toRecipe(),
        before = """
            resource "aws_ebs_volume" {
                size = 1
            }
        """,
        after = """
            resource "aws_ebs_volume" {
                size = 1
                encrypted = true
            }
        """
    )
}
