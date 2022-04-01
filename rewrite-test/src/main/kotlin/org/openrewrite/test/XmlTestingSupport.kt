package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.marker.Marker
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.tree.Xml

interface XmlTestingSupport : RecipeTestingSupport {

    val xmlParser: XmlParser
        get() = XmlParser()

    fun XmlParser.parse(
        @Language("xml") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): Xml.Document {
        return parse(ctx, source.trimIndent()).map {
            it.addMarkers(markers)
        }[0]
    }

    fun XmlParser.parse(
        @Language("xml") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<Xml.Document> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(markers)
        }
    }

    fun assertChanged(
        before: Xml.Document,
        @Language("xml") after: String,
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