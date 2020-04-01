package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.fields

open class FieldAccessTest : JavaParser() {
    
    @Test
    fun fieldAccess() {
        val b = """
            public class B {
                public B field = new B();
            }
        """
        
        val a = """
            public class A {
                B b = new B();
                String s = b . field . field;
            }
        """

        val cu = parse(a, b)

        val acc = cu.fields(0..1).flatMap { it.vars }.find { it.initializer is J.FieldAccess }?.initializer as J.FieldAccess?
        assertEquals("b . field . field", acc?.printTrimmed())
        assertEquals("field", acc?.simpleName)
        assertEquals("b . field", acc?.target?.printTrimmed())
    }
}