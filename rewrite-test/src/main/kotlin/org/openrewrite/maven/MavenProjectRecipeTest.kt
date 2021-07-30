package org.openrewrite.maven

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.fail
import org.openrewrite.*
import org.openrewrite.internal.ListUtils
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaProvenance
import org.openrewrite.java.marker.JavaProvenance.BuildTool
import org.openrewrite.java.marker.JavaProvenance.Publication
import org.openrewrite.maven.tree.Maven
import org.openrewrite.scheduling.ForkJoinScheduler
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

/**
 * This test harness for maven recipes that operates in the context of Maven-based projects. This interface provides
 * default methods to parse additional maven/java sources files and pass those along to the recipe. There are also
 * facilities to create and set JavaProvenance on the source files.
 */
interface MavenProjectRecipeTest {
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
        recipe: Recipe? = this.recipe,
        mavenParser: MavenParser = this.mavenParser,
        @Language("XML") before: String,
        @Language("XML") after: String,
        additionalSources: Array<SourceFile> = emptyArray(),
        cycles: Int = 2,
        expectedCyclesToComplete: Int = cycles - 1,
        afterConditions: (Maven) -> Unit = { },
    ) {

        val source = mavenParser.parse(before.trimIndent()).singleOrNull()
        Assertions.assertThat(source)
            .`as`("The parser did not return a source file.")
            .isNotNull

        val sources = listOf(source) + additionalSources

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
        recipe: Recipe? = this.recipe,
        javaParser: JavaParser = this.javaParser,
        mavenParser: MavenParser = this.mavenParser,
        @Language("XML") before: String,
        additionalSources: Array<SourceFile> = emptyArray(),
    ) {
        Assertions.assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = mavenParser.parse(before).singleOrNull()
        Assertions.assertThat(source)
            .`as`("The parser did not return a source file.")
            .isNotNull

        val sources = listOf(source) + additionalSources

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

    fun javaProvenance(groupId: String,
                       artifactId: String,
                       version: String = "1.0.0",
                       sourceSet: String = "main"
    ) : JavaProvenance {

        val javaRuntimeVersion = System.getProperty("java.runtime.version")
        val javaVendor = System.getProperty("java.vm.vendor")

        return JavaProvenance(
            Tree.randomId(),
            "${groupId}:${artifactId}:${version}",
            sourceSet,
            BuildTool(BuildTool.Type.Maven, ""),
            JavaProvenance.JavaVersion(javaRuntimeVersion, javaVendor,javaRuntimeVersion,javaRuntimeVersion),
            Publication(groupId, artifactId, version)
        )
    }

    fun parseMavenFiles(mavenSources: Array<String>, mavenParser: MavenParser = this.mavenParser) : Array<SourceFile> {
        val sources = mavenParser.parse(*mavenSources)
        Assertions.assertThat(sources.size)
            .`as`("The parser was provided with ${mavenSources.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(mavenSources)
        return sources.toTypedArray()
    }

    fun parseJavaFiles(javaSources: Array<String>, parser: JavaParser = this.javaParser, javaProvenance: JavaProvenance) : Array<SourceFile> {
        var sources = javaParser.parse(*javaSources)
        Assertions.assertThat(sources.size)
            .`as`("The parser was provided with ${javaSources.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(javaSources.size)

        sources = ListUtils.map(sources) { j -> j.withMarkers(j.markers.addIfAbsent(javaProvenance)) }
        return sources.toTypedArray()
    }

    private class RecipeSchedulerCheckingExpectedCycles(
        private val delegate: RecipeScheduler,
        private val expectedCyclesThatMakeChanges: Int,
    ) : RecipeScheduler {
        var cyclesThatResultedInChanges = 0

        override fun <T : Any?> schedule(fn: Callable<T>): CompletableFuture<T> {
            return delegate.schedule(fn)
        }

        override fun <S : SourceFile?> scheduleVisit(
            recipe: Recipe,
            before: MutableList<S>,
            ctx: ExecutionContext,
            recipeThatDeletedSourceFile: MutableMap<UUID, Recipe>,
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