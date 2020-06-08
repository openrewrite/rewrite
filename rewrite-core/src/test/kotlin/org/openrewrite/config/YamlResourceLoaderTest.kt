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
import org.openrewrite.Profile
import org.openrewrite.text.ChangeText

class YamlResourceLoaderTest {
    @Test
    fun loadVisitorFromYaml() {
        val yaml = """
            type: beta.openrewrite.org/v1/visitor
            name: org.openrewrite.text.ChangeTextTwice
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jonathan!
        """.trimIndent()

        val loader = YamlResourceLoader(yaml.byteInputStream())

        val visitors = loader.loadVisitors()

        assertThat(visitors).hasSize(1)
        assertThat(visitors.first().name)
                .isEqualTo("org.openrewrite.text.ChangeTextTwice")
        assertThat(visitors.first().tags)
                .contains(Tag.of("name", "org.openrewrite.text.ChangeTextTwice"))
    }

    @Test
    fun loadProfileYaml() {
        val profile = YamlResourceLoader("""
            type: beta.openrewrite.org/v1/profile
            name: test
            include:
              - 'org.openrewrite.text.*'
            exclude:
              - org.openrewrite.text.DoesNotExist
            configure:
              org.openrewrite.text.ChangeText:
                toText: 'Hello Jon!'
        """.trimIndent().byteInputStream()).loadProfiles().first().build(emptyList())

        val changeText = ChangeText()

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
        assertThat(profile.accept(changeText)).isEqualTo(Profile.FilterReply.ACCEPT)
    }

    @Test
    fun loadMultiYaml() {
        val resources = YamlResourceLoader("""
            ---
            type: beta.openrewrite.org/v1/profile
            name: checkstyle
            ---
            type: beta.openrewrite.org/v1/profile
            name: spring
            ---
            type: beta.openrewrite.org/v1/visitor
            name: org.openrewrite.text.ChangeTextToJon
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon!
        """.trimIndent().byteInputStream())

        assertThat(resources.loadProfiles().map { it.name }).containsOnly("checkstyle", "spring")
        assertThat(resources.loadVisitors().map { it.name }).containsExactly("org.openrewrite.text.ChangeTextToJon")
    }
}
