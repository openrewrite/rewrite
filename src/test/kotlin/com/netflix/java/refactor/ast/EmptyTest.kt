package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class EmptyTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
    }

    @Test
    fun empty() {
        assertTrue(a.firstMethodStatement() is Tr.Empty)
    }

    @Test
    fun format() {
        assertEquals("""
            |public void test() {
            |    ;
            |}
        """.trimMargin(), a.classes[0].methods()[0].printTrimmed())
    }
}