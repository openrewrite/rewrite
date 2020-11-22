package org.openrewrite.maven.internal

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mapdb.DBMaker
import org.mapdb.serializer.SerializerString
import org.openrewrite.Parser
import org.openrewrite.maven.internal.RawPom.ArtifactPolicy
import java.net.URI
import java.nio.file.Path

class RawPomTest {
    @Test
    fun profileActivationByJdk() {
        assertThat(RawPom.ProfileActivation("11", emptyMap()).isActive).isTrue()
        assertThat(RawPom.ProfileActivation("[,12)", emptyMap()).isActive).isTrue()
        assertThat(RawPom.ProfileActivation("[,11]", emptyMap()).isActive).isFalse()
    }

    @Test
    fun serializeAndDeserialize(@TempDir tempDir: Path) {
        val pom = RawMaven.parse(Parser.Input(URI.create("pom.xml")) {
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
        }, null).pom

        assertSerializationRoundTrip(tempDir, pom)
    }

    @Test
    fun repositorySerializationAndDeserialization() {
        val repo = RawPom.Repository("https://repo.maven.apache.org/maven2",
                ArtifactPolicy(true), ArtifactPolicy(false))

        val mapper = ObjectMapper()

        assertThat(mapper.readValue(mapper.writeValueAsBytes(repo), RawPom.Repository::class.java)).isEqualTo(repo)
    }

    @Test
    fun repositoriesSerializationAndDeserialization(@TempDir tempDir: Path) {
        val pom = RawMaven.parse(Parser.Input(URI.create("pom.xml")) {
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
        }, null).pom

        val rawPom = assertSerializationRoundTrip(tempDir, pom)
        assertThat(rawPom.repositories).hasSize(1)
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
