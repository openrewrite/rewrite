package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class WhileLoopTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            |public class A {
            |    public void test() {
            |        while ( true ) { }
            |    }
            |}
        """)
    }

    val whileLoop by lazy { a.firstMethodStatement() as Tr.WhileLoop }

    @Test
    fun whileLoop() {
        assertTrue(whileLoop.condition.tree is Tr.Literal)
        assertTrue(whileLoop.body is Tr.Block<*>)
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

        val forLoop = a.classes[0].methods()[0].body!!.statements[0] as Tr.WhileLoop
        assertEquals("while(true) test();", forLoop.printTrimmed())
    }
}