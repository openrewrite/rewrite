package org.openrewrite.cobol.tree

import org.assertj.core.api.Assertions.assertThat
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.cobol.CobolParser

interface CobolTreeTest {
    fun roundTrip(
        source: String,
        withCu: (Cobol.CompilationUnit)->Unit = {}
    ) {
        val trimmed = source.trimIndent()
        val cu = CobolParser()
            .parse(InMemoryExecutionContext { throw it }, trimmed)
            .first() as Cobol.CompilationUnit

        withCu.invoke(cu)
        assertThat(cu.printAll()).isEqualTo(trimmed)
    }
}
