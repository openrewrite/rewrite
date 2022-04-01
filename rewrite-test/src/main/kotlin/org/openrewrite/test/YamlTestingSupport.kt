package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.marker.Marker
import org.openrewrite.yaml.YamlParser
import org.openrewrite.yaml.tree.Yaml

interface YamlTestingSupport : RecipeTestingSupport {
    val yamlParser: YamlParser
        get() = YamlParser()

    fun YamlParser.parse(
        @Language("yaml") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): Yaml.Documents {
        return parse(ctx, source.trimIndent()).map {
            it.addMarkers(markers)
        }[0]
    }

    fun YamlParser.parse(
        @Language("yaml") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<Yaml.Documents> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map {
            it.addMarkers(markers)
        }
    }

    fun assertChanged(
        before: Yaml.Documents,
        @Language("yaml") after: String,
        additionalSources: List<SourceFile>,
        recipe: Recipe? = this.recipe,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Yaml.Documents) -> Unit = { }
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}