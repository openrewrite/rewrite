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
package org.openrewrite.hcl.format

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.hcl.HclParser
import org.openrewrite.hcl.HclRecipeTest
import org.openrewrite.hcl.style.SpacesStyle
import org.openrewrite.style.NamedStyles

class SpacesTest : HclRecipeTest {
    override val recipe: Recipe
        get() = Spaces()

    @Test
    fun columnarAlignment() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              size =1
              encrypted =true
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
    fun nonColumnarAlignment() = assertChanged(
        parser = HclParser.builder().styles(spaces(SpacesStyle(SpacesStyle.BodyContent(false)))).build(),
        before = """
            resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              size = 1
              encrypted = true
            }
        """
    )

    @Test
    fun blockOpen() = assertChanged(
        before = """
            resource "aws_ebs_volume"{
              size = 1
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              size = 1
            }
        """
    )

    private fun spaces(style: SpacesStyle) = listOf(
        NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(), listOf(style)
        )
    )
}
