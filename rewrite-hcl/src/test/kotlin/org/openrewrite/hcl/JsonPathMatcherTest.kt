package org.openrewrite.hcl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
