package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class EmptyTest : JavaParser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() {
                    ;
                }
            }
        """)
    }

    @Test
    fun empty() {
        assertTrue(a.firstMethodStatement() is J.Empty)
    }

    @Test
    fun format() {
        assertEquals("""
            |public void test() {
            |    ;
            |}
        """.trimMargin(), a.classes[0].methods[0].printTrimmed())
    }
}