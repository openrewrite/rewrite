package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.firstMethodStatement

open class TernaryTest : JavaParser() {
    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                int n;
                public void test() {
                    String evenOrOdd = n % 2 == 0 ? "even" : "odd";
                }
            }
        """)
    }

    private val evenOrOdd by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val ternary by lazy { evenOrOdd.vars[0].initializer as J.Ternary }

    @Test
    fun ternary() {
        assertEquals("java.lang.String", ternary.type.asClass()?.fullyQualifiedName)
        assertTrue(ternary.condition is J.Binary)
        assertTrue(ternary.truePart is J.Literal)
        assertTrue(ternary.falsePart is J.Literal)
    }

    @Test
    fun format() {
        assertEquals("""n % 2 == 0 ? "even" : "odd"""", ternary.printTrimmed())
    }
}