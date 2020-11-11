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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface CommentTest {

    @Test
    fun comments(jp: JavaParser) {
        val aSrc = """
            // About me
            public class A {
            /* } */
            // }
            }
            // Trailing
        """.trimIndent()

        val a = jp.parse(aSrc)[0]
        assertEquals(aSrc, a.printTrimmed())
    }

    @Disabled("https://github.com/openrewrite/rewrite/issues/70")
    @Test
    fun singleLineComment(jp: JavaParser) {
        val a = jp.parse(
            """
                @Category()
                // Some comment
                public class B {

                }
            """,
            """
                @interface Category {
                }
            """
        )[0]

        assert(a.classes[0].modifiers.isNotEmpty());
    }
}
