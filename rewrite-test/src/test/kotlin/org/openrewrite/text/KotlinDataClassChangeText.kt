/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.text

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Validated

/**
 * Should be equivalent in functionality to org.openrewrite.text.ChangeText
 * Exists only so that a test may be written to guarantee that our serialization/deserialization supports kotlin data classes
 */
data class KotlinDataClassChangeText (
    val toText: String?,
) : Recipe() {
    override fun getDisplayName(): String = name

    override fun getDescription(): String {
        return "Should be equivalent in functionality to org.openrewrite.text.ChangeText " +
                "Exists only so that a test may be written to guarantee that our serialization/deserialization supports kotlin data classes."
    }
    override fun getTags() = mutableSetOf("plain text")
    override fun getVisitor(): PlainTextVisitor<ExecutionContext> = ChangeTextVisitor(toText!!)
    override fun validate(): Validated = Validated.required("toText", toText)

    private class ChangeTextVisitor(val toText: String) : PlainTextVisitor<ExecutionContext>() {
        override fun preVisit(tree: PlainText, ctx: ExecutionContext?): PlainText {
            return tree.withText(toText)
        }
    }
}
