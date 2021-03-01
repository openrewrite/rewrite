/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.config.ConfiguredRecipeDescriptor
import org.openrewrite.config.Environment
import org.openrewrite.config.RecipeDescriptor
import org.openrewrite.style.NamedStyles
import java.util.*

class EnvironmentTest {

    @Test
    fun listRecipeDescriptors() {
        val env = Environment.builder().scanClasspath(Collections.emptySet()).build()
        val recipeDescriptors = env.listRecipeDescriptors()
        recipeDescriptors.forEach {
            println()
            printRecipeDescriptor(it)
        }
    }

    private fun printRecipeDescriptor(recipe: RecipeDescriptor, indent: String = "") {
        println("name: ${recipe.name}")
        println("displayName: ${recipe.displayName}")
        println("description: ${recipe.description}")
        println("tags: ${recipe.tags}")
        if (recipe.options.isNotEmpty()) {
            println("options: ")
            recipe.options.forEach {
                println()
                println("\tname: ${it.name}")
                println("\tdisplayName: ${it.displayName}")
                println("\ttype: ${it.type}")
                println("\tdescription: ${it.description}")
            }
        }
        if (recipe.recipeList.isNotEmpty()) {
            println("recipeList:")
            recipe.recipeList.forEach { printConfiguredRecipeDescriptor(it, "$indent    ") }
        }
    }

    private fun printConfiguredRecipeDescriptor(recipe: ConfiguredRecipeDescriptor, indent: String = "") {
        println()
        println("${indent}name: ${recipe.name}")
        println("${indent}displayName: ${recipe.displayName}")
        println("${indent}parameters: ")
        recipe.options.forEach {
            println()
            println("${indent}\tname: ${it.name}")
            println("${indent}\ttype: ${it.type}")
            println("${indent}\tvalue: ${it.value}")
        }
    }

    @Test
    fun listStyles() {
        val env = Environment.builder().scanClasspath(Collections.emptySet()).build()
        val styles = env.listStyles()
        styles.forEach {
            println()
            printStyle(it)
        }
    }

    private fun printStyle(style: NamedStyles) {
        println()
        println("name: ${style.name}")
        println("displayName: ${style.displayName}")
        println("description: ${style.description}")
        println("tags: ${style.tags}")
    }
}
