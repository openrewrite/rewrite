/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.gradle

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openrewrite.test.RewriteTest

class ChangeDependencyExtensionTest : RewriteTest {
    @Test
    fun worksWithEmptyStringConfig() = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyExtension("org.openrewrite", "*", "jar", ""))
        },
        buildGradle("""
            dependencies {
                api 'org.openrewrite:rewrite-gradle:latest.integration@war'
            }
        """,
        """
            dependencies {
                api 'org.openrewrite:rewrite-gradle:latest.integration@jar'
            }
        """)
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findDependency(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyExtension(group, artifact, "jar", null))
        },
        buildGradle(
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core:latest.release@war'
                    api "org.openrewrite:rewrite-core:latest.release@war"
                }
            """,
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core:latest.release@jar'
                    api "org.openrewrite:rewrite-core:latest.release@jar"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findMapStyleDependency(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyExtension(group, artifact, "jar", null))
        },
        buildGradle(
            """
                dependencies {
                    api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', ext: 'war'
                    api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", ext: "war"
                }
            """,
            """
                dependencies {
                    api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', ext: 'jar'
                    api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", ext: "jar"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun worksWithoutVersion(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyExtension(group, artifact, "jar", null))
        },
        buildGradle(
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core@war'
                    api "org.openrewrite:rewrite-core@war"
                    api group: 'org.openrewrite', name: 'rewrite-core', ext: 'war'
                    api group: "org.openrewrite", name: "rewrite-core", ext: "war"
                }
            """,
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core@jar'
                    api "org.openrewrite:rewrite-core@jar"
                    api group: 'org.openrewrite', name: 'rewrite-core', ext: 'jar'
                    api group: "org.openrewrite", name: "rewrite-core", ext: "jar"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun worksWithClassifier(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyExtension(group, artifact, "jar", null))
        },
        buildGradle(
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core:latest.release:classifier@war'
                    api "org.openrewrite:rewrite-core:latest.release:classifier@war"
                    api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier', ext: 'war'
                    api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier", ext: "war"
                }
            """,
            """
                dependencies {
                    api 'org.openrewrite:rewrite-core:latest.release:classifier@jar'
                    api "org.openrewrite:rewrite-core:latest.release:classifier@jar"
                    api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier', ext: 'jar'
                    api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier", ext: "jar"
                }
            """
        )
    )
}
