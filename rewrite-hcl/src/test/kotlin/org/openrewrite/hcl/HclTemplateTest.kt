/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

            override fun visitBlock(block: Hcl.Block, p: ExecutionContext): Hcl {
                if (block.body.size == 1) {
                    return block.withTemplate(t, block.coordinates.last())
                }
                return super.visitBlock(block, p)
            }
        }.toRecipe(),
        before = """
            resource "aws_ebs_volume" {
              size = 1
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
        """
    )
}
