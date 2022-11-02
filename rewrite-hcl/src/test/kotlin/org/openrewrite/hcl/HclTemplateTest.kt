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
import org.openrewrite.hcl.tree.Expression
import org.openrewrite.hcl.tree.Hcl

class HclTemplateTest : HclRecipeTest {

    @Test
    fun lastInConfigFile() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder({ cursor }, "after {\n}").build()

                override fun visitConfigFile(configFile: Hcl.ConfigFile, p: ExecutionContext): Hcl {
                    if (configFile.body.size == 1) {
                        return configFile.withTemplate(t, configFile.coordinates.last())
                    }
                    return super.visitConfigFile(configFile, p)
                }
            }
        },
        before = """
            before {
            }
        """,
        after = """
            before {
            }
            after {
            }
        """
    )

    @Test
    fun firstInConfigFile() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder({ cursor }, "after {\n}").build()

                override fun visitConfigFile(configFile: Hcl.ConfigFile, p: ExecutionContext): Hcl {
                    if (configFile.body.size == 1) {
                        return configFile.withTemplate(t, configFile.coordinates.first())
                    }
                    return super.visitConfigFile(configFile, p)
                }
            }
        },
        before = """
            before {
            }
        """,
        after = """
            after {
            }
            before {
            }
        """
    )

    @Test
    fun middleOfConfigFile() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder({ cursor }, "second {\n}").build()

                override fun visitConfigFile(configFile: Hcl.ConfigFile, p: ExecutionContext): Hcl {
                    if (configFile.body.size == 2) {
                        return configFile.withTemplate(t, configFile.coordinates.add(
                            Comparator.comparing { bc -> (bc as Hcl.Block).type!!.name }
                        ))
                    }
                    return super.visitConfigFile(configFile, p)
                }
            }
        },
        before = """
            first {
            }
            third {
            }
        """,
        after = """
            first {
            }
            second {
            }
            third {
            }
        """
    )

    @Test
    fun lastBodyContentInBlock() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder({ cursor }, "encrypted = true").build()

                override fun visitBlock(block: Hcl.Block, p: ExecutionContext): Hcl {
                    if (block.body.size == 1) {
                        return block.withTemplate(t, block.coordinates.last())
                    }
                    return super.visitBlock(block, p)
                }
            }
        },
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

    @Test
    fun replaceBlock() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder(
                    { cursor }, """
                resource "azure_storage_volume" {
                  size = 1
                }
            """
                ).build()

                override fun visitBlock(block: Hcl.Block, p: ExecutionContext): Hcl {
                    if ((block.labels[0] as Hcl.Literal).valueSource.contains("aws")) {
                        return block.withTemplate(t, block.coordinates.replace())
                    }
                    return super.visitBlock(block, p)
                }
            }
        },
        before = """
            resource "aws_ebs_volume" {
              size = 1
            }
        """,
        after = """
            resource "azure_storage_volume" {
              size = 1
            }
        """
    )

    @Test
    fun replaceExpression() = assertChanged(
        recipe = toRecipe {
            object : HclVisitor<ExecutionContext>() {
                val t = HclTemplate.builder({ cursor }, "\"jonathan\"").build()

                override fun visitExpression(expression: Expression, p: ExecutionContext): Hcl {
                    if (expression is Hcl.QuotedTemplate && expression.print(cursor.parentOrThrow).contains("you")) {
                        return expression.withTemplate(t, expression.coordinates.replace())
                    }
                    return super.visitExpression(expression, p)
                }
            }
        },
        before = """
            hello = "you"
        """,
        after = """
            hello = "jonathan"
        """
    )
}
