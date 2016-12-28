package com.netflix.rewrite.ast

import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class AssertTest(p: Parser): Parser by p {

    @Test
    fun assertStatement() {
        val a = parse("""
            public class A {
                void test() {
                    assert 1 == 1;
                }
            }
        """)

        assertEquals("assert 1 == 1", a.classes[0].methods()[0].body!!.statements[0].printTrimmed())
    }
}