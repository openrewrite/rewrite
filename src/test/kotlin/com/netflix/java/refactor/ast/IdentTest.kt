package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class IdentTest(p: Parser): Parser by p {
    
    @Test
    fun referToField() {
        val a = parse("""
            public class A {
                Integer n = 0;
                Integer m = n;
            }
        """)
        
        val ident = a.fields(1..1)[0].vars[0].initializer as Tr.Ident
        assertEquals("n", ident.name)
        assertEquals("java.lang.Integer", ident.type.asClass()?.fullyQualifiedName)
    }
}