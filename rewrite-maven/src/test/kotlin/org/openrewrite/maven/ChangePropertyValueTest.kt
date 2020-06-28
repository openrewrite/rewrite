package org.openrewrite.maven

import assertRefactored
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ChangePropertyValueTest {
    @Test
    fun property(@TempDir tempDir: Path) {
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
                </project>
            """.trimIndent().trim())
        }

        val pom = MavenParser.builder()
                .resolveDependencies(false)
                .build()
                .parse(pomFile.toPath(), tempDir)

        val fixed = pom.refactor()
                .visit(ChangePropertyValue().apply {
                    setKey("guava.version")
                    setToValue("29.0-jre")
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
            </project>
        """.trimIndent())
    }
}
