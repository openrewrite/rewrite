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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.Tree.randomId
import org.openrewrite.config.DeclarativeNamedStyles
import org.openrewrite.style.Style

class ImportLayoutStyleTest {
    companion object {
        private val mapper = ObjectMapper()
                .registerModule(ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @Test
    fun deserializeStyle() {
        val styleConfig = mapOf(
                "@c" to ImportLayoutStyle::class.qualifiedName,
                "@ref" to 1,
                "classCountToUseStarImport" to 999,
                "nameCountToUseStarImport" to 998,
                "layout" to listOf(
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
        )

        val style = mapper.convertValue(styleConfig, ImportLayoutStyle::class.java)
        assertThat(style.classCountToUseStarImport).isEqualTo(999)
        assertThat(style.nameCountToUseStarImport).isEqualTo(998)

        // round trip
        mapper.readValue(mapper.writeValueAsBytes(style), Style::class.java)
    }

    @Test
    fun deserializeInDeclarativeNamedStyles() {

        val style = DeclarativeNamedStyles(
                randomId(),
                "name",
                "displayName",
                "description",
                setOf("tag1", "tag2"),
                listOf<Style>(ImportLayoutStyle.builder()
                        .classCountToUseStarImport(5)
                        .nameCountToUseStarImport(5)
                        .importPackage("java.*")
                        .blankLine()
                        .importPackage("javax.*")
                        .blankLine()
                        .importAllOthers()
                        .blankLine()
                        .importPackage("org.springframework.*")
                        .blankLine()
                        .importStaticAllOthers()
                        .build()
                )
        )
        mapper.readValue(mapper.writeValueAsBytes(style),
                DeclarativeNamedStyles::class.java)
    }
}
