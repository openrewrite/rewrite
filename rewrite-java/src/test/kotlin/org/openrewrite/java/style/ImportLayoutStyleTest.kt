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

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ImportLayoutStyleTest {
    companion object {
        private val mapper = ObjectMapper()
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
    }
}
