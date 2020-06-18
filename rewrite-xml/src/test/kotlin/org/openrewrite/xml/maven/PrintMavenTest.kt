package org.openrewrite.xml.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PrintMavenTest {
    @Test
    fun printMaven(@TempDir tempDir: Path) {
        val pomText = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """.trimIndent().trim()

        val pomFile = File(tempDir.toFile(), "pom.xml")
                .apply { writeText(pomText) }

        val pom = MavenParser().parse(pomFile.toPath(), tempDir)

        assertThat(pom.print()).isEqualTo(pomText)
    }
}