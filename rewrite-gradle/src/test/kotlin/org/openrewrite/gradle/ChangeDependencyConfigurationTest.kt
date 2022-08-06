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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openrewrite.test.RewriteTest

class ChangeDependencyConfigurationTest : RewriteTest {
    @Test
    fun worksWithEmptyStringConfig() = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", ""))
        },
        buildGradle("""
            dependencies {
                compile 'org.openrewrite:rewrite-gradle:latest.release'
            }
        """,
        """
            dependencies {
                implementation 'org.openrewrite:rewrite-gradle:latest.release'
            }
        """)
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findDependency(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration(group, artifact, "implementation", null))
        },
        buildGradle(
            """
                dependencies {
                    compile 'org.openrewrite:rewrite-core:latest.release'
                    compile "org.openrewrite:rewrite-core:latest.release"
                }
            """,
            """
                dependencies {
                    implementation 'org.openrewrite:rewrite-core:latest.release'
                    implementation "org.openrewrite:rewrite-core:latest.release"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findMapStyleDependency(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration(group, artifact, "implementation", null))
        },
        buildGradle(
            """
                dependencies {
                    compile group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                    compile group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
                }
            """,
            """
                dependencies {
                    implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                    implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
                }
            """
        )
    )

    @Disabled
    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun withoutVersionShouldNotChange(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration(group, artifact, "implementation", null))
        },
        buildGradle(
            """
                dependencies {
                    compile 'org.openrewrite:rewrite-core'
                    compile "org.openrewrite:rewrite-core"
                    compile group: 'org.openrewrite', name: 'rewrite-core'
                    compile group: "org.openrewrite", name: "rewrite-core"
                }
            """,
            """
                dependencies {
                    implementation 'org.openrewrite:rewrite-core'
                    implementation "org.openrewrite:rewrite-core"
                    implementation group: 'org.openrewrite', name: 'rewrite-core'
                    implementation group: "org.openrewrite", name: "rewrite-core"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun worksWithClassifier(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration(group, artifact, "implementation", null))
        },
        buildGradle(
            """
                dependencies {
                    compile 'org.openrewrite:rewrite-core:latest.release:classifier'
                    compile "org.openrewrite:rewrite-core:latest.release:classifier"
                    compile group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier'
                    compile group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier"
                }
            """,
            """
                dependencies {
                    implementation 'org.openrewrite:rewrite-core:latest.release:classifier'
                    implementation "org.openrewrite:rewrite-core:latest.release:classifier"
                    implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier'
                    implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier"
                }
            """
        )
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun worksWithExt(group: String, artifact: String) = rewriteRun(
        { spec ->
            spec.recipe(ChangeDependencyConfiguration(group, artifact, "implementation", null))
        },
        buildGradle(
            """
                dependencies {
                    compile 'org.openrewrite:rewrite-core@ext'
                    compile "org.openrewrite:rewrite-core@ext"
                    compile 'org.openrewrite:rewrite-core:latest.release@ext'
                    compile "org.openrewrite:rewrite-core:latest.release@ext"
                    compile 'org.openrewrite:rewrite-core:latest.release:classifier@ext'
                    compile "org.openrewrite:rewrite-core:latest.release:classifier@ext"
                    compile group: 'org.openrewrite', name: 'rewrite-core', extension: 'ext'
                    compile group: "org.openrewrite", name: "rewrite-core", extension: "ext"
                    compile group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', extension: 'ext'
                    compile group: "org.openrewrite", name: "rewrite-core", version: "latest.release", extension: "ext"
                    compile group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier', extension: 'ext'
                    compile group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier", extension: "ext"
                }
            """,
            """
                dependencies {
                    implementation 'org.openrewrite:rewrite-core@ext'
                    implementation "org.openrewrite:rewrite-core@ext"
                    implementation 'org.openrewrite:rewrite-core:latest.release@ext'
                    implementation "org.openrewrite:rewrite-core:latest.release@ext"
                    implementation 'org.openrewrite:rewrite-core:latest.release:classifier@ext'
                    implementation "org.openrewrite:rewrite-core:latest.release:classifier@ext"
                    implementation group: 'org.openrewrite', name: 'rewrite-core', extension: 'ext'
                    implementation group: "org.openrewrite", name: "rewrite-core", extension: "ext"
                    implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', extension: 'ext'
                    implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release", extension: "ext"
                    implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier', extension: 'ext'
                    implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier", extension: "ext"
                }
            """
        )
    )
}
