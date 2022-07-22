/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.hcl.HclVisitor
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

class HclAttributeTest : RewriteTest {

    @Test
    fun attribute() = rewriteRun(
        hcl("a = true")
    )

    @Test
    fun attributeValue() = rewriteRun(
        { spec ->
            spec
                .cycles(1)
                .expectedCyclesThatMakeChanges(1)
                .recipe(toRecipe {
                object : HclVisitor<ExecutionContext>() {
                    override fun visitBlock(block: Hcl.Block, ctx: ExecutionContext): Hcl {
                        assertThat(block.getAttributeValue<String>("key"))
                            .isEqualTo("hello")
                        return block.withAttributeValue("key", "goodbye")
                    }
                }
            })
        },
        hcl(
            """
                provider {
                    key = "hello"
                }
            """,
            """
                provider {
                    key = "goodbye"
                }
            """
        )
    )
}
