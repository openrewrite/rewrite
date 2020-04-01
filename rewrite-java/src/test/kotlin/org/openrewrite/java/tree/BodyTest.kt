package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class BodyTest : JavaParser() {

    @Test
    fun body() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    char c = 'c';
                }
            }
        """)

        assertEquals("""
            {
                int i = 0;
                char c = 'c';
            }
        """.trimIndent(), a.classes[0].methods[0].body?.printTrimmed())
    }
}