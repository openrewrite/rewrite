package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.marker.Marker
import org.openrewrite.xml.tree.Xml

interface GroovyTestingSupport : RecipeTestingSupport {

    val groovyParser: GroovyParser
        get() = GroovyParser.builder().build()

    fun GroovyParser.parse(
        @Language("groovy") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): G.CompilationUnit {
        return parse(ctx, source.trimIndent()).map { j -> j.addMarkers(markers) }.single()
    }

    fun GroovyParser.parse(
        @Language("groovy") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<G.CompilationUnit> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map { j -> j.addMarkers(markers) }
    }

    fun assertChanged(
        before: Xml.Document,
        @Language("groovy") after: String,
        additionalSources: List<SourceFile>,
        recipe: Recipe? = this.recipe,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}