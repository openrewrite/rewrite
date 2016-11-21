package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class IfTest(p: Parser): Parser by p {
    val a by lazy {
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

    val iff by lazy { a.firstMethodStatement() as Tr.If }

    @Test
    fun ifElse() {
        assertTrue(iff.ifCondition.tree is Tr.Binary)
        assertTrue(iff.thenPart is Tr.Block<*>)
        
        assertTrue(iff.elsePart?.statement is Tr.If)
        val elseIf = iff.elsePart?.statement as Tr.If
        assertTrue(elseIf.ifCondition.tree is Tr.Binary)
        assertTrue(elseIf.thenPart is Tr.Block<*>)
        assertTrue(elseIf.elsePart?.statement is Tr.Block<*>)
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
        
        val iff = a.firstMethodStatement() as Tr.If
        assertNull(iff.elsePart)
    }

    @Test
    fun format() {
        assertEquals("""
            |if(n == 0) {
            |}
            |else if(n == 1) {
            |}
            |else {
            |}
        """.trimMargin(), iff.printTrimmed())
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
            |public void test() {
            |    if(n == 0) test();
            |    else if(n == 1) test();
            |    else test();
            |}
        """.trimMargin(), a.classes[0].methods()[0].printTrimmed())
    }
}