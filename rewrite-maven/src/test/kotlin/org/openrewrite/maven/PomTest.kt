package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PomTest {
    @Test
    fun dependencies(@TempDir tempDir: Path) {
        val pomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(pomFile.toPath(), tempDir)

        assertThat(pom.withDependencies(pom.dependencies.map { it.withVersion("29.0-jre") }).printTrimmed()).isEqualTo("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }
}
