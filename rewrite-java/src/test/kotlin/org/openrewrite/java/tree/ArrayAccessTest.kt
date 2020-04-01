package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ArrayAccessTest : JavaParser() {
    private val a: J.CompilationUnit by lazy {
        parse("""
            public class a {
                int n[] = new int[] { 0 };
                public void test() {
                    int m = n[0];
                }
            }
        """)
    }

    private val variable by lazy { a.firstMethodStatement() as J.VariableDecls }
    private val arrAccess by lazy { variable.vars[0].initializer as J.ArrayAccess }

    @Test
    fun arrayAccess() {
        assertTrue(arrAccess.indexed is J.Ident)
        assertTrue(arrAccess.dimension.index is J.Literal)
    }

    @Test
    fun format() {
        assertEquals("n[0]", arrAccess.printTrimmed())
    }
}