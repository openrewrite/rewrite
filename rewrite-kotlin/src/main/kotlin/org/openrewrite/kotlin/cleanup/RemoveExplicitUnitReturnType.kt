/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.cleanup

import org.openrewrite.Recipe
import org.openrewrite.recipe
import org.openrewrite.kotlin.marker.TypeReferencePrefix
import org.openrewrite.kotlin.tree.KotlinTypeUtils
import java.time.Duration

val RemoveExplicitUnitReturnType: Recipe = recipe(
    displayName = "Remove explicit `Unit` return type",
    description = "Kotlin functions returning `Unit` don't need to declare it explicitly; " +
        "omitting `: Unit` is idiomatic and has no effect on behavior.",
    estimatedEffortPerOccurrence = Duration.ofSeconds(20),
) {
    edit {
        kotlin {
            visitMethodDeclaration { md ->
                if (KotlinTypeUtils.isKotlinUnit(md.returnTypeExpression)) {
                    md.withReturnTypeExpression(null)
                        .withMarkers(md.markers.removeByType(TypeReferencePrefix::class.java))
                } else {
                    md
                }
            }
        }
    }
}
