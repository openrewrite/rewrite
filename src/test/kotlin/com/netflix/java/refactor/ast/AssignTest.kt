package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class AssignTest(p: Parser): Parser by p {
    
    @Test
    fun assignmentToField() {
        val a = parse("""
            public class A {
                String s;
                public void test() {
                    s = "foo";
                }
            }
        """)
        
        val assign = a.firstMethodStatement() as Tr.Assign
        assertEquals("s", (assign.variable as Tr.Ident).name)
        assertTrue(assign.assignment is Tr.Literal)
    }
    
    @Test
    fun format() {
        val a = parse("""
            @SuppressWarnings(value = "ALL")
            public class A {}
        """)
        
        val assign = a.classes[0].annotations[0].args!!.args[0] as Tr.Assign
        
        assertEquals("value = \"ALL\"", assign.printTrimmed())
    }
}