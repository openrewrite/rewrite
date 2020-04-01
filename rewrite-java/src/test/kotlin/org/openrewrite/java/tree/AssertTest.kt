package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class AssertTest : JavaParser() {

    @Test
    fun assertStatement() {
        val a = parse("""
            public class A {
                void test() {
                    assert 1 == 1;
                }
            }
        """)

        assertEquals("assert 1 == 1", a.classes[0].methods[0].body!!.statements[0].printTrimmed())
    }
}