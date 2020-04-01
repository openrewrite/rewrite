package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class CommentTest : JavaParser() {

    @Test
    fun comments() {
        val aSrc = """
            // About me
            public class A {
            /* } */
            // }
            }
            // Trailing
        """.trimIndent()

        val a = parse(aSrc)
        assertEquals(aSrc, a.printTrimmed())
    }
}
