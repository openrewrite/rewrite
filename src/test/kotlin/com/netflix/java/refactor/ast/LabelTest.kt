package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class LabelTest(p: Parser): Parser by p {
    
    @Test
    fun labeledWhileLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    labeled: while(true) {
                    }
                }
            }
        """)
        
        val labeled = a.firstMethodStatement() as Tr.Label
        assertEquals("labeled", labeled.label.name)
        assertTrue(labeled.statement is Tr.WhileLoop)
    }
}