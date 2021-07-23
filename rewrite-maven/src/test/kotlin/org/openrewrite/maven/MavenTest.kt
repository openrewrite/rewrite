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
package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.maven.tree.MavenModel
import org.openrewrite.maven.tree.Pom

class MavenTest : MavenRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/390")
    @Test
    fun withModelResultsInOneMarker() {
        val pom = """
            <project>
                <groupId>com.foo</groupId>
                <artifactId>parent</artifactId>
                <version>1</version>
            </project>
        """.trimIndent()

        val maven = MavenParser.builder()
            .build()
            .parse(pom)[0]

        val maven2 = maven.withModel(maven.model.withLicenses(listOf(Pom.License("apache", Pom.LicenseType.Apache2))))

        assertThat(maven2.markers.findAll(MavenModel::class.java)).hasSize(1)
        assertThat(maven2.markers.findAll(MavenModel::class.java).first().pom.licenses).hasSize(1)
    }
}
