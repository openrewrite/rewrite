package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class BinaryTest(p: Parser): Parser by p {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.fields()[0].vars[0].initializer as Tr.Binary
        assertTrue(bin.operator is Tr.Binary.Operator.Addition)
        assertTrue(bin.left is Tr.Literal)
        assertTrue(bin.right is Tr.Literal)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)

        val bin = a.fields()[0].vars[0].initializer as Tr.Binary
        assertEquals("0 + 1", bin.printTrimmed())
    }

    /**
     * String folding needs to be disabled in the parser to preserve the binary expression in the AST!
     * @see com.sun.tools.javac.parser.JavacParser.allowStringFolding
     */
    @Test
    fun formatFoldableStrings() {
        val a = """
            public class A {
                String s = "a" + "b";
            }
        """

        assertEquals("\"a\" + \"b\"", parse(a).fields()[0].vars[0].initializer?.printTrimmed())
    }
}
