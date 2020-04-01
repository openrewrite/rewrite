package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class DoWhileLoopTest : JavaParser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() { do { } while ( true ) ; }
            }
        """)
    }

    private val whileLoop by lazy { a.firstMethodStatement() as J.DoWhileLoop }

    @Test
    fun doWhileLoop() {
        assertTrue(whileLoop.whileCondition.condition.tree is J.Literal)
        assertTrue(whileLoop.body is J.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("{ do { } while ( true ) ; }", a.classes[0].methods[0].body!!.printTrimmed())
    }
}