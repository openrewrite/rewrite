package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ForEachLoopTest : JavaParser() {

    val a: J.CompilationUnit by lazy { parse("""
            public class A {
                public void test() {
                    for(Integer n: new Integer[] { 0, 1 }) {
                    }
                }
            }
        """)
    }

    private val forEachLoop by lazy { a.firstMethodStatement() as J.ForEachLoop }

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

        val forLoop = a.classes[0].methods[0].body!!.statements[0] as J.ForEachLoop
        assertEquals("for(Integer i : n) test();", forLoop.printTrimmed())
    }
}