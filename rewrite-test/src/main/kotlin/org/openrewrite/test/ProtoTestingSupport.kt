package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.protobuf.ProtoParser
import org.openrewrite.protobuf.tree.Proto

interface ProtoTestingSupport : RecipeTestingSupport{

    val protoParser : ProtoParser
        get() = ProtoParser()

    fun assertChanged(
        recipe: Recipe,
        executionContext: ExecutionContext,
        before: Proto.Document,
        additionalSources: List<SourceFile>,
        @Language("protobuf") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Proto.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}