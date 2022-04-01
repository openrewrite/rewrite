/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.marker.Marker
import org.openrewrite.protobuf.ProtoParser
import org.openrewrite.protobuf.tree.Proto

interface ProtoTestingSupport : RecipeTestingSupport{

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