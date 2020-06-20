package org.openrewrite.maven

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

        val pom = MavenParser.builder().build()
                .parse(pomFile.toPath(), tempDir)

        assertThat(pom.groupId).isEqualTo("com.mycompany.app")
        assertThat(pom.artifactId).isEqualTo("my-app")
        assertThat(pom.version).isEqualTo("1")
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#project-inheritance
     */
    @Test
    fun projectInheritance(@TempDir tempDir: Path) {
        val parentPomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim())
        }

        val myModuleProject = File(tempDir.toFile(), "my-module")
        myModuleProject.mkdirs()

        val pomFile = File(myModuleProject, "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>
                
                  <artifactId>my-module</artifactId>
                
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.6.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent())
        }

        val pom = MavenParser.builder().build()
                .parse(listOf(pomFile.toPath(), parentPomFile.toPath()), tempDir)[0]

        assertThat(pom.groupId).isEqualTo("com.mycompany.app")
        assertThat(pom.artifactId).isEqualTo("my-module")
        assertThat(pom.version).isEqualTo("1")
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Project_Aggregation
     */
    @Test
    fun projectAggregation(@TempDir tempDir: Path) {

    }
}
