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
package org.openrewrite.maven.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.TreeSerializer
import org.openrewrite.git.GitMetadata
import org.openrewrite.maven.MavenParser
import java.io.File
import java.nio.file.Path

class MavenPomSerializerTest {
    private val mavenParser = MavenParser.builder().resolveDependencies(false).build()
    private val pom = """
        <project>
          <modelVersion>4.0.0</modelVersion>
          
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
        </project>
    """.trimIndent()

    @Test
    fun roundTripSerialization(@TempDir tempDir: Path) {

        val serializer = TreeSerializer<Maven.Pom>()
        val a = mavenParser.parse(File(tempDir.toFile(), "pom.xml").apply {
            writeText(pom)
        }.toPath(), tempDir).withMetadata(listOf(GitMetadata().apply { headCommitId = "123" }))

        val aBytes = serializer.write(a)
        val aDeser = serializer.read(aBytes)

        assertEquals(a, aDeser)
    }

    @Test
    fun roundTripSerializationList(@TempDir tempDir: Path) {
        val serializer = TreeSerializer<Maven.Pom>()

        val m1 = mavenParser.parse(File(tempDir.toFile(), "pom.xml").apply {
            writeText(pom)
        }.toPath(), tempDir).withMetadata(listOf(GitMetadata().apply { headCommitId = "123" }))

        val m2 = mavenParser.parse(File(tempDir.toFile(), "pom.xml").apply {
            writeText(pom)
        }.toPath(), tempDir).withMetadata(listOf(GitMetadata().apply { headCommitId = "123" }))

        val serialized = serializer.write(listOf(m1, m2))
        val deserialized = serializer.readList(serialized)

        assertEquals(m1, deserialized[0])
    }
}
