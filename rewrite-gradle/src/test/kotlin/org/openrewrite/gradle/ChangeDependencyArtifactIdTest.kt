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
import org.junit.jupiter.params.provider.ValueSource

class ChangeDependencyArtifactIdTest : GradleRecipeTest {

    @Test
    fun worksWithEmptyStringConfig() = assertChanged(
        recipe = ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", ""),
        before = """
            dependencies {
                rewrite 'org.openrewrite:rewrite-gradle:latest.integration'
                implementation 'org.springframework.cloud:spring-cloud-starter-sleuth:3.0.3'
                implementation 'org.springframework.integration:spring-integration-ftp:5.5.1'
                implementation 'org.springframework.boot:spring-boot-starter:2.5.4'
                implementation 'commons-lang:commons-lang:2.6'
                testImplementation 'org.springframework.boot:spring-boot-starter-test'
            }
        """,
        after = """
            dependencies {
                rewrite 'org.openrewrite:rewrite-gradle:latest.integration'
                implementation 'org.springframework.cloud:spring-cloud-starter-sleuth:3.0.3'
                implementation 'org.springframework.integration:spring-integration-ftp:5.5.1'
                implementation 'org.springframework.boot:new-starter:2.5.4'
                implementation 'commons-lang:commons-lang:2.6'
                testImplementation 'org.springframework.boot:spring-boot-starter-test'
            }
        """
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findDependency(group: String, artifact: String) = assertChanged(
        recipe = ChangeDependencyArtifactId(group, artifact, "dewrite-core", null),
        before = """
            dependencies {
                api 'org.openrewrite:rewrite-core:latest.release'
                api "org.openrewrite:rewrite-core:latest.release"
            }
        """,
        after = """
            dependencies {
                api 'org.openrewrite:dewrite-core:latest.release'
                api "org.openrewrite:dewrite-core:latest.release"
            }
        """
    )

    @ParameterizedTest
    @CsvSource(value = ["org.openrewrite:rewrite-core", "*:*"], delimiterString = ":")
    fun findMapStyleDependency(group: String, artifact: String) = assertChanged(
        recipe = ChangeDependencyArtifactId(group, artifact, "dewrite-core", null),
        before = """
            dependencies {
                api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                api group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
            }
        """,
        after = """
            dependencies {
                api group: 'org.openrewrite', name: 'dewrite-core', version: 'latest.release'
                api group: "org.openrewrite", name: "dewrite-core", version: "latest.release"
            }
        """
    )
}
