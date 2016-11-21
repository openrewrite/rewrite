package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class CommentTest(p: Parser): Parser by p {

    @Test
    fun comments() {
        val aSrc = """
            |// About me
            |public class A {
            |/* } */
            |// }
            |}
            |// Trailing
        """.trimMargin()

        val a = parse(aSrc)
        assertEquals(aSrc, a.printTrimmed())
    }
}