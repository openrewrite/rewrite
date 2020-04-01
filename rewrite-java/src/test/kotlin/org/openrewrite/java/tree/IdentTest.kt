package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.fields

open class IdentTest : JavaParser() {
    
    @Test
    fun referToField() {
        val a = parse("""
            public class A {
                Integer n = 0;
                Integer m = n;
            }
        """)
        
        val ident = a.fields(1..1)[0].vars[0].initializer as J.Ident
        assertEquals("n", ident.simpleName)
        assertEquals("java.lang.Integer", ident.type.asClass()?.fullyQualifiedName)
    }
}