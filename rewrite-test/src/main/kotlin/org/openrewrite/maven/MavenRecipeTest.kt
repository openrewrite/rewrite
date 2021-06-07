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
package org.openrewrite.maven

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.fail
import org.openrewrite.*
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.tree.Maven
import org.openrewrite.scheduling.ForkJoinScheduler
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

/**
 * Test harness for maven-base recipes. The assertChanged/assertUnchanged expect before/after to be maven pom files and
 * both methods provide the ability to add additional java and pom files to test for the presence of parent poms and
 * dependent types in the Java source.
 */
interface MavenRecipeTest {
    val recipe: Recipe?
        get() = null

    val treePrinter: TreePrinter<*>?
        get() = null

    val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    val mavenParser : MavenParser
        get() = MavenParser.builder().build()

    fun assertChanged(
        javaPaser : JavaParser = this.javaParser,
        mavenParsre : MavenParser = this.mavenParser,
        @Language("XML") before: String,
        @Language("XML") after: String,
        additionalJavaFiles : Array<String> = emptyArray(),
        additionalMavenFiles : Array<String> = emptyArray(),
        cycles: Int = 2,
        expectedCyclesToComplete: Int = cycles - 1,
        afterConditions: (Maven) -> Unit = { }) {

        val mavenFiles = arrayOf(before) + additionalMavenFiles
        val sources = mavenParser.parse(*mavenFiles) + javaParser.parse(*additionalJavaFiles)

        val inputSize = mavenFiles.size + additionalJavaFiles.size
        Assertions.assertThat(sources.size)
            .`as`("The parser was provided with ${inputSize} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputSize)

        val source = sources.first()

        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(
            ForkJoinScheduler(ForkJoinPool(1)), expectedCyclesToComplete)

        val results = recipe!!
            .run(
                sources,
                InMemoryExecutionContext { t: Throwable -> Assertions.fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesToComplete + 1
            )

        if (results.isEmpty()) {
            Assertions.fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        Assertions.assertThat(result).`as`("The recipe must make changes").isNotNull
        Assertions.assertThat(result!!.after).isNotNull
        Assertions.assertThat(result.after!!.printTrimmed(TreePrinter.identity<Any>()))
            .isEqualTo(after.trimIndent())

        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun assertUnchanged(
        javaPaser : JavaParser = this.javaParser,
        mavenParsre : MavenParser = this.mavenParser,
        @Language("XML") before: String,
        additionalJavaFiles : Array<String> = emptyArray(),
        additionalMavenFiles : Array<String> = emptyArray(),
    ) {
        Assertions.assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val mavenFiles = arrayOf(before) + additionalMavenFiles
        val sources = mavenParser.parse(*mavenFiles) + javaParser.parse(*additionalJavaFiles)

        val inputSize = mavenFiles.size + additionalJavaFiles.size
        Assertions.assertThat(sources.size)
            .`as`("The parser was provided with ${inputSize} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputSize)

        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(
            ForkJoinScheduler(ForkJoinPool(1)),0)

        val results = recipe!!
            .run(
                sources,
                InMemoryExecutionContext { t: Throwable -> Assertions.fail<Any>("Recipe threw an exception", t) },
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
            Assertions.assertThat(result.after?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
        }

        recipeSchedulerCheckingExpectedCycles.verify()
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
                    Assertions.assertThat(afterList[0]!!.printTrimmed())
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