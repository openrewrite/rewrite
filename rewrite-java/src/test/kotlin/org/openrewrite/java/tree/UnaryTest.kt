package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class UnaryTest : JavaParser() {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.classes[0].fields[0].vars[0].initializer as J.Unary
        assertTrue(unary.operator is J.Unary.Operator.Not)
        assertTrue(unary.expr is J.Parentheses<*>)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int i = 0;
                int j = ++i;
                int k = i ++;
            }
        """)

        val (prefix, postfix) = a.classes[0].fields.subList(1, 3)
        assertEquals("int j = ++i", prefix.printTrimmed())
        assertEquals("int k = i ++", postfix.printTrimmed())
    }
}