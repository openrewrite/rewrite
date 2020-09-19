package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import java.nio.file.Paths

class JavaEcjParserTest {
    @Test
    fun projectPath() {
        val inputs = listOf(
                Parser.Input(Paths.get("p/src/main/java/org/openrewrite/A.java")) { "".byteInputStream() },
                Parser.Input(Paths.get("p/src/main/java/io/moderne/B.java")) { "".byteInputStream() },
        )

        val projectPath = JavaEcjParser.projectPath(inputs)

        assertThat(projectPath.toPortableString()).isEqualTo("p/src/main/java")
    }
}
