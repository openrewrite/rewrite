package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class JavaEcjFileParsingTest {
    @Test
    fun parseJavaSourceFromFile(@TempDir tempDir: Path) {
        val source = File(tempDir.toFile(), "A.java")
        source.writeText("public class A {}")

        JavaEcjParser.builder().build().parse(listOf(source.toPath()), tempDir)
    }
}
