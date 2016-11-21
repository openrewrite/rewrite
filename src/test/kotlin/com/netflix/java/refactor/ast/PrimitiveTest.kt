package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class PrimitiveTest(p: Parser): Parser by p {
    
    @Test
    fun primitiveField() {
        val a = parse("""
            public class A {
                int n = 0;
            }
        """)
        
        val primitive = a.fields()[0].typeExpr as Tr.Primitive
        assertEquals(Type.Tag.Int, primitive.typeTag)
    }
}