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
import org.openrewrite.gradle.Assertions.buildGradle
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("HttpUrlsUsage")
class UseHttpsForRepositoriesTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseHttpsForRepositories())
    }

    @Test
    fun unchangedUseOfHttps() = rewriteRun(
        buildGradle(
            """
                repositories {
                    maven { url 'https://repo.spring.example.com/libs-release-local' }
                }
            """
        )
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsSingleQuote() = rewriteRun(
        buildGradle(
            """
                repositories {
                    maven { url 'http://repo.spring.example.com/libs-release-local' }
                }
            """,
            """
                repositories {
                    maven { url 'https://repo.spring.example.com/libs-release-local' }
                }
            """
        )
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsDoubleQuote() = rewriteRun(
        buildGradle(
            """
                repositories {
                    maven { url "http://repo.spring.example.com/libs-release-local" }
                }
            """,
            """
                repositories {
                    maven { url "https://repo.spring.example.com/libs-release-local" }
                }
            """
        )
    )

    @Test
    fun updateUnwrappedInvocationToUseHttpsGString() = rewriteRun(
        buildGradle(
            """
                repositories {
                    maven {
                        def subRepo = properties.snapshot ? 'snapshot' : 'release'
                        url "http://repo.spring.example.com/libs-release-local/${'$'}subRepo"
                    }
                }
            """,
            """
                repositories {
                    maven {
                        def subRepo = properties.snapshot ? 'snapshot' : 'release'
                        url "https://repo.spring.example.com/libs-release-local/${'$'}subRepo"
                    }
                }
            """
        )
    )
}
