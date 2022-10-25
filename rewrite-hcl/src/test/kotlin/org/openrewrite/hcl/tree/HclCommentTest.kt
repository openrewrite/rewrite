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

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.hcl.Assertions.hcl
import org.openrewrite.test.RewriteTest

class HclCommentTest : RewriteTest {

    @Test
    fun comment() = rewriteRun(
        hcl(
            """
                # test
                /*
                 multiline
                */
                resource {
                    # test
                    // test
                    a = 1
                }
            """
        )
    )

    @Test
    fun commentCrlf() = rewriteRun(
        hcl(
            """
                # test
                /*
                 multiline
                */
                resource {
                    # test
                    // test
                    a = 1
                }
            """.replace("\r\n", "\n").replace("\n", "\r\n")
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2079")
    fun bracesInComment() = rewriteRun(
            hcl(
            """
            locals {
                myvar = {
                  # below {attributes}
                  myattribute = "myvalue"

                  # add more attributes {here}
                }
            }
            """
            )
    )
}
