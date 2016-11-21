package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class BlockTest(p: Parser): Parser by p {
    
    @Test
    fun methodBlock() {
        val a = parse("""
            public class A {
                public void foo() {
                    System.out.println("foo");
                }
            }
        """)
        
        assertEquals(1, a.classes[0].methods()[0].body!!.statements.size)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void foo() {  }
            }
        """)
        
        assertEquals("{  }", a.classes[0].methods()[0].body!!.printTrimmed())
    }

    @Test
    fun staticInitBlock() {
        val a = parse("""
            public class A {
                static {}
            }
        """)

        assertEquals("static {}", a.classes[0].body.statements[0].printTrimmed())
    }
}