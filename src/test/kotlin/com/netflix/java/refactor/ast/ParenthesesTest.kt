package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ParenthesesTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            public class A {
                public void test() {
                    int n = ( 0 );
                }
            }
        """)
    }

    val variable by lazy { (a.firstMethodStatement() as Tr.VariableDecls).vars[0].initializer }

    @Test
    fun parentheses() {
        assertTrue(variable is Tr.Parentheses<*>)
    }

    @Test
    fun format() {
        assertEquals("( 0 )", variable?.printTrimmed())
    }
}