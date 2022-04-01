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

    fun XmlParser.parse(markers : List<Marker> = emptyList(), @Language("xml") vararg sources: String): List<Xml.Document> {
        return parse(executionContext, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(markers)
        }
    }

    fun assertChanged(
        recipe: Recipe,
        ctx: ExecutionContext = executionContext,
        before: Xml.Document,
        additionalSources: List<SourceFile>,
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}