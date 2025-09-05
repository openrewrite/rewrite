/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs.text
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextVisitor


class KotlinFromDeclarativeRecipeTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipeFromYaml(
            """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.kotlin.KotlinReplaceAllRecipeWrapper
            displayName: Kotlin Replace All Wrapper
            description: Tests a simple Kotlin recipe that replaces all text with the provided option.
            recipeList:
              - org.openrewrite.kotlin.KotlinReplaceAllRecipeJackson:
                  replacementText: "REPLACED_WITH_JACKSON"
              - org.openrewrite.kotlin.KotlinReplaceAllRecipe:
                  replacementText: "REPLACED_WITH_KOTLIN"
            """, "org.openrewrite.kotlin.KotlinReplaceAllRecipeWrapper"
        )
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
    }

    @Test
    fun `replaces all text with provided replacement argument text`() =
        rewriteRun(text("Some Text", "REPLACED_WITH_KOTLIN"))
}

@Suppress("unused")
class KotlinReplaceAllRecipeJackson @JsonCreator constructor(
    @field:Option(
        displayName = "Replacement text",
        description = "Resulting text after the replacement occurs.",
        example = "REPLACEMENT_TEXT"
    ) @param:JsonProperty("replacementText") val replacementText: String
) : Recipe() {
    override fun getDisplayName() = "Tests a simple Kotlin recipe with options and Jackson annotations"
    override fun getDescription() = "A test case for recipe authoring in Kotlin."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : PlainTextVisitor<ExecutionContext>() {
            override fun visitText(text: PlainText, p: ExecutionContext): PlainText {
                return super.visitText(text.withText(replacementText), p)
            }
        }
}

@Suppress("unused")
class KotlinReplaceAllRecipe(
    @field:Option(
        displayName = "Replacement text",
        description = "Resulting text after the replacement occurs.",
        example = "REPLACEMENT_TEXT"
    ) val replacementText: String
) : Recipe() {
    override fun getDisplayName() = "Tests a simple Kotlin recipe with options"
    override fun getDescription() = "A test case for recipe authoring in Kotlin."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : PlainTextVisitor<ExecutionContext>() {
            override fun visitText(text: PlainText, p: ExecutionContext): PlainText {
                return super.visitText(text.withText(replacementText), p)
            }
        }
}
