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
package org.openrewrite.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CategoryTreeTest {
    private val env = Environment.builder()
        .scanRuntimeClasspath()
        .build()

    private val categoryTree = CategoryTree.build<Int>()
        .putAll(1, env)

    @Disabled
    @Test
    fun categoryTreeFromEnvironment() {
        printTree(categoryTree, 0)
    }

    @Test
    fun getCategory() {
        assertThat(categoryTree.getCategoryOrThrow("org", "openrewrite")).isNotNull
    }

    @Test
    fun getRecipeCount() {
        assertThat(categoryTree.getCategoryOrThrow("org", "openrewrite").recipeCount)
            .isGreaterThan(10)
        assertThat(categoryTree.recipes.size).isEqualTo(categoryTree.recipeCount)
    }

    @Test
    fun getRecipesInArtificialCorePackage() {
        assertThat(categoryTree.getCategory("org", "openrewrite", "java", "core")?.recipes ?: emptyList()).isNotEmpty
    }

    @Test
    fun getRecipe() {
        assertThat(categoryTree.getRecipe("org.openrewrite.java.ChangeMethodName")).isNotNull
    }

    @Test
    fun getRecipeGroup() {
        assertThat(categoryTree.getRecipeGroup("org.openrewrite.java.ChangeMethodName") ?: -1).isEqualTo(1)
    }

    private fun printTree(categoryTree: CategoryTree<*>, level: Int) {
        println((0..level).joinToString("  ") { "" } + categoryTree.descriptor.packageName +
                " (${categoryTree.descriptor.displayName})")
        categoryTree.subtrees.forEach {
            printTree(it, level + 1)
        }
        categoryTree.recipes.forEach {
            println((0..level + 1).joinToString("  ") { "" } + ">" + it.displayName)
        }
    }
}
