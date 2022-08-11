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
import org.junit.jupiter.api.Test
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
}
