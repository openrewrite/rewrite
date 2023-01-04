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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class AssignmentTest implements RewriteTest {

    @Disabled("Requires function call and Convert PSI.")
    @Test
    void unaryMinus() {
        rewriteRun(
          kotlin(
            """
                val i = -1
                val l = -2L
                val f = -3.0f
                val d = -4.0
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires detection of field access.")
    @Test
    void fieldAccess() {
        rewriteRun(
          kotlin(
            """
                class Test {
                    var id: String = ""
                    fun setId(id: String) {
                        this.id = id
                    }
                }
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires support for StringConcatenationCall.")
    @Test
    void parameterizedString() {
        rewriteRun(
          kotlin(
            """
                val latest = "value"
                fun someFun() {
                    val ref = "${latest}"
                }
            """,
            isFullyParsed()
          )
        );
    }
}
