package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class ArrayTypeTest : JavaParser() {

    @Test
    fun formatArrayReturnType() {
        val a = parse("""
            package a;
            public class A {
                public String[][] foo() { return null; }
            }
        """)

        val meth = a.classes[0].methods[0]
        assertEquals("public String[][] foo() { return null; }", meth.printTrimmed())
    }
}