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
package org.openrewrite

import org.junit.jupiter.api.Test
import org.openrewrite.marker.SearchResult
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs.text
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextVisitor

class ApplicabilityTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.validateRecipeSerialization(false)
    }

    @Test
    fun not() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.not(contains("z")))) },
        text("hello", "goodbye")
    )

    @Test
    fun notNot() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.not(contains("h")))) },
        text("hello")
    )


    @Test
    fun or() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.or(contains("h"), contains("z")))) },
        text("hello", "goodbye")
    )

    @Test
    fun notOr() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.or(contains("x"), contains("z")))) },
        text("hello")
    )

    @Test
    fun and() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.and(contains("h"), contains("ello")))) },
        text("hello", "goodbye")
    )

    @Test
    fun notAnd() = rewriteRun(
        { spec -> spec.recipe(recipe(Applicability.and(contains("h"), contains("z")))) },
        text("hello")
    )

    fun recipe(applicability: TreeVisitor<*, ExecutionContext>) = object : Recipe() {
        override fun getDisplayName() = "Say goodbye"
        override fun getSingleSourceApplicableTest() = applicability
        override fun getVisitor() = object : PlainTextVisitor<ExecutionContext>() {
            override fun visitText(text: PlainText, p: ExecutionContext): PlainText {
                return text.withText("goodbye")
            }
        }
    }

    fun contains(s: String) = object : PlainTextVisitor<ExecutionContext>() {
        override fun visitText(text: PlainText, p: ExecutionContext): PlainText =
            if (text.text.contains(s)) {
                SearchResult.found(text)
            } else text
    }
}
