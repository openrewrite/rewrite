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
import org.openrewrite.scheduling.ForkJoinScheduler
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ForkJoinPool

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
        expectedCyclesThatMakeChanges: Int = cycles - 1,
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
        val sources = parser!!.parse(
            InMemoryExecutionContext { t: Throwable -> fail<Any>("Parser threw an exception", t) },
            *inputs
        )

        assertThat(sources.size)
            .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputs.size)

        val recipeSchedulerCheckingExpectedCycles =
            RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler(ForkJoinPool(1)), expectedCyclesThatMakeChanges)

        var results = recipe.run(
                sources,
                InMemoryExecutionContext { t: Throwable? -> fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesThatMakeChanges + 1
            )

        results = results.filter { it.before == sources.first() }

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.first()

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.print(treePrinter ?: TreePrinter.identity<Any>(), null))
            .isEqualTo(after.trimIndent())
        afterConditions(result.after as T)

        recipeSchedulerCheckingExpectedCycles.verify()
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

        val recipeSchedulerCheckingExpectedCycles =
            RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler.common(), expectedCyclesToComplete)
        val results = recipe!!
            .run(
                listOf(source),
                InMemoryExecutionContext { t: Throwable -> fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesToComplete + 1
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

        recipeSchedulerCheckingExpectedCycles.verify()
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
        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler.common(), 0)
        val results = recipe!!
            .run(
                listOf(source),
                InMemoryExecutionContext { t -> t.printStackTrace() },
                recipeSchedulerCheckingExpectedCycles,
                2,
                2
            )

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

        recipeSchedulerCheckingExpectedCycles.verify()
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
        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler.common(), 0)
        val results = recipe!!
            .run(
                listOf(source),
                InMemoryExecutionContext { t -> fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                2,
                2
            )

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

        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun TreeVisitor<*, ExecutionContext>.toRecipe() = AdHocRecipe(this)

    class AdHocRecipe(private val visitor: TreeVisitor<*, ExecutionContext>) : Recipe() {
        override fun getDisplayName(): String = "Ad hoc recipe"
        override fun getVisitor(): TreeVisitor<*, ExecutionContext> = visitor
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
            recipe: Recipe,
            before: MutableList<S>,
            ctx: ExecutionContext,
            recipeThatDeletedSourceFile: MutableMap<UUID, Recipe>
        ): MutableList<S> {
            ctx.putMessage("cyclesThatResultedInChanges", cyclesThatResultedInChanges)
            val afterList = delegate.scheduleVisit(recipe, before, ctx, recipeThatDeletedSourceFile)
            if (afterList !== before) {
                cyclesThatResultedInChanges = cyclesThatResultedInChanges.inc()
                if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges &&
                    before.isNotEmpty() && afterList.isNotEmpty()
                ) {
                    assertThat(afterList[0]!!.printTrimmed())
                        .`as`(
                            "Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges == 1) "" else "s"}, " +
                                    "but took at least one more cycle. Between the last two executed cycles there were changes."
                        )
                        .isEqualTo(before[0]!!.printTrimmed())
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
}
