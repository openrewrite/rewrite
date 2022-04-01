package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.properties.PropertiesParser


interface PropertyTestingSupport : RecipeTestingSupport  {

    val propertyParser: PropertiesParser
        get() = PropertiesParser()

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: org.openrewrite.properties.tree.Properties.File,
        additionalSources: List<SourceFile>,
        @Language("properties") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (org.openrewrite.properties.tree.Properties.File) -> Unit = { }
    ) {
        assertChangedBase(recipe, executionContext, before, additionalSources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}