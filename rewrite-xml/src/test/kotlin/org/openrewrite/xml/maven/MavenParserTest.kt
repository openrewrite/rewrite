package org.openrewrite.xml.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MavenParserTest {
    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#minimal-pom
     */
    @Test
    fun minimal(@TempDir tempDir: Path) {
        val pomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser().parse(pomFile.toPath(), tempDir)

        assertThat(pom.groupId).isEqualTo("com.mycompany.app")
        assertThat(pom.artifactId).isEqualTo("my-app")
        assertThat(pom.version).isEqualTo("1")
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#project-inheritance
     */
    @Test
    fun projectInheritance(@TempDir tempDir: Path) {
        File(tempDir.toFile(), "pom.xml").writeText("""
            <project>
              <modelVersion>4.0.0</modelVersion>
             
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </project>
        """.trimIndent().trim())
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Project_Aggregation
     */
    @Test
    fun projectAggregation(@TempDir tempDir: Path) {

    }
}
