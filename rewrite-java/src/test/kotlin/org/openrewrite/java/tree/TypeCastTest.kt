package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class TypeCastTest : JavaParser() {

    @Test
    fun cast() {
        val a = parse("""
            public class A {
                Object o = (Class<String>) Class.forName("java.lang.String");
            }
        """)

        val typeCast = a.classes[0].fields[0].vars[0].initializer as J.TypeCast
        assertEquals("""(Class<String>) Class.forName("java.lang.String")""",
                typeCast.printTrimmed())
    }
}