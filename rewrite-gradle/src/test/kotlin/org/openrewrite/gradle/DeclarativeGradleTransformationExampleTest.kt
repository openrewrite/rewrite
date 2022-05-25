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
package org.openrewrite.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment

class DeclarativeGradleTransformationExampleTest : GradleRecipeTest {

    override val recipe: Recipe
        get() = DeclarativeGradleTransformationExample(
        "old.groupid",
        "rewrite-gradle",
        "new.groupid",
        "1.0"
    )

    @Test
    fun doesNotChangeOtherMethods() = assertUnchanged(
        before = """
            dependencies {
                compile project('projectName:packageName:util')
                compile(other('old.groupid', 'rewrite-gradle', 'runtime')) // does not match bundle.
                compile(bundle('old.groupid', 'is-not-rewrite-gradle', 'runtime')) // does not match artifact.
                compile(bundle('java', 'rewrite-gradle', 'runtime')) // does not match group.
                compile group: 'new.groupid', name: 'rewrite-gradle', version: '1.0' // is already transformed.
            }
        """
    )

    @Test
    fun notInDependencyBlock() = assertUnchanged(
        before = """
            other {
                compile(group: 'old.groupid', name: 'rewrite-gradle', version: '2.2.1.0ms', configuration: 'runtime')
            }
        """
    )

    @Test
    fun changeByBundleMethod() = assertChanged(
        before = """
            dependencies {
                compile(bundle('old.groupid', 'rewrite-gradle', 'runtime'))
            }
        """,
        after = """
            dependencies {
                compile group: 'new.groupid', name: 'rewrite-gradle', version: '1.0'
            }
        """
    )

    @Test
    fun changeByGroupAndName() = assertChanged(
        before = """
            dependencies {
                compile(group: 'old.groupid', name: 'rewrite-gradle', version: '2.2.1.0ms', configuration: 'runtime')
            }
        """,
        after = """
            dependencies {
                compile group: 'new.groupid', name: 'rewrite-gradle', version: '1.0'
            }
        """
    )

    @Test
    fun changeTestMethod() = assertChanged(
        before = """
            dependencies {
                test(bundle('old.groupid', 'rewrite-gradle', 'runtime'))
            }
        """,
        after = """
            dependencies {
                test group: 'new.groupid', name: 'rewrite-gradle', version: '1.0'
            }
        """
    )

    @Test
    fun declarativeExample() = assertChanged(
        recipe = Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.gradle")
            .build()
            .activateRecipes("org.openrewrite.gradle.OssJavaMigration"),
        before = """
            dependencies {
                compile(bundle('ossjava', 'commons-compress', 'runtime'))
                compile(group: 'ossjava', name: 'quartz', version: '2.2.1.1ms', configuration: 'runtime')
                compile(bundle('ossjava', 'urlrewritefilter', 'runtime'))
            }
        """,
        after = """
            dependencies {
                compile group: 'org.apache.commons', name: 'commons-compress', version: '1.19'
                compile group: 'org.quartz-scheduler', name: 'quartz', version: '2.2.1'
                compile group: 'org.turkey', name: 'urlrewritefilter', version: '3.2.0'
            }
        """
    )
}
