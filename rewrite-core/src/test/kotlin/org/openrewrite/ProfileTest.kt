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
package org.openrewrite

import io.micrometer.core.instrument.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.text.ChangeText

internal class ProfileTest {
    private val changeText = ChangeText()

    @Test
    fun configureSourceVisitor() {
        val profile: Profile = Profile().apply {
            setConfigure(
                    mapOf("org.openrewrite.text.ChangeText" to
                            mapOf("toText" to "Hello Jon!")
                    )
            )
        }

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun defineDeclarativeVisitor() {
        val profile = Profile().apply {
            setDefine(
                    mapOf("org.openrewrite.text.ChangeTextTwice" to
                            listOf(
                                    mapOf("org.openrewrite.text.ChangeText" to
                                            mapOf(
                                                    "toText" to "Hello Jon"
                                            )
                                    ),
                                    mapOf("text.ChangeText" to
                                            mapOf(
                                                    "toText" to "Hello Jonathan!"
                                            )
                                    )
                            )
                    )
            )
        }

        assertThat(profile.definitions).hasSize(1)
        assertThat(profile.definitions.iterator().next().tags)
                .contains(Tag.of("name", "org.openrewrite.text.ChangeTextTwice"))
    }

    @Test
    fun propertyNameCombinedWithVisitorName() {
        val profile: Profile = Profile().apply {
            setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "Hello Jon!"))
        }

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun propertyNameCombinedWithWildcardVisitor() {
        val profile: Profile = Profile().apply {
            setConfigure(mapOf("org.openrewrite.text.*.toText" to "Hello Jon!"))
        }

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackageWildcard() {
        val profile: Profile = Profile().apply {
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.*" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackage() {
        val profile: Profile = Profile().apply {
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.ChangeText" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }

        assertThat(profile.configure(changeText).toText).isEqualTo("Hello Jon!")
    }
}
