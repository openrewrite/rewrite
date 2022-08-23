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
package org.openrewrite.java.format

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.style.GeneralFormatStyle
import org.openrewrite.style.NamedStyles

interface EmptyNewlineAtEndOfFileTest {

    fun generalFormat(useCRLF: Boolean) = listOf(
        NamedStyles(
            Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                GeneralFormatStyle(useCRLF))
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1045")
    @Test
    fun usesCRLF(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.styles(generalFormat(true)).build()
                .parse("class Test {}")).results[0].after!!.printAll()).isEqualTo(
            "class Test {}\r\n"
        )
    }

    @Test
    fun autodetectCRLF(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.build().parse("class Test {\r\n}")).results[0].after!!.printAll()).isEqualTo(
            "class Test {\r\n}\r\n"
        )
    }

    @Test
    fun autodetectLF(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.build().parse("class Test {\n}")).results[0].after!!.printAll()).isEqualTo(
            "class Test {\n}\n"
        )
    }

    @Test
    fun noComments(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.styles(generalFormat(false)).build()
                .parse("class Test {}")).results[0].after!!.printAll()).isEqualTo(
            """
                class Test {}
                
            """.trimIndent()
        )
    }

    @Test
    fun comments(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.styles(generalFormat(false)).build()
                .parse("class Test {}\n/*comment*/")).results[0].after!!.printAll()).isEqualTo(
            """
                class Test {}
                /*comment*/
                
            """.trimIndent()
        )
    }

    @Test
    fun multipleLinesToOne(jp: JavaParser.Builder<*, *>) {
        assertThat(EmptyNewlineAtEndOfFile().run(
            jp.styles(generalFormat(false)).build()
                .parse("class Test {}\n\n")).results[0].after!!.printAll()).isEqualTo(
            """
                class Test {}
                
            """.trimIndent()
        )
    }
}
