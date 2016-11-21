package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class DoWhileLoopTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            |public class A {
            |    public void test() { do { } while ( true ) ; }
            |}
        """)
    }

    val whileLoop by lazy { a.firstMethodStatement() as Tr.DoWhileLoop }

    @Test
    fun doWhileLoop() {
        assertTrue(whileLoop.condition.tree is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
    }

    @Test
    fun format() {
        assertEquals("{ do { } while ( true ) ; }", a.classes[0].methods()[0].body!!.printTrimmed())
    }
}