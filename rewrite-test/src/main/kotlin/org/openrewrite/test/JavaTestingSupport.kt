package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.marker.Marker

interface JavaTestingSupport : RecipeTestingSupport {

    val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    fun JavaParser.parse(
        @Language("java") source: String,
        sourceSet: String = "main",
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): J.CompilationUnit {
        setSourceSet(sourceSet)
        return parse(ctx, source.trimIndent()).map { j -> j.addMarkers(markers) }.single()
    }

    fun JavaParser.parse(
        @Language("java") vararg sources: String,
        sourceSet: String = "main",
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<J.CompilationUnit> {
        setSourceSet(sourceSet)
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map { j -> j.addMarkers(markers) }
    }

    fun assertChanged(
        before: J.CompilationUnit,
        @Language("java") after: String,
        additionalSources: List<SourceFile> = emptyList(),
        recipe: Recipe? = this.recipe,
        executionContext: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (J.CompilationUnit) -> Unit = { }
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}