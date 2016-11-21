package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class TypeCastTest(p: Parser) : Parser by p {

    @Test
    fun cast() {
        val a = parse("""
            public class A {
                Object o = (Class<String>) Class.forName("java.lang.String");
            }
        """)

        val typeCast = a.classes[0].fields()[0].vars[0].initializer as Tr.TypeCast
        assertEquals("""(Class<String>) Class.forName("java.lang.String")""",
                typeCast.printTrimmed())
    }
}