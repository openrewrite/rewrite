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
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextParser
import org.openrewrite.text.PlainTextVisitor
import java.util.concurrent.atomic.AtomicInteger

class TreeObserverTest {

    @Test
    fun observePropertyChange() {
        val t = PlainTextParser().parse(
            """
            hello jon
        """.trimIndent()
        )

        val observed = AtomicInteger(0)

        val ctx = InMemoryExecutionContext()
        ctx.addObserver(TreeObserver.Subscription(object : TreeObserver {
            override fun propertyChanged(
                property: String,
                cursor: Cursor,
                newTree: Tree,
                oldValue: Any,
                newValue: Any
            ): Tree {
                if (property == "text") {
                    observed.incrementAndGet()
                }
                return newTree
            }
        }).subscribeToType(PlainText::class.java))

        object : Recipe() {
            override fun getDisplayName(): String = "Change hello"

            override fun getVisitor(): PlainTextVisitor<ExecutionContext> =
                object : PlainTextVisitor<ExecutionContext>() {
                    override fun visitText(text: PlainText, p: ExecutionContext): PlainText {
                        return text.withText("hello jonathan")
                    }
                }
        }.run(t, ctx)

        assertThat(observed.get()).isEqualTo(1)
    }
}
