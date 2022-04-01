package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.xml.tree.Xml

interface GroovyTestingSupport : RecipeTestingSupport {

    val groovyParser: GroovyParser
        get() = GroovyParser.builder().build()

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: Xml.Document,
        additionalSources: List<SourceFile>,
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { },
    ) {
        assertChangedBase(recipe, executionContext, before, additionalSources, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}