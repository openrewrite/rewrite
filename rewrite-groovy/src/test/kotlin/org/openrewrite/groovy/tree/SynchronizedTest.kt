package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test

class SynchronizedTest : GroovyTreeTest {

    @Test
    fun synchronized() = assertParsePrintAndProcess(
        """
            Integer n = 0;
            synchronized(n) {
            }
        """
    )
}
