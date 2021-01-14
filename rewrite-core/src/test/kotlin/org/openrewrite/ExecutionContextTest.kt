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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.text.PlainText
import java.util.function.Supplier

class ExecutionContextTest {
    @Test
    fun anotherCycleIfNewMessagesInExecutionContext() {
        var cycles = 0

        object: Recipe() {
            init {
                this.processor = Supplier {
                    object : TreeProcessor<PlainText, ExecutionContext>() {
                        override fun visit(tree: Tree?, p: ExecutionContext): PlainText? {
                            if(p.pollMessage<String>("test") == null) {
                                p.putMessage("test", "test")
                            }
                            cycles = cycles.inc()
                            return super.visit(tree, p)
                        }
                    }
                }
            }
        }.run(listOf(PlainText(randomId(), "hello world", emptyList())))

        assertThat(cycles).isEqualTo(2)
    }
}
