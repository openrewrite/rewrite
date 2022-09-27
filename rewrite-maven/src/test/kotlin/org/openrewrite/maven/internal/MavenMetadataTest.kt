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
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openrewrite.maven.tree.MavenMetadata

class MavenMetadataTest {
    @Test
    fun deserializeMetadata() {
        @Language("xml") val metadata = """
            <metadata>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot</artifactId>
                <versioning>
                    <latest>2.4.2</latest>
                    <release>2.4.2</release>
                    <versions>
                        <version>2.4.1</version>
                        <version>2.4.2</version>
                    </versions>
                    <lastUpdated>20210115042754</lastUpdated>
                </versioning>
            </metadata>
        """.trimIndent()

        val parsed = MavenMetadata.parse(metadata.toByteArray())

        assertThat(parsed.versioning.versions).hasSize(2)
    }

    @Test
    fun deserializeSnapshotMetadata() {
        @Language("xml") val metadata = """
            <metadata modelVersion="1.1.0">
                <groupId>org.openrewrite.recipe</groupId>
                <artifactId>rewrite-recommendations</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <versioning>
                    <snapshot>
                        <timestamp>20220927.033510</timestamp>
                        <buildNumber>223</buildNumber>
                    </snapshot>
                    <snapshotVersions>
                        <snapshotVersion>
                            <extension>pom.asc</extension>
                            <value>0.1.0-20220927.033510-223</value>
                            <updated>20220927033510</updated>
                        </snapshotVersion>
                        <snapshotVersion>
                            <extension>pom</extension>
                            <value>0.1.0-20220927.033510-223</value>
                            <updated>20220927033510</updated>
                        </snapshotVersion>
                    </snapshotVersions>
                </versioning>
            </metadata>
        """.trimIndent()

        val parsed = MavenMetadata.parse(metadata.toByteArray())

        assertThat(parsed.versioning.snapshot?.timestamp).isEqualTo("20220927.033510")
        assertThat(parsed.versioning.snapshot?.buildNumber).isEqualTo("223")
        assertThat(parsed.versioning.versions).isNotNull
        assertThat(parsed.versioning.snapshotVersions).hasSize(2)
        assertThat(parsed.versioning.snapshotVersions!![0].extension).isNotNull()
        assertThat(parsed.versioning.snapshotVersions!![0].value).isNotNull()
        assertThat(parsed.versioning.snapshotVersions!![0].updated).isNotNull()
    }

}
