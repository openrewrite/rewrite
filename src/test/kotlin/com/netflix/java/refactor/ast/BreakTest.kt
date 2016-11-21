package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class BreakTest(p: Parser): Parser by p {

    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)

        val whileLoop = a.firstMethodStatement() as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertNull((whileLoop.body as Tr.Break).label)
    }

    @Test
    fun breakFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertTrue(whileLoop.body is Tr.Break)
        assertEquals("labeled", (whileLoop.body as Tr.Break).label?.name)
    }

    @Test
    fun formatLabeledBreak() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        break labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as Tr.Label).statement as Tr.WhileLoop
        assertEquals("break labeled", whileLoop.body.printTrimmed())
    }
}