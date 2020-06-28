package org.openrewrite.maven

import assertRefactored
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ChangeDependencyVersionTest {
    private val guavaTo29 = ChangeDependencyVersion().apply {
        setGroupId("com.google.guava")
        setArtifactId("guava")
        setToVersion("29.0-jre")
    }

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

        val fixed = pom.refactor().visit(guavaTo29).fix().fixed

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


        val fixed = pom.refactor().visit(guavaTo29).fix().fixed

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
    fun inDependencyManagementSection(@TempDir tempDir: Path) {
        val parentPomFile = File(tempDir.toFile(), "pom.xml").apply {
            writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
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
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent())
        }

        val (_, parentPom) = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(listOf(pomFile.toPath(), parentPomFile.toPath()), tempDir)

        val fixed = parentPom.refactor().visit(guavaTo29).fix().fixed

        assertRefactored(fixed, """
            <project>
              <modelVersion>4.0.0</modelVersion>
             
              <packaging>pom</packaging>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
        """.trimIndent())
    }

    @Test
    fun inParentProperty() {
        fail("unimplemented")
    }
}
