package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ThrowTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() throws Exception {
                    throw new UnsupportedOperationException();
                }
            }
        """)
    }

    private val thrown by lazy {
        a.firstMethodStatement() as J.Throw
    }

    @Test
    fun throwException() {
        assertTrue(thrown.exception is J.NewClass)
    }

    @Test
    fun format() {
        assertEquals("throw new UnsupportedOperationException()", thrown.printTrimmed())
    }
}