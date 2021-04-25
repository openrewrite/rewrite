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
package org.openrewrite

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.fail
import java.io.File

interface RecipeTest {
    val recipe: Recipe?
        get() = null

    val treePrinter: TreePrinter<*>?
        get() = null

    val parser: Parser<*>?
        get() = null

    fun assertChanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String,
        cycles: Int = 2
    ) {
        assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SourceFile> assertChanged(
        parser: Parser<T>? = this.parser as Parser<T>?,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String,
        cycles: Int = 2,
        expectedCyclesToComplete: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        if (recipe !is AdHocRecipe) {
            val recipeSerializer = RecipeSerializer()
            assertThat(recipeSerializer.read(recipeSerializer.write(recipe!!)))
                .`as`("Recipe must be serializable/deserializable")
                .isEqualTo(recipe)
        }

        val inputs = arrayOf(before.trimIndent()) + dependsOn
        val sources = parser!!.parse(*inputs)
        assertThat(sources.size)
                .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
                .isEqualTo(inputs.size)

        val results = RecipeCheckingExpectedCycles(recipe, expectedCyclesToComplete)
            .run(
                sources,
                InMemoryExecutionContext { t: Throwable? -> fail<Any>("Recipe threw an exception", t) },
                cycles
            )
            .filter { it.before == sources.first() }

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.first()

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.print(treePrinter ?: TreePrinter.identity<Any>(), null))
            .isEqualTo(after.trimIndent())
        afterConditions(result.after as T)
    }

    fun assertChanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray(),
        after: String,
        cycles: Int = 2
    ) {
        assertChanged(parser, recipe, before, dependsOn, after, cycles) {}
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SourceFile> assertChanged(
        parser: Parser<T>? = this.parser as Parser<T>?,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray(),
        after: String,
        cycles: Int = 2,
        expectedCyclesToComplete: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val inputs = (arrayOf(before) + dependsOn).map { it.toPath() }.toList()
        val sources = parser!!.parse(inputs, null, InMemoryExecutionContext())
        assertThat(sources.size)
                .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
                .isEqualTo(inputs.size)

        val source = sources.first()

        val results = RecipeCheckingExpectedCycles(recipe!!, expectedCyclesToComplete).run(
            listOf(source),
            InMemoryExecutionContext { t: Throwable? -> fail<Any>("Recipe threw an exception", t) },
            cycles
        )

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.printTrimmed(treePrinter ?: TreePrinter.identity<Any>()))
            .isEqualTo(after.trimIndent())
        afterConditions(result.after as T)
    }

    fun assertUnchanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: String,
        dependsOn: Array<String> = emptyArray()
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val inputs = (arrayOf(before.trimIndent()) + dependsOn)
        val sources = parser!!.parse(*inputs)
        assertThat(sources.size)
                .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
                .isEqualTo(inputs.size)
        val source = sources.first()
        val results = recipe!!.run(listOf(source), InMemoryExecutionContext { t -> t.printStackTrace() }, 1)

        results.forEach { result ->
            if (result.diff(treePrinter ?: TreePrinter.identity<Any>()).isEmpty()) {
                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
            }
        }

        for (result in results) {
            assertThat(result.after?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
        }
    }

    fun assertUnchanged(
        parser: Parser<*>? = this.parser,
        recipe: Recipe? = this.recipe,
        before: File,
        dependsOn: Array<File> = emptyArray()
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull
        val inputs = (listOf(before) + dependsOn).map { it.toPath() }
        val sources = parser!!.parse(
                inputs,
            null,
            InMemoryExecutionContext()
        )
        assertThat(sources.size)
                .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
                .isEqualTo(inputs.size)
        val source = sources.first()
        val results = recipe!!.run(listOf(source), InMemoryExecutionContext { t -> t.printStackTrace() }, 1)

        results.forEach { result ->
            if (result.diff(treePrinter ?: TreePrinter.identity<Any>()).isEmpty()) {
                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
            }
        }

        for (result in results) {
            assertThat(result.after?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
        }
    }

    fun TreeVisitor<*, ExecutionContext>.toRecipe() = AdHocRecipe(this)

    class AdHocRecipe(private val visitor: TreeVisitor<*, ExecutionContext>) : Recipe() {

        override fun getDisplayName(): String {
            return "Ad hoc recipe"
        }

        override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
            return visitor
        }
    }

    private class RecipeCheckingExpectedCycles(private val recipe: Recipe, private val expectedCyclesToComplete: Int): Recipe() {
        private var executedCycles = 0

        override fun getDisplayName(): String = "Check expected cycles"

        init {
            doNext(recipe)
        }

        override fun visit(before: List<SourceFile>, ctx: ExecutionContext): List<SourceFile> {
            val afterList = recipe.visit(before, ctx)
            executedCycles = executedCycles.inc()
            if(executedCycles > expectedCyclesToComplete && afterList != before) {
                fail("Expected recipe to complete in $expectedCyclesToComplete cycle")
            }
            return afterList
        }
    }
}
