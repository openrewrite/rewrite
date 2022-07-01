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
import org.openrewrite.Recipe

class DependencyUseStringNotationTest: GradleRecipeTest {
    override val recipe: Recipe
        get() = DependencyUseStringNotation()

    @Test
    fun basicMap() = assertChanged(
        before = """
            dependencies {
                api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                implementation group: 'group', name: 'artifact', version: 'version'
            }
        """,
        after = """
            dependencies {
                api("org.openrewrite:rewrite-core:latest.release")
                implementation "group:artifact:version"
            }
        """
    )

    @Test
    fun basicMapLiteral() = assertChanged(
        before = """
            dependencies {
                api([group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'])
            }
        """,
        after = """
            dependencies {
                api("org.openrewrite:rewrite-core:latest.release")
            }
        """
    )

    @Test
    fun withGString() = assertChanged(
        before = """
            def version = "latest.release"
            dependencies {
                api(group: 'org.openrewrite', name: 'rewrite-core', version: version)
                implementation group: 'group', name: 'artifact', version: version
            }
        """,
        after = """
            def version = "latest.release"
            dependencies {
                api("org.openrewrite:rewrite-core:${"$"}version")
                implementation "group:artifact:${"$"}version"
            }
        """
    )

    @Test
    fun withoutVersion() = assertChanged(
        before = """
            dependencies {
                api(group: "org.openrewrite", name: "rewrite-core")
                implementation group: "group", name: "artifact"
            }
        """,
        after = """
            dependencies {
                api("org.openrewrite:rewrite-core")
                implementation "group:artifact"
            }
        """
    )

    @Test
    fun withExclusion() = assertChanged(
        before = """
            dependencies {
                api(group: "org.openrewrite", name: "rewrite-core", version: "latest.release") {
                    exclude group: "group", module: "artifact"
                }
            }
        """,
        after = """
            dependencies {
                api("org.openrewrite:rewrite-core:latest.release") {
                    exclude group: "group", module: "artifact"
                }
            }
        """
    )

    @Test
    fun withGStringExclusion() = assertChanged(
        before = """
            def version = "latest.release"
            dependencies {
                api(group: "org.openrewrite", name: "rewrite-core", version: version) {
                    exclude group: "group", module: "artifact"
                }
            }
        """,
        after = """
            def version = "latest.release"
            dependencies {
                api("org.openrewrite:rewrite-core:${"$"}version") {
                    exclude group: "group", module: "artifact"
                }
            }
        """
    )
}
