/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.security

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.gradle.GradleRecipeTest

@Suppress("HttpUrlsUsage")
class UseHttpsForRepositoriesTest : GradleRecipeTest {
    override val recipe: Recipe
        get() = UseHttpsForRepositories()

    @Test
    fun unchangedUseOfHttps() = assertUnchanged(
        before = """
            repositories {
                maven { url 'https://repo.spring.example.com/libs-release-local' }
            }
        """
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsSingleQuote() = assertChanged(
        before = """
            repositories {
                maven { url 'http://repo.spring.example.com/libs-release-local' }
            }
        """,
        after = """
            repositories {
                maven { url 'https://repo.spring.example.com/libs-release-local' }
            }
        """
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsDoubleQuote() = assertChanged(
        before = """
            repositories {
                maven { url "http://repo.spring.example.com/libs-release-local" }
            }
        """,
        after = """
            repositories {
                maven { url "https://repo.spring.example.com/libs-release-local" }
            }
        """
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsGString() = assertChanged(
        before = """
            repositories {
                maven {
                    def subRepo = properties.snapshot ? 'snapshot' : 'release'
                    url "http://repo.spring.example.com/libs-release-local/${'$'}subRepo"
                }
            }
        """,
        after = """
            repositories {
                maven {
                    def subRepo = properties.snapshot ? 'snapshot' : 'release'
                    url "https://repo.spring.example.com/libs-release-local/${'$'}subRepo"
                }
            }
        """
    )
}
