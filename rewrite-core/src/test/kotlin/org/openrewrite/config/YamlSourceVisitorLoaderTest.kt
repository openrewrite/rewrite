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

import io.micrometer.core.instrument.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YamlSourceVisitorLoaderTest {
    @Test
    fun loadFromYaml() {
        val yaml = """
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jonathan!
        """.trimIndent()

        val loader = YamlSourceVisitorLoader("org.openrewrite.text.ChangeTextTwice", yaml.byteInputStream())

        val visitors = loader.load()

        assertThat(visitors).hasSize(1)
        assertThat(visitors.first().name)
                .isEqualTo("org.openrewrite.text.ChangeTextTwice")
        assertThat(visitors.first().tags)
                .contains(Tag.of("name", "org.openrewrite.text.ChangeTextTwice"))
    }
}
