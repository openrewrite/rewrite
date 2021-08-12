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

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.fail
import org.openrewrite.internal.StringUtils
import org.openrewrite.scheduling.ForkJoinScheduler
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

interface RecipeTest <T: SourceFile> {
    val recipe: Recipe?
        get() = null

    val treePrinter: TreePrinter<*>?
        get() = null

    val parser: Parser<T>?
        get() = null

    val executionContext: ExecutionContext
        get() = InMemoryExecutionContext { t: Throwable -> fail<Any>("Failed to run parse sources or recipe", t) }

    @Suppress("UNCHECKED_CAST")
    fun assertChangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        before: String,
        dependsOn: Array<String> = emptyArray(),
        after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        val inputs = arrayOf(before.trimIndent()) + dependsOn.map(String::trimIndent)
        val sources = parser.parse(executionContext, *inputs )

        assertThat(sources.size)
            .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputs.size)

        assertChangedBase(recipe, sources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    @Suppress("UNCHECKED_CAST")
    fun assertChangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
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

        assertChangedBase(recipe, sources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertChangedBase(
        recipe: Recipe = this.recipe!!,
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
            RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler(ForkJoinPool(1)), expectedCyclesThatMakeChanges)

        var results = recipe.run(
            sources,
            executionContext,
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
        val actual = result.after!!.print(treePrinter ?: TreePrinter.identity<Any>(), null)
        val expected = after.trimIndent()
        assertThat(actual).isEqualTo(expected)
        afterConditions(result.after as T)

        recipeSchedulerCheckingExpectedCycles.verify()
        writeExampleReport(sources, recipe, result)
    }

    private fun writeExampleReport(sources: List<SourceFile>, recipe: Recipe, result: Result) {
        // Focus on simple examples by excluding any that require more than a single input source
        if(sources.size > 1 || recipe.name.contains("$")) {
            return
        }

        // This property points to a directory where a report can be placed
        val exampleOutput = System.getProperty("org.openrewrite.TestExampleOutputDir") ?: return
        val exampleOutputDir = File(exampleOutput)
        if(!exampleOutputDir.exists() && !exampleOutputDir.mkdirs()) {
            return
        }

        // Figure out which test class+method is producing this example
        val testFrame = Thread.currentThread().stackTrace.asSequence()
                .drop(2) // skip the frames for Thread.getStackTrace(), writeExampleReport()
                .first { !it.methodName.startsWith("assertChanged") }
                ?: return

        // Tests implemented as default methods on interfaces, such as those in rewrite-test, will be in an inner class called $DefaultImpls
        val testClassName = testFrame.className.removeSuffix("\$DefaultImpls")
        val testMethodName = testFrame.methodName
        val reportProps = Properties().apply {
            set("testClassName", testClassName)
            set("testMethodName", testMethodName)
            set("before", result.before!!.printTrimmed())
            set("after", result.after!!.printTrimmed())
            set("recipe", recipe.name)
            recipe.descriptor.options.forEach { recipeOption ->
                set("recipeOption.${recipeOption.name}.type", recipeOption.type)
                // Use reflection to get the actual value
                val recipeFieldGetter = recipe::class.functions.find {
                    val capitalRecipeName = StringUtils.capitalize(recipeOption.name)
                    it.name == "get$capitalRecipeName" || it.name == "is$capitalRecipeName"
                }

                if(recipeFieldGetter != null) {
                    val recipeFieldValue = recipeFieldGetter.call(recipe)
                    set("recipeOption.${recipeOption.name}", recipeFieldValue.toString())
                }
            }
        }

        File(exampleOutputDir, "${recipe.name}.$testMethodName.properties").outputStream().use { reportOutputStream ->
            reportProps.store(reportOutputStream, null)
        }
    }

    fun assertUnchangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        before: String,
        dependsOn: Array<String> = emptyArray()
    ) {
        val inputs = arrayOf(before.trimIndent()) + dependsOn.map(String::trimIndent)
        val sources = parser.parse(executionContext, *inputs)

        assertThat(sources.size)
            .`as`("The parser was provided with ${inputs.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(inputs.size)

        assertUnchangedBase(recipe, sources)
    }

    fun assertUnchangedBase(
        parser: Parser<T> = this.parser!!,
        recipe: Recipe = this.recipe!!,
        before: File,
        relativeTo: Path? = null,
        dependsOn: Array<File> = emptyArray()
    ) {
        val sources = parser.parse(
                listOf(before).plus(dependsOn).map { it.toPath() },
            relativeTo,
            executionContext
        )

        assertUnchangedBase(recipe, sources)
    }

    private fun assertUnchangedBase(
        recipe: Recipe = this.recipe!!,
        sources: List<SourceFile>,
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = sources.first()
        val results = recipe
            .run(
                sources,
                executionContext
            )

        results.filter { result -> result.before == source }
            .forEach { result ->
                if (result.diff(treePrinter ?: TreePrinter.identity<Any>()).isEmpty()) {
                    fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
                }
                assertThat(result.after?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
                    .`as`("The recipe must not make changes")
                    .isEqualTo(result.before?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
            }
    }

    fun toRecipe(supplier : () -> TreeVisitor<*, ExecutionContext>) : Recipe {
        return AdHocRecipe(supplier)
    }

    class AdHocRecipe(private val visitor : () -> TreeVisitor<*, ExecutionContext>) : Recipe() {
        override fun getDisplayName(): String = "Ad hoc recipe"
        override fun getVisitor(): TreeVisitor<*, ExecutionContext> = visitor()
    }

    @Suppress("UNCHECKED_CAST")
    fun assertChangedBase(
        recipe: Recipe = this.recipe!!,
        moderneAstLink: String,
        moderneApiBearerToken: String = apiTokenFromUserHome(),
        after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (T) -> Unit = { }
    ) {
        val treeSerializer = TreeSerializer<T>()
        val httpClient = OkHttpClient.Builder().build()
        try {
            val request = Request.Builder()
                .url(moderneAstLink)
                .header("Authorization", moderneApiBearerToken)
                .build()

            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected status $response" }
                val responseBody: ResponseBody = response.body ?:
                throw IllegalStateException("No response body")
                val source = treeSerializer.read(responseBody.byteStream())

                val recipeSchedulerCheckingExpectedCycles =
                    RecipeSchedulerCheckingExpectedCycles(ForkJoinScheduler(ForkJoinPool(1)), expectedCyclesThatMakeChanges)

                var results = recipe.run(
                    listOf(source),
                    executionContext,
                    recipeSchedulerCheckingExpectedCycles,
                    cycles,
                    expectedCyclesThatMakeChanges + 1
                )

                results = results.filter { it.before == source }

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
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    fun assertUnchanged(
        recipe: Recipe = this.recipe!!,
        moderneAstLink: String,
        moderneApiBearerToken: String = apiTokenFromUserHome()
    ) {
        val treeSerializer = TreeSerializer<T>()
        val httpClient = OkHttpClient.Builder().build()
        try {
            val request = Request.Builder()
                .url(moderneAstLink)
                .header("Authorization", moderneApiBearerToken)
                .build()

            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected status $response" }
                val responseBody: ResponseBody = response.body ?:
                throw IllegalStateException("No response body")
                val source = treeSerializer.read(responseBody.byteStream())

                val results = recipe
                    .run(
                        listOf(source),
                        executionContext,
                        2
                    )

                results.forEach { result ->
                    if (result.diff().isEmpty()) {
                        fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
                    }
                }

                for (result in results) {
                    assertThat(result.after?.print())
                        .`as`("The recipe must not make changes")
                        .isEqualTo(result.before?.print())
                }
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    fun apiTokenFromUserHome(): String {
        val tokenFile = File(System.getProperty("user.home") + "/.moderne/token.txt")
        if(!tokenFile.exists()) {
            throw IllegalStateException("No token file was not found at ~/.moderne/token.txt")
        }
        val token = tokenFile.readText().trim()
        return if(token.startsWith("Bearer ")) token else "Bearer $token"
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
                    for (i in before.indices) {
                        assertThat(afterList[i]!!.printTrimmed())
                            .`as`(
                                "Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges == 1) "" else "s"}, " +
                                        "but took at least one more cycle. Between the last two executed cycles there were changes."
                            )
                            .isEqualTo(before[i]!!.printTrimmed())
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
