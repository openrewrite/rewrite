package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ArrayAccessTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            public class a {
                int n[] = new int[] { 0 };
                public void test() {
                    int m = n[0];
                }
            }
        """)
    }

    val variable by lazy { a.firstMethodStatement() as Tr.VariableDecls }
    val arrAccess by lazy { variable.vars[0].initializer as Tr.ArrayAccess }

    @Test
    fun arrayAccess() {
        assertTrue(arrAccess.indexed is Tr.Ident)
        assertTrue(arrAccess.dimension.index is Tr.Literal)
    }

    @Test
    fun format() {
        assertEquals("n[0]", arrAccess.printTrimmed())
    }
}