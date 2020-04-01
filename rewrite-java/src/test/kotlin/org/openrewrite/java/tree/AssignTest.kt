package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class AssignTest : JavaParser() {
    
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
        
        val assign = a.firstMethodStatement() as J.Assign
        assertEquals("s", (assign.variable as J.Ident).simpleName)
        assertTrue(assign.assignment is J.Literal)
    }
    
    @Test
    fun format() {
        val a = parse("""
            @SuppressWarnings(value = "ALL")
            public class A {}
        """)
        
        val assign = a.classes[0].annotations[0].args!!.args[0] as J.Assign
        
        assertEquals("value = \"ALL\"", assign.printTrimmed())
    }
}