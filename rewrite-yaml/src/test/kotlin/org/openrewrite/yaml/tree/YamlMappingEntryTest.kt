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
package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.yaml.YamlParser

class YamlMappingEntryTest {

    @Issue("https://github.com/spring-projects/spring-boot/issues/8438")
    @Test
    fun valueStartsWithAt() {
        val y = """
          date: @build.timestamp@
          version: @project.version@
        """.trimIndent()

        val parsed = YamlParser().parse(InMemoryExecutionContext { t -> t.printStackTrace() }, y)[0]
        assertThat(parsed.print()).isEqualTo(y)
    }

    @Test
    fun suffixBeforeColon() {
        val y = """
          data :
            test : 0
        """.trimIndent()

        val parsed = YamlParser().parse(InMemoryExecutionContext { t -> t.printStackTrace() }, y)[0]
        assertThat(parsed.print()).isEqualTo(y)
    }

    @Test
    fun literals() {
        val y = """
          data:
            prometheus.yml: |-
              global:
                scrape_interval: 10s
                scrape_timeout: 9s
                evaluation_interval: 10s
        """.trimIndent()

        val parsed = YamlParser().parse(InMemoryExecutionContext { t -> t.printStackTrace() }, y)[0]
        assertThat(parsed.print()).isEqualTo(y)
    }
}
