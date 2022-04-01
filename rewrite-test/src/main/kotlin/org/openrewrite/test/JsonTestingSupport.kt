package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.json.JsonParser
import org.openrewrite.json.tree.Json

interface JsonTestingSupport : RecipeTestingSupport {

    val jsonParser: JsonParser
        get() = JsonParser()

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: Json.Document,
        additionalSources: List<SourceFile>,
        @Language("json") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Json.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}