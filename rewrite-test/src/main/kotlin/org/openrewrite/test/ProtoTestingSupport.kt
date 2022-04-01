package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.marker.Marker
import org.openrewrite.protobuf.ProtoParser
import org.openrewrite.protobuf.tree.Proto

interface ProtoTestingSupport : RecipeTestingSupport{

    val protoParser : ProtoParser
        get() = ProtoParser()

    fun ProtoParser.parse(
        @Language("protobuf") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): Proto.Document {
        return parse(ctx, source.trimIndent()).map {
            it.addMarkers(markers)
        }[0]
    }

    fun ProtoParser.parse(
        @Language("protobuf") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<Proto.Document> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(markers)
        }
    }

    fun assertChanged(
        before: Proto.Document,
        @Language("protobuf") after: String,
        additionalSources: List<SourceFile>,
        recipe: Recipe? = this.recipe,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Proto.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}