package com.netflix.rewrite.ast

import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingTest {

    @Test
    fun flyweights() {
        val f1 = format("")
        val f2 = format("")

        assertTrue(f1 === f2)
    }
}