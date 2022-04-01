package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.yaml.YamlParser
import org.openrewrite.yaml.tree.Yaml

interface YamlTestingSupport : RecipeTestingSupport {
    val yamlParser: YamlParser
        get() = YamlParser()

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: Yaml.Documents,
        additionalSources: List<SourceFile>,
        @Language("yaml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Yaml.Documents) -> Unit = { }
    ) {
        assertChangedBase(recipe, executionContext, before, additionalSources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}