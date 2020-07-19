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
import org.junit.jupiter.api.Test
import org.openrewrite.Profile
import org.openrewrite.text.ChangeText
import org.openrewrite.text.TextStyle

internal class ProfileConfigurationTest {
    private val changeText = ChangeText()

    @Test
    fun includeWildcard() {
        val profile = ProfileConfiguration().apply {
            setInclude(setOf("org.openrewrite.text.*"))
        }.build(emptyList())

        assertThat(profile.accept(ChangeText().apply {
            toText = "hi"
        })).isEqualTo(Profile.FilterReply.ACCEPT)
    }

    @Test
    fun includeWildcardsDontIncludeNestedPackages() {
        val profile = ProfileConfiguration().apply {
            setInclude(setOf("org.openrewrite.*"))
        }.build(emptyList())

        assertThat(profile.accept(ChangeText().apply {
            toText = "hi"
        })).isEqualTo(Profile.FilterReply.NEUTRAL)
    }

    @Test
    fun configureSourceVisitor() {
        val profile = ProfileConfiguration().apply {
            setConfigure(
                    mapOf("org.openrewrite.text.ChangeText" to
                            mapOf("toText" to "Hello Jon!")
                    )
            )
        }.build(emptyList())

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun configureStyles() {
        val profile = ProfileConfiguration().apply {
            setStyles(
                    mapOf("org.openrewrite.text.TextStyle" to
                            mapOf("charset" to "UTF-8")
                    )
            )
        }

        assertThat(profile.build(emptyList()).styles.first()).isInstanceOf(TextStyle::class.java)
        assertThat((profile.build(emptyList()).styles.first() as TextStyle).charset).isEqualTo("UTF-8")
    }

    @Test
    fun everyProfileImplicitlyExtendsDefault() {
        val default = ProfileConfiguration().apply {
            name = "default"
            setConfigure(
                    mapOf("org.openrewrite.text.ChangeText" to
                            mapOf("toText" to "Hello Jon!")
                    )
            )
        }

        val profile = ProfileConfiguration().apply {
            name = "hello-jon"
        }.build(listOf(default))

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun propertyNameCombinedWithVisitorName() {
        val profile = ProfileConfiguration().apply {
            setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "Hello Jon!"))
        }.build(emptyList())

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun propertyNameCombinedWithWildcardVisitor() {
        val profile = ProfileConfiguration().apply {
            setConfigure(mapOf("org.openrewrite.text.*.toText" to "Hello Jon!"))
        }.build(emptyList())

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackageWildcard() {
        val profile = ProfileConfiguration().apply {
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.*" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }.build(emptyList())

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackage() {
        val profile = ProfileConfiguration().apply {
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.ChangeText" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }.build(emptyList())

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }
}
