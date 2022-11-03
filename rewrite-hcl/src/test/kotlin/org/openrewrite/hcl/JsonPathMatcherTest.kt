/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.hcl.Assertions.hcl
import org.openrewrite.hcl.tree.Hcl
import org.openrewrite.test.RewriteTest

class JsonPathMatcherTest : RewriteTest {

    private fun anyBlockMatch(hcl: Hcl, matcher: JsonPathMatcher): Boolean {
        var matches = false
        object : HclVisitor<Int>() {
            override fun visitBlock(block: Hcl.Block, p: Int): Hcl {
                val b = super.visitBlock(block, p)
                matches = matcher.matches(cursor) || matches
                return b
            }
        }.visit(hcl, 0)
        return matches
    }

    private fun anyAttributeMatch(hcl: Hcl, matcher: JsonPathMatcher): Boolean {
        var matches = false
        object : HclVisitor<Int>() {
            override fun visitAttribute(attribute: Hcl.Attribute, p: Int): Hcl {
                val a = super.visitAttribute(attribute, p)
                matches = matcher.matches(cursor) || matches
                return a
            }
        }.visit(hcl, 0)
        return matches
    }

    @Test
    fun match() = rewriteRun(
        hcl(
            """
                provider "azurerm" {
                  features {
                    key_vault {
                      purge_soft_delete_on_destroy = true
                    }
                  }
                  somethingElse {
                  }
                  attr = 1
                }
            """
        ) { spec ->
            spec.beforeRecipe { configFile ->
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.provider.features.key_vault"))).isTrue
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.provider.features.dne"))).isFalse
            }
        }
    )

    @Test
    fun binaryExpression() = rewriteRun(
        hcl(
            """
                provider "azurerm" {
                  features {
                    key_vault {
                      purge_soft_delete_on_destroy = true
                    }
                  }
                }
            """
        ) { spec ->
            spec.beforeRecipe { configFile ->
                assertThat(anyAttributeMatch(configFile, JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy == 'true')]"))).isTrue
                assertThat(anyAttributeMatch(configFile, JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy == 'false')]"))).isFalse
            }
        }
    )

    @Test
    fun unaryExpression() = rewriteRun(
        hcl(
            """
                provider "azurerm" {
                  features {
                    key_vault {
                      purge_soft_delete_on_destroy = true
                    }
                  }
                }
            """
        ) { spec ->
            spec.beforeRecipe { configFile ->
                assertThat(anyAttributeMatch(configFile, JsonPathMatcher("$.provider.features.key_vault[?(@.purge_soft_delete_on_destroy)]"))).isTrue
                assertThat(anyAttributeMatch(configFile, JsonPathMatcher("$.provider.features.key_vault[?(@.no_match)]"))).isFalse
            }
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2197")
    @Test
    fun unaryExpressionOnBlockName() = rewriteRun(
        hcl(
            """
            provider "azurerm" {
              features {
                key_vault {
                  purge_soft_delete_on_destroy = true
                }
              }
              somethingElse {
              }
              attr = 1
            }
            """
        ) { spec ->
            spec.beforeRecipe { configFile ->
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.*[?(@.features)]"))).isTrue
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.*[?(@.no_match)]"))).isFalse
            }
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2198")
    @Test
    fun matchPropertyFromWildCardOnBlock() = rewriteRun(
        hcl(
            """
            provider "azurerm" {
              features {
                key_vault {
                  purge_soft_delete_on_destroy = true
                }
              }
              somethingElse {
              }
              attr = 1
            }
            """
        ) { spec ->
            spec.beforeRecipe { configFile ->
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.*.features"))).isTrue
                assertThat(anyBlockMatch(configFile, JsonPathMatcher("$.*.no_match"))).isFalse
            }
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2198")
    @Test
    fun matchParentBlockWithWildCard() = assertMatched(
        jsonPath = "$.*",
        before = arrayOf("""
            provider "azurerm" {
              features {
                key_vault {
                  purge_soft_delete_on_destroy = true
                }
              }
              somethingElse {
              }
              attr = 1
            }
        """.trimIndent()),
        after = arrayOf("""
            provider "azurerm" {
              features {
                key_vault {
                  purge_soft_delete_on_destroy = true
                }
              }
              somethingElse {
              }
              attr = 1
            }
        """.trimIndent()),
        printMatches = false
    )

    @Disabled("Test enables a simple way to test JsonPatchMatches on HCL.")
    @Test
    fun findJsonPathMatches() = assertMatched(
        jsonPath = "$.*",
        before = arrayOf("""
            provider "azurerm" {
              features {
                key_vault {
                  purge_soft_delete_on_destroy = true
                }
              }
              somethingElse {
              }
              attr = 1
            }
        """.trimIndent()),
        after = arrayOf("""
        """.trimIndent()),
        printMatches = true
    )

    @Suppress("SameParameterValue")
    private fun assertMatched(before: Array<String>, after: Array<String>, jsonPath: String, printMatches: Boolean = false) {
        val results = visit(before, jsonPath, printMatches)
        assertThat(results).hasSize(after.size)
        for (n in results.indices) {
            assertThat(results[n]).isEqualTo(after[n])
        }
    }

    private fun visit(before: Array<String>, jsonPath: String, printMatches: Boolean): List<String> {
        val ctx = InMemoryExecutionContext { it.printStackTrace() }
        val documents = HclParser.builder().build().parse(ctx, *before)
        if (documents.isEmpty()) {
            return emptyList()
        }
        val matcher = JsonPathMatcher(jsonPath)

        val results = ArrayList<String>()
        documents.forEach {
            object : HclIsoVisitor<MutableList<String>>() {
                override fun visitAttribute(attribute: Hcl.Attribute, p: MutableList<String>): Hcl.Attribute {
                    val a = super.visitAttribute(attribute, p)
                    if (matcher.matches(cursor)) {
                        val match = a.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitAttribute")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return a
                }

                override fun visitAttributeAccess(
                    attributeAccess: Hcl.AttributeAccess,
                    p: MutableList<String>
                ): Hcl.AttributeAccess {
                    val a = super.visitAttributeAccess(attributeAccess, p)
                    if (matcher.matches(cursor)) {
                        val match = a.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitAttributeAccess")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return a
                }

                override fun visitBinary(binary: Hcl.Binary, p: MutableList<String>): Hcl.Binary {
                    val b = super.visitBinary(binary, p)
                    if (matcher.matches(cursor)) {
                        val match = b.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitBinary")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return b
                }

                override fun visitBlock(block: Hcl.Block, p: MutableList<String>): Hcl.Block {
                    val b = super.visitBlock(block, p)
                    if (matcher.matches(cursor)) {
                        val match = b.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitBlock")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return b
                }

                override fun visitConditional(conditional: Hcl.Conditional, p: MutableList<String>): Hcl.Conditional {
                    val c = super.visitConditional(conditional, p)
                    if (matcher.matches(cursor)) {
                        val match = c.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitConditional")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return c
                }

                override fun visitConfigFile(configFile: Hcl.ConfigFile, p: MutableList<String>): Hcl.ConfigFile {
                    val c = super.visitConfigFile(configFile, p)
                    if (matcher.matches(cursor)) {
                        val match = c.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitConfigFile")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return c
                }

                override fun visitEmpty(empty: Hcl.Empty, p: MutableList<String>): Hcl.Empty {
                    val e = super.visitEmpty(empty, p)
                    if (matcher.matches(cursor)) {
                        val match = e.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitEmpty")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return e
                }

                override fun visitForIntro(forIntro: Hcl.ForIntro, p: MutableList<String>): Hcl.ForIntro {
                    val f = super.visitForIntro(forIntro, p)
                    if (matcher.matches(cursor)) {
                        val match = f.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitForIntro")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return f
                }

                override fun visitForObject(forObject: Hcl.ForObject, p: MutableList<String>): Hcl.ForObject {
                    val f = super.visitForObject(forObject, p)
                    if (matcher.matches(cursor)) {
                        val match = f.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitForObject")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return f
                }

                override fun visitForTuple(forTuple: Hcl.ForTuple, p: MutableList<String>): Hcl.ForTuple {
                    val f = super.visitForTuple(forTuple, p)
                    if (matcher.matches(cursor)) {
                        val match = f.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitForTuple")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return f
                }

                override fun visitFunctionCall(
                    functionCall: Hcl.FunctionCall,
                    p: MutableList<String>
                ): Hcl.FunctionCall {
                    val f = super.visitFunctionCall(functionCall, p)
                    if (matcher.matches(cursor)) {
                        val match = f.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitFunctionCall")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return f
                }

                override fun visitHeredocTemplate(
                    heredocTemplate: Hcl.HeredocTemplate,
                    p: MutableList<String>
                ): Hcl.HeredocTemplate {
                    val h = super.visitHeredocTemplate(heredocTemplate, p)
                    if (matcher.matches(cursor)) {
                        val match = h.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitHeredocTemplate")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return h
                }

                override fun visitIdentifier(identifier: Hcl.Identifier, p: MutableList<String>): Hcl.Identifier {
                    val i = super.visitIdentifier(identifier, p)
                    if (matcher.matches(cursor)) {
                        val match = i.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitIdentifier")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return i
                }

                override fun visitIndex(index: Hcl.Index, p: MutableList<String>): Hcl.Index {
                    val i = super.visitIndex(index, p)
                    if (matcher.matches(cursor)) {
                        val match = i.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitIndex")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return i
                }

                override fun visitIndexPosition(
                    indexPosition: Hcl.Index.Position,
                    p: MutableList<String>
                ): Hcl.Index.Position {
                    val i = super.visitIndexPosition(indexPosition, p)
                    if (matcher.matches(cursor)) {
                        val match = i.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitIndexPosition")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return i
                }

                override fun visitLiteral(literal: Hcl.Literal, p: MutableList<String>): Hcl.Literal {
                    val l = super.visitLiteral(literal, p)
                    if (matcher.matches(cursor)) {
                        val match = l.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitLiteral")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return l
                }

                override fun visitObjectValue(objectValue: Hcl.ObjectValue, p: MutableList<String>): Hcl.ObjectValue {
                    val o = super.visitObjectValue(objectValue, p)
                    if (matcher.matches(cursor)) {
                        val match = o.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitObjectValue")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return o
                }

                override fun visitParentheses(parentheses: Hcl.Parentheses, p: MutableList<String>): Hcl.Parentheses {
                    val pp = super.visitParentheses(parentheses, p)
                    if (matcher.matches(cursor)) {
                        val match = pp.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitParentheses")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return pp
                }

                override fun visitQuotedTemplate(
                    template: Hcl.QuotedTemplate,
                    p: MutableList<String>
                ): Hcl.QuotedTemplate {
                    val q = super.visitQuotedTemplate(template, p)
                    if (matcher.matches(cursor)) {
                        val match = q.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitQuotedTemplate")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return q
                }

                override fun visitSplat(splat: Hcl.Splat, p: MutableList<String>): Hcl.Splat {
                    val s = super.visitSplat(splat, p)
                    if (matcher.matches(cursor)) {
                        val match = s.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitSplat")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return s
                }

                override fun visitSplatOperator(
                    splatOperator: Hcl.Splat.Operator,
                    p: MutableList<String>
                ): Hcl.Splat.Operator {
                    val s = super.visitSplatOperator(splatOperator, p)
                    if (matcher.matches(cursor)) {
                        val match = s.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitSplatOperator")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return s
                }

                override fun visitTemplateInterpolation(
                    template: Hcl.TemplateInterpolation,
                    p: MutableList<String>
                ): Hcl.TemplateInterpolation {
                    val t = super.visitTemplateInterpolation(template, p)
                    if (matcher.matches(cursor)) {
                        val match = t.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitSplatOperator")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return t
                }

                override fun visitTuple(tuple: Hcl.Tuple, p: MutableList<String>): Hcl.Tuple {
                    val t = super.visitTuple(tuple, p)
                    if (matcher.matches(cursor)) {
                        val match = t.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitSplatOperator")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return t
                }

                override fun visitUnary(unary: Hcl.Unary, p: MutableList<String>): Hcl.Unary {
                    val u = super.visitUnary(unary, p)
                    if (matcher.matches(cursor)) {
                        val match = u.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitSplatOperator")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return u
                }

                override fun visitVariableExpression(
                    variableExpression: Hcl.VariableExpression,
                    p: MutableList<String>
                ): Hcl.VariableExpression {
                    val v = super.visitVariableExpression(variableExpression, p)
                    if (matcher.matches(cursor)) {
                        val match = v.printTrimmed(cursor.parentOrThrow)
                        if (printMatches) {
                            println("matched in visitVariableExpression")
                            println(match)
                            println()
                        }
                        p.add(match)
                    }
                    return v
                }
            }.visit(it, results)
        }
        return results
    }
}
