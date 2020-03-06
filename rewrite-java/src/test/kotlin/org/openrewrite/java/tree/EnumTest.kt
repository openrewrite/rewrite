package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

class EnumTest: JavaParser() {
    @Test
    fun anonymousClassInitializer() {
        val aSource = """
            public enum A {
                A1(1) {
                    @Override
                    void foo() {}
                },

                A2 {
                    @Override
                    void foo() {}
                };
                
                A() {}
                A(int n) {}
                
                abstract void foo();
            }
        """.trimIndent()

        val a = parse(aSource)

        assertEquals(aSource, a.printTrimmed())
    }

    @Test
    fun noArguments() {
        val aSource = """
            public enum A {
                A1, A2();
            }
        """.trimIndent()

        val a = parse(aSource)

        assertEquals(aSource, a.printTrimmed())
    }
}