package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class SynchronizedTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                Integer n = 0;
                public void test() {
                    synchronized(n) {
                    }
                }
            }
        """)
    }

    private val sync by lazy { a.firstMethodStatement() as J.Synchronized }

    @Test
    fun synchronized() {
        assertTrue(sync.lock.tree is J.Ident)
    }

    @Test
    fun format() {
        assertEquals("synchronized(n) {\n}", sync.printTrimmed())
    }
}