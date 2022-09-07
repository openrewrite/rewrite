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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.hcl.HclParser
import org.openrewrite.hcl.HclRecipeTest
import org.openrewrite.hcl.style.SpacesStyle
import org.openrewrite.hcl.tree.Comment
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

    @Test
    fun attributeGroups() = assertChanged(
        before = """
            resource "custom_resource" {
              size = 1
              x = 1
              
              longerattribute = "long"
              y = 2
            }
        """,
        after = """
            resource "custom_resource" {
              size = 1
              x    = 1
              
              longerattribute = "long"
              y               = 2
            }
        """
    )

    @Test
    fun attributeGroupMultilineValue() = assertChanged(
        before = """
            variable myvar {
              description = "Sample Variable"
              type = object({
                string_var = string
                string_var_2 = string
                multiline_var = object({
                  x = string
                  foo = string
                  
                  y = string
                })
                another_string_var = string
                another_string_var_2 = string
              })
            }
        """,
        after = """ 
            variable myvar {
              description = "Sample Variable"
              type = object({
                string_var   = string
                string_var_2 = string
                multiline_var = object({
                  x   = string
                  foo = string
                  
                  y = string
                })
                another_string_var   = string
                another_string_var_2 = string
              })
            }
        """
    )

    @Test
    fun objectAttributeGroupMultilineValue() = assertChanged(
        before = """
            locals {
               myvar = {
                 description = "Sample Variable"
                 type = {
                   string_var = "value"
                   string_var_2 = "value"
                   multiline_var = {
                     x = "value"
                     foo = "value"
                  
                     y = "value"
                   }
                   another_string_var = "value"
                   another_string_var_2 = "value"
                }
              } 
            }
        """,
        after = """ 
            locals {
               myvar = {
                 description = "Sample Variable"
                 type = {
                   string_var   = "value"
                   string_var_2 = "value"
                   multiline_var = {
                     x   = "value"
                     foo = "value"
                  
                     y = "value"
                   }
                   another_string_var   = "value"
                   another_string_var_2 = "value"
                }
              } 
            }
        """
    )

    @Test
    fun noAttributeGroups() = assertChanged(
        before = """
            resource "custom_resource" {
              size   = 1

              x = 1
              
              longerattribute   = "long"

              y = 2
            }
        """,
        after = """
            resource "custom_resource" {
              size = 1

              x = 1
              
              longerattribute = "long"

              y = 2
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/974")
    @Test
    fun lineHashComment() = assertChanged(
        before = """
            # a hash comment with inline # or // is still 1 line.
            resource{}
        """,
        after = """
            # a hash comment with inline # or // is still 1 line.
            resource {}
        """,
        afterConditions = { c ->
            assertThat(c.prefix.comments.size).isEqualTo(1)
            assertThat(c.prefix.comments[0].style).isEqualTo(Comment.Style.LINE_HASH)
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/974")
    @Test
    fun lineSlashComment() = assertChanged(
        before = """
            // a slash comment with inline # or // is still 1 line.
            resource{}
        """,
        after = """
            // a slash comment with inline # or // is still 1 line.
            resource {}
        """,
        afterConditions = { c ->
            assertThat(c.prefix.comments.size).isEqualTo(1)
            assertThat(c.prefix.comments[0].style).isEqualTo(Comment.Style.LINE_SLASH)
        }
    )

    private fun spaces(style: SpacesStyle) = listOf(
        NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(), listOf(style)
        )
    )
}
