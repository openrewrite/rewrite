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
package org.openrewrite.json

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.internal.StringUtils

class JsonParserTest {
    private val parser: JsonParser = JsonParser()

    private fun assertUnchanged(before: String) {
        val jsonDocument = parser.parse(InMemoryExecutionContext { t -> t.printStackTrace() },
            StringUtils.trimIndent(before)).iterator().next()
        assertThat(jsonDocument.printAll()).`as`("Source should not be changed").isEqualTo(before)
    }

    @Test
    fun parseJsonDocument() = assertUnchanged(
            before = """
                {
                  // comments
                  unquoted: 'and you can quote me on that',
                  singleQuotes: 'I can use "double quotes" here',
                  hexadecimal: 0xdecaf,
                  leadingDecimalPoint: .8675309, andTrailing: 8675309.,
                  positiveSign: +1,
                  trailingComma: 'in objects', andIn: ['arrays',],
                  "backwardsCompatible": "with JSON",
                }
            """.trimIndent()
    )

    @Test
    fun stringLiteral() = assertUnchanged(
        before = "'hello world'"
    )

    @Test
    fun booleanLiteral() = assertUnchanged(
        before = "true"
    )

    @Test
    fun doubleLiteralExpSigned() = assertUnchanged(
        before = "-1.e3"
    )

    @Test
    fun array() = assertUnchanged(
        before = "[ 1 , 2 , 3 , ]"
    )

    @Test
    fun obj() = assertUnchanged(
        before = """
            {
                key: "value",
                "key": 1,
            }
        """.trimIndent()
    )

    @Test
    fun comments() = assertUnchanged(
        before = """
            // test
            {
                /* test */
                key: "value",
                // test
                "key": 1,
            }
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1145")
    @Test
    fun long() = assertUnchanged(
        before = """
            {
                "timestamp": 1577000812973
            }
        """.trimIndent()
    )
}
