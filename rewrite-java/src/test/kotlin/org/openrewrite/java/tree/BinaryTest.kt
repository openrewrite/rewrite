package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class BinaryTest : JavaParser() {
    
    @Test
    fun arithmetic() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)
        
        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
        assertTrue(bin.operator is J.Binary.Operator.Addition)
        assertTrue(bin.left is J.Literal)
        assertTrue(bin.right is J.Literal)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n = 0 + 1;
            }
        """)

        val bin = a.classes[0].fields[0].vars[0].initializer as J.Binary
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

        assertEquals("\"a\" + \"b\"", parse(a).classes[0].fields[0].vars[0].initializer?.printTrimmed())
    }

    @Test
    fun endOfLineBreaks() {
        val aSource = """
            import java.util.Objects;
            public class A {
                {
                    boolean b = Objects.equals(1, 2) //
                        && Objects.equals(3, 4) //
                        && Objects.equals(4, 5);
                }
            }
        """.trimIndent()

        val a = parse(aSource)

        assertEquals(aSource, a.print())
    }
}
