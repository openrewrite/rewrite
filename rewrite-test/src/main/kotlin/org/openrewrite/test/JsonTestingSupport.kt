package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.json.JsonParser
import org.openrewrite.json.tree.Json
import org.openrewrite.marker.Marker

interface JsonTestingSupport : RecipeTestingSupport {

    val jsonParser: JsonParser
        get() = JsonParser()

    fun JsonParser.parse(
        @Language("json") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): Json.Document {
        return parse(ctx, source.trimIndent()).map {
            it.addMarkers(markers)
        }[0]
    }

    fun JsonParser.parse(
        @Language("json") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<Json.Document> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(markers)
        }
    }

    fun assertChanged(
        before: Json.Document,
        @Language("json") after: String,
        additionalSources: List<SourceFile>,
        recipe: Recipe? = this.recipe,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Json.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}