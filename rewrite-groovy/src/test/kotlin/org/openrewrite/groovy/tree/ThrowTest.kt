package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test

class ThrowTest : GroovyTreeTest {

    @Test
    fun throwException() = assertParsePrintAndProcess(
        """
            def test() {
                throw new UnsupportedOperationException()
            }
        """
    )
}
