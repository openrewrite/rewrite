package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting.format

class FormattingTest {

    @Test
    fun flyweights() {
        val f1 = format("")
        val f2 = format("")

        assertTrue(f1 === f2)
    }
}