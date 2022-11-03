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
import org.openrewrite.hcl.HclRecipeTest

class BracketSpacesTest : HclRecipeTest {
    override val recipe: Recipe
        get() = AutoFormat()

    @Test
    fun blockBraces() = assertChanged(
        before = """
            resource "aws_ebs_volume"    {    size      = 1
            encrypted = true   }
        """,
        after = """
            resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
        """
    )

    @Test
    fun objectValues() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
            foo = { bar = "bar"
            baz = "baz"
            }
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              foo = {
                bar = "bar"
                baz = "baz"
              }
            }
        """
    )


    @Test
    fun objectValueBracesMulti() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              foo = { bar = "bar"
                    bazzz = "bazzz"}
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              foo = {
                bar   = "bar"
                bazzz = "bazzz"
              }
            }
        """,
        cycles = 3
    )

    @Test
    fun objectValueBracesSingle() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              foo = {    bar = "bar"   ,     baz = "baz"     }
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              foo = { bar = "bar", baz = "baz" }
            }
        """,
    )

    @Test
    fun objectValueBracesComplex() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              multiline_var = { xxx = "xxx"
                    yyy = "yyy"
                    zzz = {    x = "x"   ,   y = "y"   }
                    nested = {                    xx = "xx"
                    yy = "yy"                    }
             }
            }
        """,
        after = """
            resource "aws_ebs_volume" {
              multiline_var = {
                xxx = "xxx"
                yyy = "yyy"
                zzz = { x = "x", y = "y" }
                nested = {
                  xx = "xx"
                  yy = "yy"
                }
              }
            }
        """,
        cycles = 3
    )

}
