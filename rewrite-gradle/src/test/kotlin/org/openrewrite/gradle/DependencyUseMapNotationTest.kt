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

class DependencyUseMapNotationTest: GradleRecipeTest {
    override val recipe: Recipe
        get() = DependencyUseMapNotation()

    @Test
    fun basicString() = assertChanged(
        before = """
            dependencies {
                api('org.openrewrite:rewrite-core:latest.release')
                implementation "group:artifact:version"
            }
        """,
        after = """
            dependencies {
                api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                implementation group: 'group', name: 'artifact', version: 'version'
            }
        """
    )

    @Test
    fun withGString() = assertChanged(
        before = """
            def version = "latest.release"
            dependencies {
                api("org.openrewrite:rewrite-core:${"$"}version")
                implementation "group:artifact:${"$"}version"
            }
        """,
        after = """
            def version = "latest.release"
            dependencies {
                api(group: 'org.openrewrite', name: 'rewrite-core', version: version)
                implementation group: 'group', name: 'artifact', version: version
            }
        """
    )
}
