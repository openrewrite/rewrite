package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J

class FindReferencesToVariableTest : JavaParser() {
    @Test
    fun findReferences() {
        val a = parse("""
            public class A {
                int n;
                public void foo() {
                    int n;
                    n = 1;
                    (n) = 2;
                    n++;
                    if((n = 4) > 1) {}
                    this.n = 1;
                }
            }
        """.trimIndent())

        val n = (a.classes[0]!!.methods[0]!!.body!!.statements[0] as J.VariableDecls).vars[0]

        val refs = FindReferencesToVariable(n.name).visit(a.classes[0])

        assertEquals(4, refs.size)
    }
}
