package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class WhileLoopTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() {
                    while ( true ) { }
                }
            }
        """)
    }

    private val whileLoop by lazy { a.firstMethodStatement() as J.WhileLoop }

    @Test
    fun whileLoop() {
            assertTrue(whileLoop.condition.tree is J.Literal)
        assertTrue(whileLoop.body is J.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("while ( true ) { }", whileLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineWhileLoops() {
        val a = parse("""
            public class A {
                public void test() {
                    while(true) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[0] as J.WhileLoop
        assertEquals("while(true) test();", forLoop.printTrimmed())
    }
}