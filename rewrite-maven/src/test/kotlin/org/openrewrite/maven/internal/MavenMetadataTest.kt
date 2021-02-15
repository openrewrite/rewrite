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
                        <version>2.4.2</version>
                    </versions>
                    <lastUpdated>20210115042754</lastUpdated>
                </versioning>
            </metadata>
        """.trimIndent()

        val parsed = MavenMetadata.parse(metadata.toByteArray())

        assertThat(parsed.versioning.versions).isNotEmpty
    }
}
