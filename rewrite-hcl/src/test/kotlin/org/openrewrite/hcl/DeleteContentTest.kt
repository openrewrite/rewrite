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
package org.openrewrite.hcl

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.hcl.tree.Expression
import org.openrewrite.hcl.tree.Hcl
import org.openrewrite.test.RewriteTest

class DeleteContentTest : RewriteTest {

    @Test
    fun deleteBlock() = rewriteRun(
        { spec -> spec.recipe(DeleteContent("$.before")) },
        hcl(
            """
            after {
            }
            before {
            }
            """,
                """
                after {
                }
            """
        )
    )
}
