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
import org.openrewrite.config.Environment
import org.openrewrite.scheduling.DirectScheduler
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

interface RecipeTest<T : SourceFile> {
    val recipe: Recipe?
        get() = null

    val parser: Parser<T>?
        get() = null

    val executionContext: ExecutionContext
        get() = InMemoryExecutionContext { t: Throwable -> fail<Any>("Failed to run parse sources or recipe", t) }

    @Suppress("UNCHECKED_CAST")
    fun assertChangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        val inputs = arrayOf(before.trimIndentPreserveCRLF()) + dependsOn.map { s -> s.trimIndentPreserveCRLF() }
        val sources = parser.parse(executionContext, *inputs)

        assertThat(sources.size)
            .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputs.size)

        assertChangedBase(recipe, executionContext, sources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    @Suppress("UNCHECKED_CAST")
    fun assertChangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        before: File,
        relativeTo: Path? = null,
        dependsOn: Array<File> = emptyArray(),
        after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        val sources = parser.parse(
            listOf(before).plus(dependsOn).map { it.toPath() },
            relativeTo,
            executionContext
        )
        val inputSize = 1 + dependsOn.size
        assertThat(sources.size)
            .`as`("The parser was provided with $inputSize inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputSize)

        assertChangedBase(recipe, executionContext, sources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertChangedBase(
        recipe: Recipe,
        executionContext: ExecutionContext,
        sources: List<SourceFile>,
        after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        if (recipe !is AdHocRecipe) {
            val recipeSerializer = RecipeSerializer()
            assertThat(recipeSerializer.read(recipeSerializer.write(recipe)))
                .`as`("Recipe must be serializable/deserializable")
                .isEqualTo(recipe)
        }

        val recipeSchedulerCheckingExpectedCycles =
            RecipeSchedulerCheckingExpectedCycles(DirectScheduler.common(), expectedCyclesThatMakeChanges)

        var results = recipe.run(
            sources,
            executionContext,
            recipeSchedulerCheckingExpectedCycles,
            cycles,
            expectedCyclesThatMakeChanges + 1
        ).results

        results = results.filter { it.before == sources.first() }
        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.first()

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        val actual = result.after!!.printAll()
        val expected = after.trimIndentPreserveCRLF()
        assertThat(actual).isEqualTo(expected)
        afterConditions(result.after as T)

        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun assertUnchangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        before: String,
        dependsOn: Array<String> = emptyArray()
    ) {
        val inputs = arrayOf(before.trimIndentPreserveCRLF()) + dependsOn.map { s -> s.trimIndentPreserveCRLF() }
        val sources = parser.parse(executionContext, *inputs)

        assertThat(sources.size)
            .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputs.size)

        assertUnchangedBase(recipe,  executionContext, sources)
    }

    fun assertUnchangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        before: File,
        relativeTo: Path? = null,
        dependsOn: Array<File> = emptyArray()
    ) {
        val sources = parser.parse(
            listOf(before).plus(dependsOn).map { it.toPath() },
            relativeTo,
            executionContext
        )

        assertUnchangedBase(recipe, executionContext, sources)
    }

    private fun assertUnchangedBase(
        recipe: Recipe,
        executionContext: ExecutionContext,
        sources: List<SourceFile>,
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = sources.first()
        val results = recipe
            .run(
                sources,
                executionContext
            ).results

        results.filter { result -> result.before == source }
            .forEach { result ->
                if (result.diff().isEmpty()) {
                    fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
                }
                assertThat(result.after?.printAll())
                    .`as`("The recipe must not make changes")
                    .isEqualTo(result.before?.printAll())
            }
    }

    fun toRecipe(supplier: () -> TreeVisitor<*, ExecutionContext>): Recipe {
        return AdHocRecipe(supplier)
    }

    class AdHocRecipe(private val visitor: () -> TreeVisitor<*, ExecutionContext>) : Recipe() {
        override fun getDisplayName(): String = "Ad hoc recipe"
        override fun getVisitor(): TreeVisitor<*, ExecutionContext> = visitor()
    }

    private class RecipeSchedulerCheckingExpectedCycles(
        private val delegate: RecipeScheduler,
        private val expectedCyclesThatMakeChanges: Int
    ) : RecipeScheduler {
        var cyclesThatResultedInChanges = 0

        override fun <T : Any?> schedule(fn: Callable<T>): CompletableFuture<T> {
            return delegate.schedule(fn)
        }

        override fun <S : SourceFile?> scheduleVisit(
            runStats: RecipeRunStats,
            recipeStack: Stack<Recipe>,
            before: MutableList<S>,
            ctx: ExecutionContext,
            recipeThatDeletedSourceFile: MutableMap<UUID, Stack<Recipe>>
        ): MutableList<S> {
            ctx.putMessage("cyclesThatResultedInChanges", cyclesThatResultedInChanges)
            val afterList = delegate.scheduleVisit(runStats, recipeStack, before, ctx, recipeThatDeletedSourceFile)
            if (afterList !== before) {
                cyclesThatResultedInChanges = cyclesThatResultedInChanges.inc()
                if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges &&
                    before.isNotEmpty() && afterList.isNotEmpty()
                ) {
                    for (i in before.indices) {
                        assertThat(afterList[i]!!.printAllTrimmed())
                            .`as`(
                                "Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges == 1) "" else "s"}, " +
                                        "but took at least one more cycle. Between the last two executed cycles there were changes."
                            )
                            .isEqualTo(before[i]!!.printAllTrimmed())
                    }
                }
            }
            return afterList
        }

        fun verify() {
            if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
                fail("Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges > 1) "s" else ""}, but took $cyclesThatResultedInChanges cycle${if (cyclesThatResultedInChanges > 1) "s" else ""}.")
            }
        }
    }

    private fun String.trimIndentPreserveCRLF() = replace('\r', '⏎').trimIndent().replace('⏎', '\r')

    fun fromRuntimeClasspath(recipe: String): Recipe = Environment.builder()
        .scanRuntimeClasspath()
        .build()
        .activateRecipes(recipe)
}
