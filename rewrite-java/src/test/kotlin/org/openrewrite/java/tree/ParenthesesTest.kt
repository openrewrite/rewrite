package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ParenthesesTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                public void test() {
                    int n = ( 0 );
                }
            }
        """)
    }

    private val variable by lazy { (a.firstMethodStatement() as J.VariableDecls).vars[0].initializer }

    @Test
    fun parentheses() {
        assertTrue(variable is J.Parentheses<*>)
    }

    @Test
    fun format() {
        assertEquals("( 0 )", variable?.printTrimmed())
    }
}