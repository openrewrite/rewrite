package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.YamlParser

interface YamlParserTest {
    val parser: YamlParser
        get() = YamlParser()

    val onError: (Throwable) -> Unit
        get() = { t -> t.printStackTrace() }

    /**
     * Assert that the provided yaml source is parsed and printed back out unchanged.
     * Supply a documentsAssert to inspect the parsed AST
     */
    fun assertRoundTrip(
        @Language("yml") source: String,
        afterConditions: (Yaml.Documents)->Unit = { }
    ) {
        val trimmedSource = source.trimIndent()
        val ast = parser.parse(InMemoryExecutionContext(onError), trimmedSource).first()
        afterConditions(ast)
        val after = ast.print()
        Assertions.assertThat(after).isEqualTo(trimmedSource)
    }
}
