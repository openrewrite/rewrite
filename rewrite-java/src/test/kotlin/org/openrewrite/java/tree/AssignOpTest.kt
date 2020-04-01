package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class AssignOpTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
        parse("""
            public class A {
                int n = 0;
                public void test() {
                    n += 1;
                }
            }
        """)
    }

    private val assign by lazy {
        a.firstMethodStatement() as J.AssignOp
    }

    @Test
    fun compoundAssignment() {
        assertTrue(assign.operator is J.AssignOp.Operator.Addition)
    }

    @Test
    fun format() {
        assertEquals("n += 1", assign.printTrimmed())
    }
}
