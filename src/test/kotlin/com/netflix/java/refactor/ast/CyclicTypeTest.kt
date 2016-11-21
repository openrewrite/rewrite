package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class CyclicTypeTest(p: Parser): Parser by p {
    
    @Test
    fun cyclicType() {
        val a = parse("""
            public class A {
                A[] nested = new A[0];
            }
        """)
        
        val fieldType = a.fields()[0].vars[0].type.asArray()
        assertTrue(fieldType is Type.Array)

        val elemType = fieldType!!.elemType.asClass()
        assertTrue(elemType is Type.Class)

        assertTrue(elemType!!.members[0].type?.asClass()?.isCyclicRef() ?: false)
    }
}