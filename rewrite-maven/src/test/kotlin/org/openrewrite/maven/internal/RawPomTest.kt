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

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mapdb.DBMaker
import org.mapdb.serializer.SerializerString
import org.openrewrite.Parser
import org.openrewrite.maven.internal.RawRepositories.ArtifactPolicy
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

class RawPomTest {
    @Test
    fun profileActivationByJdk() {
        assertThat(RawPom.ProfileActivation("11", emptyMap()).isActive).isTrue()
        assertThat(RawPom.ProfileActivation("[,12)", emptyMap()).isActive).isTrue()
        assertThat(RawPom.ProfileActivation("[,11]", emptyMap()).isActive).isFalse()
    }

    @Test
    fun serializeAndDeserialize(@TempDir tempDir: Path) {
        val pom = RawMaven.parse(Parser.Input(Paths.get("pom.xml")) {
            """
                <project>
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <developers>
                    <developer>
                        <name>Trygve Laugst&oslash;l</name>
                    </developer>
                </developers>
                
                <licenses>
                    <license>
                        <name>Apache License, Version 2.0</name>
                    </license>
                </licenses>

                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                  </dependency>
                </dependencies>
            </project>
            """.trimIndent().byteInputStream()
        }, null, null).pom

        assertSerializationRoundTrip(tempDir, pom)
    }

    @Test
    fun repositorySerializationAndDeserialization() {
        val repo = RawRepositories.Repository("central","https://repo.maven.apache.org/maven2",
                ArtifactPolicy(true), ArtifactPolicy(false))

        val mapper = ObjectMapper()

        assertThat(mapper.readValue(mapper.writeValueAsBytes(repo), RawRepositories.Repository::class.java)).isEqualTo(repo)
    }

    @Test
    fun repositoriesSerializationAndDeserialization(@TempDir tempDir: Path) {
        val pom = RawMaven.parse(Parser.Input(Paths.get("pom.xml")) {
            """
                <project>
                  `<modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                </project>
            """.trimIndent().byteInputStream()
        }, null, null).pom

        val rawPom = assertSerializationRoundTrip(tempDir, pom)
        assertThat(rawPom.getActiveRepositories(emptyList())).hasSize(1)
    }

    private fun assertSerializationRoundTrip(tempDir: Path, pom: RawPom): RawPom =
            DBMaker
                    .fileDB(tempDir.resolve("cache.db").toFile())
                    .make()
                    .hashMap("pom", SerializerString(), JacksonMapdbSerializer(RawPom::class.java))
                    .create()
                    .use { pomCache ->
                        pomCache["test"] = pom
                        assertThat(pomCache["test"]).isEqualTo(pom)
                        pom
                    }
}
