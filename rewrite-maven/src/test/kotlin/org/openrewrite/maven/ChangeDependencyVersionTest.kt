package org.openrewrite.maven

import assertRefactored
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ChangeDependencyVersionTest {
    @Test
    fun fixedVersion(@TempDir tempDir: Path) {
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
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(pomFile.toPath(), tempDir)

        val fixed = pom.refactor()
                .visit(ChangeDependencyVersion().apply {
                    setGroupId("com.google.guava")
                    setArtifactId("guava")
                    setToVersion("29.0-jre")
                })
                .fix().fixed

        assertRefactored(fixed, """
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
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }

    @Test
    fun propertyVersion(@TempDir tempDir: Path) {
        val pomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                   
                  <properties>
                    <guava.version>28.2-jre</guava.version>
                  </properties>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${"$"}{guava.version}</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(pomFile.toPath(), tempDir)

        val fixed = pom.refactor()
                .visit(ChangeDependencyVersion().apply {
                    setGroupId("com.google.guava")
                    setArtifactId("guava")
                    setToVersion("29.0-jre")
                })
                .fix().fixed

        assertRefactored(fixed, """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <guava.version>29.0-jre</guava.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>${"$"}{guava.version}</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }

    @Test
    fun inDependencyManagementSection() {
        fail("unimplemented")
    }

    @Test
    fun inParentProperty() {
        fail("unimplemented")
    }
}
