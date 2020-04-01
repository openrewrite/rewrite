package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class IfTest : JavaParser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {
                    }
                    else if(n == 1) {
                    }
                    else {
                    }
                }
            }
        """)
    }

    private val iff by lazy { a.firstMethodStatement() as J.If }

    @Test
    fun ifElse() {
        assertTrue(iff.ifCondition.tree is J.Binary)
        assertTrue(iff.thenPart is J.Block<*>)
        
        assertTrue(iff.elsePart?.statement is J.If)
        val elseIf = iff.elsePart?.statement as J.If
        assertTrue(elseIf.ifCondition.tree is J.Binary)
        assertTrue(elseIf.thenPart is J.Block<*>)
        assertTrue(elseIf.elsePart?.statement is J.Block<*>)
    }
    
    @Test
    fun noElse() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) {} 
                }
            }
        """)
        
        val iff = a.firstMethodStatement() as J.If
        assertNull(iff.elsePart)
    }

    @Test
    fun format() {
        assertEquals("""
            if(n == 0) {
            }
            else if(n == 1) {
            }
            else {
            }
        """.trimIndent(), iff.printTrimmed())
    }

    @Test
    fun singleLineIfElseStatements() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    if(n == 0) test();
                    else if(n == 1) test();
                    else test();
                }
            }
        """)

        assertEquals("""
            public void test() {
                if(n == 0) test();
                else if(n == 1) test();
                else test();
            }
        """.trimIndent(), a.classes[0].methods[0].printTrimmed())
    }
}