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

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: J.CompilationUnit,
        additionalSources: List<SourceFile>,
        @Language("java") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (J.CompilationUnit) -> Unit = { }
    ) {
        assertChangedBase(recipe, executionContext, before, additionalSources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun JavaParser.parse(
        @Language("java") vararg sources: String,
        sourceSet: String = "main",
        markers : List<Marker> = emptyList()
    ): List<J.CompilationUnit> {
        setSourceSet(sourceSet)
        return parse(executionContext, *sources.map { it.trimIndent() }.toTypedArray()).map { j -> j.addMarkers(markers) }
    }
}