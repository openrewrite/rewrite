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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs
import org.openrewrite.test.SourceSpecs.text
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextVisitor

class RecipeSchedulerTest : RewriteTest {

    @Test
    fun exceptionsCauseResult() = rewriteRun(
            { spec ->
                spec
                        .executionContext(InMemoryExecutionContext())
                        .recipe(BoomRecipe())
                        .afterRecipe { run -> assertThat(run.results[0].recipeErrors).isNotEmpty() }
            },
            text(
                    "hello",
                    "~~(org.openrewrite.BoomException: boom\n" +
                            "  org.openrewrite.BoomRecipe\$getVisitor\$1.visitText(RecipeSchedulerTest.kt:49)\n" +
                            "  org.openrewrite.BoomRecipe\$getVisitor\$1.visitText(RecipeSchedulerTest.kt:47))~~>hello"
            )
    )
}

class BoomRecipe : Recipe() {
    override fun getDisplayName() = "We go boom"
    override fun getVisitor() = object : PlainTextVisitor<ExecutionContext>() {
        override fun visitText(text: PlainText, p: ExecutionContext): PlainText {
            throw BoomException()
        }
    }
}

/**
 * Simplified exception that only displays stack trace elements within the [BoomRecipe].
 */
class BoomException : RuntimeException("boom") {
    override fun getStackTrace() = super.getStackTrace()
            .filter { it.className.startsWith(BoomRecipe::class.java.name) }
            .toTypedArray()
}
