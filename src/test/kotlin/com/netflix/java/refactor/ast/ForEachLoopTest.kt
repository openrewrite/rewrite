package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ForEachLoopTest(p: Parser): Parser by p {

    val a by lazy { parse("""
            public class A {
                public void test() {
                    for(Integer n: new Integer[] { 0, 1 }) {
                    }
                }
            }
        """)
    }

    val forEachLoop by lazy { a.firstMethodStatement() as Tr.ForEachLoop }

    @Test
    fun forEachLoop() {
        assertTrue(forEachLoop.control.variable is Tr.VariableDecls)
        assertTrue(forEachLoop.control.iterable is Expression)
    }

    @Test
    fun format() {
        assertEquals("for(Integer n: new Integer[] { 0, 1 }) {\n}", forEachLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineForLoops() {
        val a = parse("""
            public class A {
                Integer[] n;
                public void test() {
                    for(Integer i : n) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods()[0].body!!.statements[0] as Tr.ForEachLoop
        assertEquals("for(Integer i : n) test();", forLoop.printTrimmed())
    }
}