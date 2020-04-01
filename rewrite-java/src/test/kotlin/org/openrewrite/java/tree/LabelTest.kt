package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class LabelTest : JavaParser() {
    
    @Test
    fun labeledWhileLoop() {
        val orig = """
            |public class A {
            |    public void test() {
            |        labeled: while(true) {
            |        }
            |    }
            |}
        """.trimMargin()
        val a = parse(orig)
        
        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("labeled", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(orig, a.print())
    }

    @Test
    fun nonEmptyLabeledWhileLoop() {
        val orig = """
            |public class A {
            |    public void test() {
            |        outer: while(true) {
            |            while(true) {
            |                break outer;
            |            }
            |        }
            |    }
            |}
        """.trimMargin()

        val a = parse(orig)

        val labeled = a.firstMethodStatement() as J.Label
        assertEquals("outer", labeled.label.simpleName)
        assertTrue(labeled.statement is J.WhileLoop)
        assertEquals(orig, a.print())
    }
}
