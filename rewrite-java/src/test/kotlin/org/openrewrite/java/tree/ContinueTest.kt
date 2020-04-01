package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ContinueTest : JavaParser() {
    
    @Test
    fun continueFromWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) continue;
                }
            }
        """)
        
        val whileLoop = a.firstMethodStatement() as J.WhileLoop
        assertTrue(whileLoop.body is J.Continue)
        assertNull((whileLoop.body as J.Continue).label)
    }

    @Test
    fun continueFromLabeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertTrue(whileLoop.body is J.Continue)
        assertEquals("labeled", (whileLoop.body as J.Continue).label?.simpleName)
    }

    @Test
    fun formatContinueLabeled() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled : while(true)
                        continue labeled;
                }
            }
        """)

        val whileLoop = (a.firstMethodStatement() as J.Label).statement as J.WhileLoop
        assertEquals("continue labeled", whileLoop.body.printTrimmed())
    }
}