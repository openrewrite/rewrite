package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class ReturnTest(p: Parser): Parser by p {
    
    @Test
    fun returnValue() {
        val a = parse("""
            public class A {
                public String test() {
                    return "";
                }
            }
        """)
        
        val rtn = a.firstMethodStatement() as Tr.Return
        assertTrue(rtn.expr is Tr.Literal)
    }

    @Test
    fun returnVoid() {
        val a = parse("""
            public class A {
                public void test() {
                    return;
                }
            }
        """)

        val rtn = a.firstMethodStatement() as Tr.Return
        assertNull(rtn.expr)
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public int test() {
                    return 0;
                }
            }
        """)

        val rtn = a.firstMethodStatement() as Tr.Return
        assertEquals("return 0", rtn.printTrimmed())
    }
}