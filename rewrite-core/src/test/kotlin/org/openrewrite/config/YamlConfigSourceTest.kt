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
package org.openrewrite.config

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.config.Config
import org.junit.jupiter.api.Test

class YamlConfigSourceTest {
    private val config: Config = YamlConfigSource.yamlConfig("""
            checkstyle:
                FallThrough:
                    checkLastCaseGroup: true
            checkstyle.LeftCurly:
                tokens:
                    - lambda
                    - class_def
        """.trimIndent())

    @Test
    fun nestedProperties() {
        assertThat(config.getValue("checkstyle.FallThrough.checkLastCaseGroup", Boolean::class.java)).isTrue()
        assertThat(config.getValue("checkstyle.LeftCurly.tokens", List::class.java))
                .containsExactlyInAnyOrder("lambda", "class_def")
    }

    @Test
    fun caseInsensitive() {
        assertThat(config.getValue("checkstyle.fallThrough.checkLastCaseGroup", Boolean::class.java)).isTrue()
    }

    @Test
    fun kebabCase() {
        assertThat(config.getValue("checkstyle.fall-through.check-last-case-group", Boolean::class.java)).isTrue()
    }
}
