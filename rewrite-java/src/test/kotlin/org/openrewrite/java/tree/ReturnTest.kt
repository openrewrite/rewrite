package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ReturnTest : JavaParser() {
    
    @Test
    fun returnValue() {
        val a = parse("""
            public class A {
                public String test() {
                    return "";
                }
            }
        """)
        
        val rtn = a.firstMethodStatement() as J.Return
        assertTrue(rtn.expr is J.Literal)
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

        val rtn = a.firstMethodStatement() as J.Return
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

        val rtn = a.firstMethodStatement() as J.Return
        assertEquals("return 0", rtn.printTrimmed())
    }
}