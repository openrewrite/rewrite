package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class BreakTest : JavaParser() {

    @Test
    fun breakFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) break;
                }
            }
        """)

        val whileLoop = a.firstMethodStatement() as J.WhileLoop
        assertTrue(whileLoop.body is J.Break)
        assertNull((whileLoop.body as J.Break).label)
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

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertTrue(whileLoop.body is J.Break)
        assertEquals("labeled", (whileLoop.body as J.Break).label?.simpleName)
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

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertEquals("break labeled", whileLoop.body.printTrimmed())
    }
}