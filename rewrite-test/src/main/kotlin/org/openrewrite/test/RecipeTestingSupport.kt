package org.openrewrite.test

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.fail
import org.openrewrite.*
import org.openrewrite.java.JavaParser
import org.openrewrite.marker.Marker
import org.openrewrite.scheduling.DirectScheduler
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

interface RecipeTestingSupport {

    val recipe: Recipe?
        get() = null

    val executionContext: ExecutionContext
        get() {
            return InMemoryExecutionContext {
                    t: Throwable -> Assertions.fail<Any>("Failed to run parse sources or recipe", t)
            }.apply { this.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true) }
        }

    fun <T : SourceFile> T.addMarkers(markers: List<Marker>): T {
        if (markers.isEmpty()) {
            return this
        }
        var s = this
        for (marker in markers) {
            s = s.withMarkers(s.markers.addIfAbsent(marker))
        }
        return s
    }

    fun <T: SourceFile> assertChangedBase(
        before: T,
        after: String,
        additionalSources: List<SourceFile> = emptyList(),
        recipe: Recipe? = this.recipe!!,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { },
    ) {
        Assertions.assertThat(recipe).`as`("A recipe must be specified").isNotNull

        if (recipe !is RecipeTest.AdHocRecipe) {
            val recipeSerializer = RecipeSerializer()
            Assertions.assertThat(recipeSerializer.read(recipeSerializer.write(recipe)))
                .`as`("Recipe must be serializable/deserializable")
                .isEqualTo(recipe)
        }

        val recipeSchedulerCheckingExpectedCycles =
            RecipeSchedulerCheckingExpectedCycles(DirectScheduler.common(), expectedCyclesThatMakeChanges)

        var results = recipe?.run(
            listOf(before) + additionalSources,
            ctx,
            recipeSchedulerCheckingExpectedCycles,
            cycles,
            expectedCyclesThatMakeChanges + 1
        )

        results = results!!.filter { it.before == before }
        if (results.isEmpty()) {
            Assertions.fail<Any>("The recipe must make changes")
        }

        val result = results.first()

        Assertions.assertThat(result).`as`("The recipe must make changes").isNotNull
        Assertions.assertThat(result!!.after).isNotNull
        val actual = result.after!!.printAll()
        val expected = after.trimIndentPreserveCRLF()
        Assertions.assertThat(actual).isEqualTo(expected)
        @Suppress("UNCHECKED_CAST")
        afterConditions(result.after as T)
        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun assertUnchanged(
        before: SourceFile,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        additionalSources: List<SourceFile> = emptyList(),
    ) {
        Assertions.assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val results = recipe
            .run(
                listOf(before) + additionalSources,
                executionContext
            )

        results.filter { result -> result.before == before }
            .forEach { result ->
                if (result.diff().isEmpty()) {
                    fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
                }
                Assertions.assertThat(result.after?.printAll())
                    .`as`("The recipe must not make changes")
                    .isEqualTo(result.before?.printAll())
            }
    }

    private fun String.trimIndentPreserveCRLF() = replace('\r', '⏎').trimIndent().replace('⏎', '\r')

    private class RecipeSchedulerCheckingExpectedCycles(private val delegate: RecipeScheduler, private val expectedCyclesThatMakeChanges: Int) : RecipeScheduler {

        var cyclesThatResultedInChanges = 0

        override fun <T : Any?> schedule(fn: Callable<T>): CompletableFuture<T> {
            return delegate.schedule(fn)
        }

        override fun <S : SourceFile?> scheduleVisit(
            recipeStack: Stack<Recipe>,
            before: MutableList<S>,
            ctx: ExecutionContext,
            recipeThatDeletedSourceFile: MutableMap<UUID, Stack<Recipe>>,
        ): MutableList<S> {
            ctx.putMessage("cyclesThatResultedInChanges", cyclesThatResultedInChanges)
            val afterList = delegate.scheduleVisit(recipeStack, before, ctx, recipeThatDeletedSourceFile)
            if (afterList !== before) {
                cyclesThatResultedInChanges = cyclesThatResultedInChanges.inc()
                if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges &&
                    before.isNotEmpty() && afterList.isNotEmpty()
                ) {
                    for (i in before.indices) {
                        Assertions.assertThat(afterList[i]!!.printAllTrimmed())
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

}