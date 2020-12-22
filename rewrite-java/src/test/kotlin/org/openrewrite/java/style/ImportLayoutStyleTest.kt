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
package org.openrewrite.java.style

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

class ImportLayoutStyleTest {

    val mapper = ObjectMapper()

    @Test
    fun serializeStyle() {

        val blocks = listOf<String>(
            "import java.*",
            "<blank line>",
            "import javax.*",
            "<blank line>",
            "import all other imports",
            "<blank line>",
            "import org.springframework.*",
            "<blank line>",
            "import static all other imports"
        )
        val style = ImportLayoutStyle.layout(999,998, *blocks.toTypedArray())

        val copy= mapper.readValue(mapper.writeValueAsString(style), ImportLayoutStyle::class.java)
        assertThat(copy.classCountToUseStarImport).isEqualTo(999)
        assertThat(copy.nameCountToUseStarImport).isEqualTo(998)

        @Suppress("UNCHECKED_CAST") val blockCopy : List<String> = copy.layout.get("blocks") as List<String>

        assertThat(blockCopy).containsExactly(*blocks.toTypedArray())

    }

    @Test
    fun serializeStyleWithSyntaxError() {
        val style = ImportLayoutStyle.layout(999,999,
            "import java.*",
            "<blank line>",
            "import javax.*",
            "<blank line>",
            "import all other imports",
            "<blank line>",
            "import org.springframework.*",
            "<blank line>",
            "import static all other imports"
        )

        val serializedStyle = mapper.writeValueAsString(style)
        val syntaxError = serializedStyle.replace("import org.springframework", "fred")

        assertThatThrownBy {
            mapper.readValue(syntaxError, ImportLayoutStyle::class.java)
        }.isInstanceOf(JsonMappingException::class.java).hasMessageStartingWith("Syntax error")
    }

}